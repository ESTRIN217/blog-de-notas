package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
    private ArrayList<ItemAdjunto> listaAdjuntos = new ArrayList<>();

    private Uri uriDeArchivoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);

        inicializarVistas();
        configurarListeners();
        manejarIntent();
    }

    private void inicializarVistas() {
        titulo = findViewById(R.id.titulo);
        editor = findViewById(R.id.editor);
        adjuntosRecyclerView = findViewById(R.id.adjuntos);
        // Configurar RecyclerView para adjuntos si es necesario
    }

    private void configurarListeners() {
        // Ejemplo de un botón de guardado
        findViewById(R.id.guardar).setOnClickListener(v -> {
            guardarNota();
            finish(); // Cierra el editor después de guardar
        });
    }

    private void manejarIntent() {
        Intent intent = getIntent();
        String uriString = intent.getStringExtra("uri_archivo");

        if (uriString != null && !uriString.isEmpty()) {
            uriDeArchivoActual = Uri.parse(uriString);
            cargarContenido(uriDeArchivoActual);
        } else {
            // Es una nota nueva, no se carga nada
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
            String tituloNota = jsonObject.getString("titulo");
            String contenidoNota = jsonObject.getString("contenido");

            titulo.setText(tituloNota);
            editor.setText(Html.fromHtml(contenidoNota, Html.FROM_HTML_MODE_LEGACY));

            // Lógica para cargar adjuntos si existe
            if (jsonObject.has("adjuntos")) {
                JSONArray adjuntosArray = jsonObject.getJSONArray("adjuntos");
                // ... procesar y mostrar adjuntos
            }

        } catch (Exception e) {
            Log.e("EditorActivity", "Error al parsear el JSON de la nota", e);
            Toast.makeText(this, "El formato de la nota es inválido.", Toast.LENGTH_SHORT).show();
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
            // Lógica para guardar adjuntos en el JSON
            // jsonObject.put("adjuntos", new JSONArray(...));
        } catch (Exception e) {
            Log.e("EditorActivity", "Error creando el JSON de la nota", e);
            return; // No guardamos si el JSON no se puede crear
        }

        File file;
        if (uriDeArchivoActual == null) {
            // Nota nueva: crear un archivo en almacenamiento interno
            File notesDir = new File(getFilesDir(), "notas");
            if (!notesDir.exists() && !notesDir.mkdirs()) {
                Log.e("EditorActivity", "No se pudo crear el directorio de notas");
                Toast.makeText(this, "Error al crear directorio", Toast.LENGTH_SHORT).show();
                return;
            }
            String nombreArchivo = System.currentTimeMillis() + ".txt";
            file = new File(notesDir, nombreArchivo);
        } else {
            // Nota existente: obtener el archivo desde la URI
            file = new File(uriDeArchivoActual.getPath());
        }

        // Escribir el contenido JSON al archivo
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(jsonObject.toString());
            Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();

            // Si era una nota nueva, actualizamos la URI para futuros guardados
            if (uriDeArchivoActual == null) {
                uriDeArchivoActual = Uri.fromFile(file);
            }
        } catch (IOException e) {
            Log.e("EditorActivity", "Error al guardar la nota", e);
            Toast.makeText(this, "Error al guardar la nota", Toast.LENGTH_SHORT).show();
        }
    }
}
