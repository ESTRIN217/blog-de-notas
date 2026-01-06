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
    private TextView floatingTitleText;
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
        
        // --- CORRECCIÓN: Llamar aquí para aplicar tamaño y colores al crear la vista ---
        aplicarPreferenciasVisuales(); 
        
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
            String contenido = intent.getStringExtra("contenido_nota");
            String uriString = intent.getStringExtra("uri_archivo");

            if (uriString != null && !uriString.isEmpty()) {
                uriArchivoActual = Uri.parse(uriString);
                DocumentFile df = DocumentFile.fromSingleUri(this, uriArchivoActual);
                if (df != null && df.getName() != null) {
                    floatingTitleText.setText(df.getName().replace(".txt", ""));
                }
            }

            if (floatingTxtNota != null && contenido != null) {
                floatingTxtNota.setText(contenido);
                // --- MEJORA: Re-aplicar preferencias por si se cambiaron mientras el servicio corría ---
                aplicarPreferenciasVisuales(); 
                
                try {
                    floatingTxtNota.setSelection(floatingTxtNota.getText().length());
                } catch (Exception ignored) {}
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

        // 5. Botones
        btnClose.setOnClickListener(v -> stopSelf());
        btnModify.setOnClickListener(v -> {
            guardarNotaFlotante();
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("nombre_archivo", uriArchivoActual != null ? uriArchivoActual.toString() : "");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            stopSelf();
        });
    }

    private void guardarNotaFlotante() {
        if (uriArchivoActual == null) return;
        try (OutputStream os = getContentResolver().openOutputStream(uriArchivoActual, "wt")) {
            if (os != null) {
                os.write(floatingTxtNota.getText().toString().getBytes());
            }
        } catch (Exception e) { e.printStackTrace(); }
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
    // Agrega este método dentro de FloatingService
    private void aplicarPreferenciasVisuales() {
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        
        // 1. Tamaño de Fuente
        float fontSize = prefs.getFloat("editor_font_size", 16f);
        if (floatingTxtNota != null) {
            floatingTxtNota.setTextSize(fontSize);
        }

        // 2. Color de Fondo
        int bgMode = prefs.getInt("editor_bg_mode", 0);
        
        // Usamos Integer en lugar de int para manejar el nulo si es modo "Sistema"
        Integer colorFondo = null;
        Integer colorTexto = null;

        switch (bgMode) {
            case 1: // Papel
                colorFondo = 0xFFFFF8E1;
                colorTexto = 0xFF3E2723;
                break;
            case 2: // Negro
                colorFondo = 0xFF000000;
                colorTexto = 0xFFFFFFFF;
                break;
        }

        if (floatingTxtNota != null && colorFondo != null) {
            floatingTxtNota.setBackgroundColor(colorFondo);
            floatingTxtNota.setTextColor(colorTexto);
        } else if (floatingTxtNota != null) {
            // Si es modo sistema (0), quitamos colores forzados para que use el XML
            floatingTxtNota.setBackgroundColor(Color.TRANSPARENT); 
            // Aquí podrías poner el color por defecto de tu tema si fuera necesario
        }
    }
}