package com.Jhon.myempty.blogdenotasjava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.FrameLayout;
import com.google.android.material.color.MaterialColors;

import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FloatingService extends Service {

    private WindowManager mWindowManager;
    private View mFloatingView;
    private WindowManager.LayoutParams params;

    private EditText floatingTxtNota;
    private TextView floatingTitleText, lblContadorFlotante;
    private ImageView btnClose, btnModify, minimizar;
    private View headerView, resizeHandle;
    private FrameLayout minimizedContainer;
    private ImageView minimizedIcon;

    private Uri uriArchivoActual;
    private int minWidthPx;
    private int minHeightPx;
    
    // Variables para controlar el estado
    private boolean isMinimized = false;
    private int savedWidth, savedHeight;
    private int savedX, savedY;
    
    // Variable para almacenar el checklist (si existe)
    private String checklistData = "";

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startMyOwnForeground();

        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        boolean usarMaterialYou = prefs.getBoolean("material_theme_activado", false);
        
        Context contextToUse;
        int themeId = R.style.AppTheme; 
        if (usarMaterialYou) {
            contextToUse = DynamicColors.wrapContextIfAvailable(this, themeId);
        } else {
            contextToUse = new ContextThemeWrapper(this, themeId);
        }

        mFloatingView = LayoutInflater.from(contextToUse).inflate(R.layout.floating_editor_layout, null);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        minWidthPx = (int) (200 * metrics.density);
        minHeightPx = (int) (150 * metrics.density);

        inicializarVistas();
        
        configurarParametrosVentana(metrics);
        configurarListeners();
    }

    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.Jhon.myempty.blogdenotasjava";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, "Servicio Flotante", NotificationManager.IMPORTANCE_MIN);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(chan);
        }

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Nota Flotante")
                .setContentText("Modo edición activo")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .build();

        startForeground(2, notification);
    }

    private void inicializarVistas() {
        floatingTxtNota = mFloatingView.findViewById(R.id.floating_txtNota);
        floatingTitleText = mFloatingView.findViewById(R.id.floating_title_text);
        btnClose = mFloatingView.findViewById(R.id.btn_close_floating);
        btnModify = mFloatingView.findViewById(R.id.modify);
        headerView = mFloatingView.findViewById(R.id.floating_header);
        resizeHandle = mFloatingView.findViewById(R.id.resize_handle);
        lblContadorFlotante = mFloatingView.findViewById(R.id.lbl_contador_floating);
        minimizar = mFloatingView.findViewById(R.id.minimizar);
        
        minimizedContainer = mFloatingView.findViewById(R.id.minimized_container);
        minimizedIcon = mFloatingView.findViewById(R.id.minimized_icon);
    }

    private void configurarParametrosVentana(DisplayMetrics metrics) {
        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                (int) (300 * metrics.density),
                (int) (350 * metrics.density),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        mWindowManager.addView(mFloatingView, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String contenidoRaw = intent.getStringExtra("contenido_nota");
            String uriString = intent.getStringExtra("uri_archivo");

            if (uriString != null && !uriString.isEmpty()) {
                uriArchivoActual = Uri.parse(uriString);
                DocumentFile df = DocumentFile.fromSingleUri(this, uriArchivoActual);
                if (df != null && df.getName() != null) {
                    floatingTitleText.setText(df.getName().replace(".txt", ""));
                }
            }

            if (floatingTxtNota != null && contenidoRaw != null) {
                // --- LÓGICA COMPATIBLE CON EditorActivity ---
                
                // 1. Extraer y almacenar el checklist (si existe)
                checklistData = extraerChecklistData(contenidoRaw);
                
                // 2. Extraer y aplicar el color de fondo
                int colorNota = extraerColorDeHtml(contenidoRaw);
                floatingTxtNota.setBackgroundColor(colorNota);

                // 3. Limpiar el HTML para mostrar solo el texto limpio
                // Primero eliminamos el checklistData para que no aparezca como texto
                String contenidoSinChecklist = contenidoRaw;
                if (!checklistData.isEmpty()) {
                    contenidoSinChecklist = contenidoRaw.replace(checklistData, "");
                }
                
                // Luego quitamos el DIV envolvente de color
                String textoLimpio = contenidoSinChecklist.replaceAll("<div[^>]*>", "").replace("</div>", "");
                
                // 4. Mostrar el texto en el EditText
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    floatingTxtNota.setText(android.text.Html.fromHtml(textoLimpio, android.text.Html.FROM_HTML_MODE_LEGACY));
                } else {
                    floatingTxtNota.setText(android.text.Html.fromHtml(textoLimpio));
                }
                
                try {
                    floatingTxtNota.setSelection(floatingTxtNota.getText().length());
                } catch (Exception ignored) {}
                
                // 5. Actualizar contador
                String inicial = floatingTxtNota.getText().toString().trim();
                int p = inicial.isEmpty() ? 0 : inicial.split("\\s+").length;
                lblContadorFlotante.setText(p + "p | " + floatingTxtNota.getText().length() + "c");
            }
        }
        return START_STICKY;
    }

    private String extraerChecklistData(String html) {
        try {
            // Buscar el bloque <div id='checklist_data'>...</div>
            Pattern pattern = Pattern.compile("<div id='checklist_data'.*?>(.*?)</div>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            
            if (matcher.find()) {
                // Devolvemos todo el bloque DIV (no solo el contenido interno)
                return matcher.group(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ""; // Retornar cadena vacía si no hay checklist
    }

    private void configurarListeners() {
        // 1. Habilitar teclado al tocar el texto
        floatingTxtNota.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                mWindowManager.updateViewLayout(mFloatingView, params);
                v.performClick();
                floatingTxtNota.requestFocus();
            }
            return false;
        });
        
        floatingTxtNota.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String texto = s.toString().trim();
                int p = texto.isEmpty() ? 0 : texto.split("\\s+").length;
                int c = s.length();
                if (lblContadorFlotante != null) {
                    lblContadorFlotante.setText(p + "p | " + c + "c");
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // 2. Guardar y quitar teclado al tocar fuera de la ventana
        mFloatingView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_OUTSIDE && !isMinimized) {
                guardarNotaFlotante();
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                mWindowManager.updateViewLayout(mFloatingView, params);
                return true;
            }
            return false;
        });

        // 3. Mover ventana (Header) - Solo cuando no está minimizado
        headerView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isMinimized) return false;
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x; initialY = params.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });

        // 4. Redimensionar ventana (Resize Handle)
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialWidth, initialHeight;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isMinimized) return false;
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialWidth = params.width;
                        initialHeight = params.height;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);

                        int newWidth = initialWidth + deltaX;
                        int newHeight = initialHeight + deltaY;

                        if (newWidth >= minWidthPx) params.width = newWidth;
                        if (newHeight >= minHeightPx) params.height = newHeight;

                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });

        // 5. Botón cerrar
        btnClose.setOnClickListener(v -> {
            guardarNotaFlotante();
            stopSelf();
        });

        // 6. Botón para volver al Editor completo
        btnModify.setOnClickListener(v -> {
            guardarNotaFlotante();
            
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("uri_archivo", uriArchivoActual != null ? uriArchivoActual.toString() : "");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            startActivity(intent);
            stopSelf();
        });

        // 7. Botón para minimizar/restaurar
        minimizar.setOnClickListener(v -> {
            toggleMinimize();
        });

        // 8. Listener para el icono minimizado
        minimizedContainer.setOnClickListener(v -> {
            if (isMinimized) {
                toggleMinimize();
            }
        });

        // 9. Permitir mover la ventana minimizada
        minimizedContainer.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isMinimized) return false;
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void toggleMinimize() {
        if (!isMinimized) {
            // Guardar estado actual antes de minimizar
            guardarNotaFlotante();
            savedWidth = params.width;
            savedHeight = params.height;
            savedX = params.x;
            savedY = params.y;
            
            // Ocultar todos los elementos de la vista normal
            floatingTxtNota.setVisibility(View.GONE);
            floatingTitleText.setVisibility(View.GONE);
            btnClose.setVisibility(View.GONE);
            btnModify.setVisibility(View.GONE);
            headerView.setVisibility(View.GONE);
            resizeHandle.setVisibility(View.GONE);
            lblContadorFlotante.setVisibility(View.GONE);
            minimizar.setVisibility(View.GONE);
            
            // Mostrar la vista minimizada
            minimizedContainer.setVisibility(View.VISIBLE);
            
            // Cambiar el tamaño de la ventana
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int minimizedSize = (int) (60 * metrics.density);
            
            params.width = minimizedSize;
            params.height = minimizedSize;
            
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                          WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            
            isMinimized = true;
            
        } else {
            // Restaurar la vista normal
            minimizedContainer.setVisibility(View.GONE);
            
            floatingTxtNota.setVisibility(View.VISIBLE);
            floatingTitleText.setVisibility(View.VISIBLE);
            btnClose.setVisibility(View.VISIBLE);
            btnModify.setVisibility(View.VISIBLE);
            headerView.setVisibility(View.VISIBLE);
            resizeHandle.setVisibility(View.VISIBLE);
            lblContadorFlotante.setVisibility(View.VISIBLE);
            minimizar.setVisibility(View.VISIBLE);
            
            params.width = savedWidth;
            params.height = savedHeight;
            
            isMinimized = false;
        }
        
        mWindowManager.updateViewLayout(mFloatingView, params);
    }

    private void guardarNotaFlotante() {
        if (uriArchivoActual == null || isMinimized) return;

        // 1. Convertir el texto visible a HTML
        String contenidoHtml = android.text.Html.toHtml(floatingTxtNota.getText());

        // 2. Obtener el color de fondo actual
        int colorActual = com.google.android.material.color.MaterialColors.getColor(floatingTxtNota, com.google.android.material.R.attr.colorSurfaceContainer);
        if (floatingTxtNota.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
            colorActual = ((android.graphics.drawable.ColorDrawable) floatingTxtNota.getBackground()).getColor();
        }

        // 3. Convertir el color a hexadecimal
        String hexColor = String.format("#%06X", (0xFFFFFF & colorActual));

        // 4. Construir el HTML FINAL (COMPATIBLE con EditorActivity)
        // Estructura: <div con color> + Contenido HTML + Checklist (si existe) + </div>
        String htmlParaGuardar = "<div style='background-color:" + hexColor + ";'>" 
                               + contenidoHtml 
                               + (checklistData != null && !checklistData.isEmpty() ? checklistData : "")
                               + "</div>";

        // 5. Escribir en el archivo
        try (OutputStream os = getContentResolver().openOutputStream(uriArchivoActual, "wt")) {
            if (os != null) {
                os.write(htmlParaGuardar.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        guardarNotaFlotante();
        if (mFloatingView != null && mWindowManager != null) {
            try {
                mWindowManager.removeView(mFloatingView);
            } catch (Exception e) {
                // View not attached
            }
        }
        super.onDestroy();
    }
    
    private int extraerColorDeHtml(String html) {
        try {
            if (html != null && html.contains("background-color:")) {
                int inicio = html.indexOf("background-color:") + 17;
                int fin = html.indexOf(";", inicio);
                if (fin == -1) fin = html.indexOf("\"", inicio);
                if (fin == -1) fin = html.indexOf("'", inicio);
                
                if (fin > inicio) {
                    String hexColor = html.substring(inicio, fin).trim();
                    return Color.parseColor(hexColor);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return com.google.android.material.R.attr.colorSurfaceContainer; 
    }
}