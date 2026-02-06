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
import android.graphics.Paint;
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
import com.google.android.material.checkbox.MaterialCheckBox;

import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.DynamicColors;
import android.text.Html;
import android.util.Log;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

public class FloatingService extends Service {

    private WindowManager mWindowManager;
    private View mFloatingView;
    private WindowManager.LayoutParams params;

    private EditText floatingTxtNota;
    private TextView floatingTitleText, lblContadorFlotante;
    private ImageView btnClose, btnModify, minimizar,minimizedIcon;
    private View headerView, resizeHandle;
    private FrameLayout minimizedContainer;
    private RecyclerView contenedorAdjuntos;

    private Uri uriArchivoActual;
    private int minWidthPx;
    private int minHeightPx;
    
    // Variables para controlar el estado
    private boolean isMinimized = false;
    private int savedWidth, savedHeight;
    private int savedX, savedY;
    
    // Variable para almacenar el checklist (si existe)
    private String checklistData = "";
    private SimpleAdapter adapterAdjuntos;
    private String carpetaUriPadre;

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
        
        // --- ERROR CORREGIDO 1: Inicializar el adaptador ---
        adapterAdjuntos = new SimpleAdapter(this);
        
        // --- ERROR CORREGIDO 2: Configurar el RecyclerView ---
        contenedorAdjuntos.setLayoutManager(new LinearLayoutManager(this));
        contenedorAdjuntos.setAdapter(adapterAdjuntos);

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
        contenedorAdjuntos = mFloatingView.findViewById(R.id.contenedorAdjuntos);
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
        String uriString = intent.getStringExtra("uri_archivo");
        // Recuperar carpeta padre para poder guardar luego
        carpetaUriPadre = getSharedPreferences("MisPreferencias", MODE_PRIVATE)
                            .getString("carpeta_uri", null);

        if (uriString != null && !uriString.isEmpty()) {
            uriArchivoActual = Uri.parse(uriString);
            
            // 1. Leer contenido
            String fullContent = NoteIOHelper.readContent(this, uriArchivoActual);
            
            // 2. Título (Sin extensión)
            DocumentFile df = DocumentFile.fromSingleUri(this, uriArchivoActual);
            if (df != null && df.getName() != null) {
                floatingTitleText.setText(df.getName().replace(".txt", ""));
            }

            // 3. Limpiar listas previas antes de cargar (Evita duplicados si se reinicia el service)
            if (adapterAdjuntos != null) {
                adapterAdjuntos.getListaDatos().clear();
            }

            // 4. Procesar Checklist y ADJUNTOS
            // Nota: Aquí deberías extraer también las etiquetas <img> y <audio> 
            // si quieres que se vean en la ventana flotante.
            String checklistData = NoteIOHelper.extractChecklistData(fullContent);
            if (!checklistData.isEmpty()) {
                procesarChecklist(checklistData); 
            }

            // 5. Aplicar Color de fondo
            int colorNota = NoteIOHelper.extractColor(fullContent);
            // Aplicamos al fondo del layout principal de la burbuja
            if (floatingTxtNota != null) {
                floatingTxtNota.setBackgroundColor(colorNota);
            }

            // 6. Texto principal
            String textoLimpio = NoteIOHelper.cleanHtmlForEditor(fullContent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                floatingTxtNota.setText(Html.fromHtml(textoLimpio, Html.FROM_HTML_MODE_LEGACY));
            } else {
                floatingTxtNota.setText(Html.fromHtml(textoLimpio));
            }

            actualizarContador();
        }
    }
    return START_NOT_STICKY;
    }
    
    
    private void procesarChecklist(String data) {
    if (data == null || data.isEmpty()) return;

    Pattern pattern = Pattern.compile("<chk state=\"(.*?)\">(.*?)</chk>");
    Matcher matcher = pattern.matcher(data);

    while (matcher.find()) {
        boolean isChecked = "true".equalsIgnoreCase(matcher.group(1));
        String texto = matcher.group(2);

        // 1. Creamos el objeto de datos
        ItemAdjunto item = new ItemAdjunto(ItemAdjunto.TIPO_CHECK, texto);
        item.setMarcado(isChecked);

        // 2. Lo añadimos al adaptador (que ya sabe cómo inflar la vista)
        if (adapterAdjuntos != null) {
            adapterAdjuntos.agregarItem(item); // Usa el método que ya definimos
        }
    }
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
        minimizedIcon.setOnClickListener(v -> {
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

    // 1. Obtener el HTML del cuerpo
    String bodyHtml = Html.toHtml(floatingTxtNota.getText());

    // 2. GENERAR EL HTML DEL CHECKLIST DESDE EL ADAPTADOR
    StringBuilder sb = new StringBuilder();
    // Importante: Asegúrate de que 'adapterAdjuntos' sea el SimpleAdapter de tu servicio
    if (adapterAdjuntos != null) {
        for (ItemAdjunto item : adapterAdjuntos.getListaDatos()) {
            if (item.getTipo() == ItemAdjunto.TIPO_CHECK) {
                String state = item.isMarcado() ? "true" : "false";
                // Escapamos caracteres para no romper el HTML
                String text = item.getContenido()
                        .replace("\"", "&quot;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
                
                sb.append("<chk state=\"").append(state).append("\">")
                  .append(text).append("</chk>");
            }
        }
    }
    String checklistHtml = sb.toString();

    // 3. Determinar el color de fondo
    int colorActual = Color.WHITE; // Valor por defecto
    if (floatingTxtNota.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
        colorActual = ((android.graphics.drawable.ColorDrawable) floatingTxtNota.getBackground()).getColor();
    }

    // 4. LLAMADA CORREGIDA AL HELPER (7 parámetros exactos)
    boolean exito = NoteIOHelper.saveNote(
            this, 
            uriArchivoActual, 
            bodyHtml, 
            checklistHtml, // El HTML que acabamos de generar arriba
            colorActual, 
            null, // backgroundName
            null  // backgroundImageUri
    );

    if (!exito) {
        Log.e("FLOATING_SAVE", "No se pudo guardar la nota desde el servicio");
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
    
    private void actualizarContador() {
        String texto = floatingTxtNota.getText().toString().trim();
        int p = texto.isEmpty() ? 0 : texto.split("\\s+").length;
        if (lblContadorFlotante != null) {
            lblContadorFlotante.setText(p + "p | " + texto.length() + "c");
        }
    }
}