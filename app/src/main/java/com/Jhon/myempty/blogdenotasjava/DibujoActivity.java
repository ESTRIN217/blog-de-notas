package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

        // ID corregido para que coincida con dialogo_dibujo.xml
        lienzoView = findViewById(R.id.lienzo);
        configurarMenuHerramientas();
        manejarIntent();
    }

    private void configurarMenuHerramientas() {
        // IDs corregidos para que coincidan con dialogo_dibujo.xml
        findViewById(R.id.btnGuardarDibujo).setOnClickListener(v -> guardarDibujo());
        findViewById(R.id.btnDeshacer).setOnClickListener(v -> lienzoView.deshacer());
        findViewById(R.id.btnRehacer).setOnClickListener(v -> lienzoView.rehacer());
        findViewById(R.id.btnBorrador).setOnClickListener(v -> lienzoView.modoBorrador());
        findViewById(R.id.btnLapiz).setOnClickListener(v -> lienzoView.modoLapiz());
    }

    private void manejarIntent() {
        Intent intent = getIntent();
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
            file = new File(Uri.parse(uriDeArchivoActual).getPath());
        } else {
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
