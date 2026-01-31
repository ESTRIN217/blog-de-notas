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
        adapterAdjuntos = new SimpleAdapter();
        
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
        carpetaUriPadre = getSharedPreferences("MisPreferencias", MODE_PRIVATE)
                            .getString("carpeta_uri", null);

        if (uriString != null && !uriString.isEmpty()) {
            uriArchivoActual = Uri.parse(uriString);
            
            // 1. Leer contenido usando el Helper
            String fullContent = NoteIOHelper.readContent(this, uriArchivoActual);
            
            // 2. Título del archivo
            DocumentFile df = DocumentFile.fromSingleUri(this, uriArchivoActual);
            if (df != null && df.getName() != null) {
                floatingTitleText.setText(df.getName().replace(".txt", ""));
            }

            // 3. Procesar Checklist (Usando Helper)
            String checklistData = NoteIOHelper.extractChecklistData(fullContent);
            if (!checklistData.isEmpty()) {
                procesarChecklist(checklistData);
            }

            // 4. Color y Fondo (Usando Helper)
            int colorNota = NoteIOHelper.extractColor(fullContent);
            String bgImageUri = NoteIOHelper.extractBackgroundImageUri(fullContent);
            
            // Aplicar al contenedor flotante (ajusta 'floatingRootView' al ID de tu layout)
            if (floatingTxtNota != null) {
                floatingTxtNota.setBackgroundColor(colorNota);
                // Si tienes un método para poner imagen en el service, úsalo aquí con bgImageUri
            }

            // 5. Mostrar texto principal limpio
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

    // Regex para los items: <chk state="true">Texto</chk>
    Pattern pattern = Pattern.compile("<chk state=\"(.*?)\">(.*?)</chk>");
    Matcher matcher = pattern.matcher(data);

    // Limpiar lista antes de cargar si fuera necesario
    // adapterAdjuntos.clear(); 

    while (matcher.find()) {
        boolean isChecked = "true".equalsIgnoreCase(matcher.group(1));
        String texto = matcher.group(2);

        // Inflamos la vista del item_check
        View v = LayoutInflater.from(this).inflate(R.layout.item_check, null);
        android.widget.CheckBox cb = v.findViewById(R.id.chkEstado);
        EditText et = v.findViewById(R.id.txtCheckCuerpo);

        if (cb != null && et != null) {
            cb.setChecked(isChecked);
            et.setText(texto);
            
            // Añadimos al contenedor de la ventana flotante
            // Nota: Asegúrate de que adapterAdjuntos sea tu contenedor de vistas (LinearLayout)
            adapterAdjuntos.addView(v); 
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
    // 1. Validaciones previas
    if (uriArchivoActual == null || isMinimized) return;

    // 2. Preparar el contenido del cuerpo (HTML)
    String bodyHtml = Html.toHtml(floatingTxtNota.getText());
    
    // 3. Obtener el color de fondo actual
    int colorActual = com.google.android.material.color.MaterialColors.getColor(floatingTxtNota, com.google.android.material.R.attr.colorSurfaceContainer);
    if (floatingTxtNota.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
        colorActual = ((android.graphics.drawable.ColorDrawable) floatingTxtNota.getBackground()).getColor();
    }

    // 4. Obtener la lista de vistas del checklist
List<View> checklistViews = new ArrayList<>();
for (int i = 0; i < contenedorAdjuntos.getChildCount(); i++) {
    checklistViews.add(contenedorAdjuntos.getChildAt(i));
}

    // 5. Generar el HTML del checklist usando el Helper
    String checklistHtml = NoteIOHelper.generateChecklistHtml(checklistViews);

    // 6. GUARDAR TODO USANDO EL HELPER
    // IMPORTANTE: Pasamos null en backgroundName y backgroundUri para que 
    // NoteIOHelper mantenga la compatibilidad o use los valores por defecto.
    // Si tienes variables globales para estos en el Service, pásalas aquí.
    boolean exito = NoteIOHelper.saveNote(
            this, 
            uriArchivoActual, 
            bodyHtml, 
            checklistHtml, 
            colorActual, 
            null, // backgroundName (Opcional en el flotante)
            null  // backgroundImageUri (Opcional en el flotante)
    );

    if (!exito) {
        android.util.Log.e("FLOATING_SAVE", "No se pudo guardar la nota");
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
    private void configurarItemCheck(View vistaFila, String texto, boolean marcado) {
    ImageView handle = vistaFila.findViewById(R.id.drag);
    ImageView btnEliminar = vistaFila.findViewById(R.id.btnEliminarCheck); 
    MaterialCheckBox checkBox = vistaFila.findViewById(R.id.chkEstado);
    EditText editText = vistaFila.findViewById(R.id.txtCheckCuerpo);
        
        handle.setVisibility(View.INVISIBLE);

    // 1. Si vienen datos guardados, los ponemos
    if (texto != null) editText.setText(texto);
    checkBox.setChecked(marcado);

    // 2. Lógica del Botón Eliminar
    btnEliminar.setOnClickListener(v -> {

    ((SimpleAdapter)contenedorAdjuntos.getAdapter()).removeView(vistaFila);
    });

    // 3. Tachado automático al marcar (Estético)
    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (isChecked) {
            editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            editText.setTextColor(Color.GRAY);
        } else {
            editText.setPaintFlags(editText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            editText.setTextColor(MaterialColors.getColor(editText, com.google.android.material.R.attr.colorOnSurface));
        }
    });
    // Aplicar estado inicial del tachado
    if (marcado) {
        editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        editText.setTextColor(Color.GRAY);
    }
    }
}