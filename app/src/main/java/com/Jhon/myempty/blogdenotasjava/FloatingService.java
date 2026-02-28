package com.Jhon.myempty.blogdenotasjava;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private EditText floatingTxtNota;
    private Uri uriDeArchivoActual;

    public FloatingService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupFloatingView();
    }

    private void setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_editor_layout, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingView, params);

        // IDs revertidos a los originales. Asegúrate de que tu layout los tenga.
        floatingTxtNota = floatingView.findViewById(R.id.floating_txt_nota);
        floatingView.findViewById(R.id.btn_cerrar_flotante).setOnClickListener(v -> stopSelf());
        floatingView.findViewById(R.id.btn_guardar_flotante).setOnClickListener(v -> guardarNota());

        floatingView.findViewById(R.id.floating_header).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
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
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("uri_archivo")) {
            String uriString = intent.getStringExtra("uri_archivo");
            uriDeArchivoActual = Uri.parse(uriString);
            cargarContenido(uriDeArchivoActual);
        }
        return START_STICKY;
    }

    private void cargarContenido(Uri uri) {
        String jsonContent = readContentFromFile(uri);
        if (jsonContent.isEmpty()) {
            floatingTxtNota.setText("Error al cargar la nota.");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonContent);
            String contenido = jsonObject.getString("contenido");
            floatingTxtNota.setText(Html.fromHtml(contenido, Html.FROM_HTML_MODE_LEGACY));
        } catch (Exception e) {
            Log.e("FloatingService", "Error al parsear contenido de la nota", e);
            floatingTxtNota.setText("Formato de nota inválido.");
        }
    }

    private String readContentFromFile(Uri uri) {
        if (uri == null) return "";
        File file = new File(uri.getPath());
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e("FloatingService", "Error leyendo el archivo", e);
            return "";
        }
        return stringBuilder.toString();
    }

    private void guardarNota() {
        if (uriDeArchivoActual == null) {
            Toast.makeText(this, "No se puede guardar, URI no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(uriDeArchivoActual.getPath());
        String originalJsonContent = readContentFromFile(uriDeArchivoActual);
        if (originalJsonContent.isEmpty()) {
            Toast.makeText(this, "Error: No se pudo leer la nota original para guardar.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(originalJsonContent);
            String contenidoActualizado = Html.toHtml(floatingTxtNota.getText());
            jsonObject.put("contenido", contenidoActualizado);

            try (FileOutputStream fos = new FileOutputStream(file, false);
                 OutputStreamWriter writer = new OutputStreamWriter(fos)) {
                writer.write(jsonObject.toString());
                Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e("FloatingService", "Error al guardar la nota", e);
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("FloatingService", "Error al actualizar el JSON para guardar", e);
            Toast.makeText(this, "Error en el formato de la nota", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
