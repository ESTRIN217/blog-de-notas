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

import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;

import java.io.OutputStream;

public class FloatingService extends Service {

    private WindowManager mWindowManager;
    private View mFloatingView;
    private WindowManager.LayoutParams params;

    private EditText floatingTxtNota;
    private TextView floatingTitleText, lblContadorFlotante;
    private ImageView btnClose, btnModify;
    private View headerView, resizeHandle;

    private Uri uriArchivoActual;
    private int minWidthPx;
    private int minHeightPx;

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
            // --- NUEVA LÓGICA DE PROCESAMIENTO ---
            
            // 1. Extraer y aplicar el color de fondo
            int colorNota = extraerColorDeHtml(contenidoRaw);
            floatingTxtNota.setBackgroundColor(colorNota);

            // 2. Limpiar el HTML para mostrar solo el texto limpio al usuario
            // Quitamos el DIV envolvente para que no se vea el código
            String textoLimpio = contenidoRaw.replaceAll("<div[^>]*>", "").replace("</div>", "");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                floatingTxtNota.setText(android.text.Html.fromHtml(textoLimpio, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                floatingTxtNota.setText(android.text.Html.fromHtml(textoLimpio));
            }
            
            try {
                floatingTxtNota.setSelection(floatingTxtNota.getText().length());
            } catch (Exception ignored) {}
            String inicial = floatingTxtNota.getText().toString().trim();
    int p = inicial.isEmpty() ? 0 : inicial.split("\\s+").length;
    lblContadorFlotante.setText(p + "p | " + floatingTxtNota.getText().length() + "c");
        }
    }
    return START_STICKY;
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
            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                guardarNotaFlotante();
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                mWindowManager.updateViewLayout(mFloatingView, params);
                return true;
            }
            return false;
        });

        // 3. Mover ventana (Header)
        headerView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

        // 4. Redimensionar ventana (Resize Handle) - ¡AQUÍ ESTÁ LA LÓGICA RESTAURADA!
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialWidth, initialHeight;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

                        // Respetar tamaños mínimos
                        if (newWidth >= minWidthPx) params.width = newWidth;
                        if (newHeight >= minHeightPx) params.height = newHeight;

                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });

        // 5. Botones en FloatingService
        btnClose.setOnClickListener(v -> {
    guardarNotaFlotante(); // Guardamos antes de cerrar
    stopSelf();
        });

        // 5. Botón para volver al Editor completo
        btnModify.setOnClickListener(v -> {
    guardarNotaFlotante(); // Guardar cambios actuales
    
    Intent intent = new Intent(this, EditorActivity.class);
    // Aseguramos que la llave sea "uri_archivo"
    intent.putExtra("uri_archivo", uriArchivoActual != null ? uriArchivoActual.toString() : "");
    
    // IMPORTANTE: Añadir Flags para que no cree una instancia nueva si ya existe
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    
    startActivity(intent);
    stopSelf(); // Cerrar la burbuja
    });
    }

    private void guardarNotaFlotante() {
    if (uriArchivoActual == null) return;

    // 1. Convertir el texto que ves en la ventana flotante a HTML
    // Esto preserva negritas, cursivas, etc. si las hubiera.
    String contenidoHtml = android.text.Html.toHtml(floatingTxtNota.getText());

    // 2. Obtener el color de fondo actual de la vista flotante
    // IMPORTANTE: Asumo que al abrir la ventana flotante, le pusiste el color al floatingTxtNota.
    // Si el color está en otro layout padre, cambia 'floatingTxtNota' por esa vista.
    int colorActual = android.graphics.Color.WHITE; // Color por defecto de seguridad
    if (floatingTxtNota.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
        colorActual = ((android.graphics.drawable.ColorDrawable) floatingTxtNota.getBackground()).getColor();
    }

    // 3. Convertir el color a formato Hexadecimal (#RRGGBB)
    String hexColor = String.format("#%06X", (0xFFFFFF & colorActual));

    // 4. Envolver todo en la etiqueta DIV con el estilo de fondo
    // Esto mantiene la compatibilidad con tu EditorActivity
    String htmlParaGuardar = "<div style='background-color:" + hexColor + ";'>" + contenidoHtml + "</div>";

    // 5. Escribir en el archivo
    try (java.io.OutputStream os = getContentResolver().openOutputStream(uriArchivoActual, "wt")) {
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
            String hexColor = html.substring(inicio, fin).trim();
            return Color.parseColor(hexColor);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return Color.WHITE; // Color por defecto si falla
    }
}