package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DibujoActivity extends AppCompatActivity {

    private LienzoView lienzoView;
    private String uriDeArchivoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialogo_dibujo);

        lienzoView = findViewById(R.id.lienzoView);
        configurarMenuHerramientas();
        manejarIntent();
    }

    private void configurarMenuHerramientas() {
        findViewById(R.id.btn_guardar_dibujo).setOnClickListener(v -> guardarDibujo());
        findViewById(R.id.btn_deshacer).setOnClickListener(v -> lienzoView.deshacer());
        findViewById(R.id.btn_rehacer).setOnClickListener(v -> lienzoView.rehacer());
        findViewById(R.id.btn_borrador).setOnClickListener(v -> lienzoView.modoBorrador());
        findViewById(R.id.btn_lapiz).setOnClickListener(v -> lienzoView.modoLapiz());
        // Añade más listeners para otras herramientas si es necesario
    }

    private void manejarIntent() {
        Intent intent = getIntent();
        // Se unifica la carga, ya sea un dibujo o una foto para editar
        if (intent.hasExtra("uri_dibujo_editar")) {
            uriDeArchivoActual = intent.getStringExtra("uri_dibujo_editar");
            cargarImagenParaEdicion(uriDeArchivoActual);
        }
    }

    private void cargarImagenParaEdicion(String uriString) {
        try {
            File file = new File(Uri.parse(uriString).getPath());
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                lienzoView.setBitmap(bitmap);
            } else {
                Toast.makeText(this, "No se encontró el archivo para editar", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("DibujoActivity", "Error al cargar la imagen para editar", e);
            Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarDibujo() {
        Bitmap bitmap = lienzoView.getBitmap();

        File file;
        if (uriDeArchivoActual != null) {
            // Si estamos editando, sobrescribimos el archivo existente
            file = new File(Uri.parse(uriDeArchivoActual).getPath());
        } else {
            // Si es un dibujo nuevo, creamos un nuevo archivo
            File adjuntosDir = new File(getFilesDir(), "adjuntos");
            if (!adjuntosDir.exists() && !adjuntosDir.mkdirs()) {
                Toast.makeText(this, "Error al crear carpeta de adjuntos", Toast.LENGTH_SHORT).show();
                return;
            }
            String nombreArchivo = "dibujo_" + System.currentTimeMillis() + ".png";
            file = new File(adjuntosDir, nombreArchivo);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            Toast.makeText(this, "Dibujo guardado", Toast.LENGTH_SHORT).show();

            // Devolver la URI del archivo guardado a la actividad que nos llamó (EditorActivity)
            Intent resultadoIntent = new Intent();
            resultadoIntent.setData(Uri.fromFile(file));
            setResult(RESULT_OK, resultadoIntent);
            finish();

        } catch (IOException e) {
            Log.e("DibujoActivity", "Error al guardar el dibujo", e);
            Toast.makeText(this, "No se pudo guardar el dibujo", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
