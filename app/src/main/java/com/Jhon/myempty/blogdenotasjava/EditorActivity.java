package com.Jhon.myempty.blogdenotasjava;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class EditorActivity extends AppCompatActivity {

    private EditText titulo;
    private EditText editor;
    private RecyclerView adjuntosRecyclerView;
    private SimpleAdapter adjuntoAdapter;

    private Uri uriDeArchivoActual;
    
    public ActivityResultLauncher<Intent> dibujoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);

        inicializarVistas();
        configurarLanzadores();
        configurarAdaptadores();
        configurarListeners();
        manejarIntent();
    }

    private void inicializarVistas() {
        // IDs corregidos para que coincidan con editor.xml
        titulo = findViewById(R.id.txtTitulo);
        editor = findViewById(R.id.editor_de_texto);
        adjuntosRecyclerView = findViewById(R.id.lista_adjuntos);
    }

    private void configurarLanzadores() {
        dibujoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri dibujoUri = result.getData().getData();
                    if (dibujoUri != null) {
                        ItemAdjunto nuevoDibujo = new ItemAdjunto(ItemAdjunto.TIPO_DIBUJO, dibujoUri.toString(), false);
                        adjuntoAdapter.agregarItem(nuevoDibujo);
                    }
                }
            }
        );
    }

    private void configurarAdaptadores() {
        adjuntoAdapter = new SimpleAdapter(this);
        adjuntosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adjuntosRecyclerView.setAdapter(adjuntoAdapter);
    }

    private void configurarListeners() {
        // Se asume que este ID es correcto o se encuentra en un menú/toolbar
        findViewById(R.id.guardar).setOnClickListener(v -> {
            guardarNota();
            finish();
        });
    }

    private void manejarIntent() {
        Intent intent = getIntent();
        String uriString = intent.getStringExtra("uri_archivo");

        if (uriString != null && !uriString.isEmpty()) {
            uriDeArchivoActual = Uri.parse(uriString);
            cargarContenido(uriDeArchivoActual);
        } else {
            titulo.setText("");
            editor.setText("");
        }
    }

    private void cargarContenido(Uri uri) {
        String jsonContent = readContentFromFile(uri);
        if (jsonContent.isEmpty()) {
            Toast.makeText(this, "Error al cargar la nota.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonContent);
            titulo.setText(jsonObject.getString("titulo"));
            editor.setText(Html.fromHtml(jsonObject.getString("contenido"), Html.FROM_HTML_MODE_LEGACY));

        } catch (Exception e) {
            Log.e("EditorActivity", "Error al parsear el JSON de la nota", e);
        }
    }

    private String readContentFromFile(Uri uri) {
        File file = new File(uri.getPath());
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e("EditorActivity", "Error al leer el archivo: " + uri.toString(), e);
            return "";
        }
        return stringBuilder.toString();
    }

    private void guardarNota() {
        String tituloNota = titulo.getText().toString().trim();
        String contenidoNota = Html.toHtml(editor.getEditableText());

        if (tituloNota.isEmpty()) {
            Toast.makeText(this, "La nota debe tener un título", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("titulo", tituloNota);
            jsonObject.put("contenido", contenidoNota);

        } catch (Exception e) {
            Log.e("EditorActivity", "Error creando el JSON de la nota", e);
            return;
        }

        File file;
        if (uriDeArchivoActual == null) {
            File notesDir = new File(getFilesDir(), "notas");
            if (!notesDir.exists() && !notesDir.mkdirs()) {
                Toast.makeText(this, "Error al crear directorio", Toast.LENGTH_SHORT).show();
                return;
            }
            String nombreArchivo = System.currentTimeMillis() + ".txt";
            file = new File(notesDir, nombreArchivo);
        } else {
            file = new File(uriDeArchivoActual.getPath());
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(jsonObject.toString());
            Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();

            if (uriDeArchivoActual == null) {
                uriDeArchivoActual = Uri.fromFile(file);
            }
        } catch (IOException e) {
            Log.e("EditorActivity", "Error al guardar la nota", e);
        }
    }
}
