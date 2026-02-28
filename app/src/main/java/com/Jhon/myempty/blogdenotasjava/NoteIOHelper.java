package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteIOHelper {

    private static final String KEY_TITULO = "titulo";
    private static final String KEY_CONTENIDO = "contenido";
    private static final String KEY_COLOR = "color";
    private static final String KEY_PATH = "path"; // En el JSON, "path" almacena la URI de la nota

    // Convertir un objeto Nota a un objeto JSON
    public static JSONObject aJson(Nota nota) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_TITULO, nota.getTitulo());
            jsonObject.put(KEY_CONTENIDO, nota.getContenido());
            jsonObject.put(KEY_COLOR, nota.getColor());
            jsonObject.put(KEY_PATH, nota.getUri()); // Usar getUri() en lugar del antiguo getPath()
            return jsonObject;
        } catch (Exception e) {
            Log.e("NoteIOHelper", "Error al convertir Nota a JSON", e);
            return null;
        }
    }

    // Convertir un objeto JSON a un objeto Nota
    public static Nota aNota(JSONObject jsonObject) {
        try {
            String titulo = jsonObject.getString(KEY_TITULO);
            String contenido = jsonObject.getString(KEY_CONTENIDO);
            int color = jsonObject.optInt(KEY_COLOR, Color.WHITE); // Usar optInt para seguridad
            String path = jsonObject.getString(KEY_PATH); // El path es la URI guardada

            // Usar el constructor corregido que no requiere fecha
            return new Nota(titulo, contenido, color, path);
        } catch (Exception e) {
            Log.e("NoteIOHelper", "Error al convertir JSON a Nota", e);
            return null;
        }
    }

    // --- Métodos de ayuda (probablemente ya no se usan activamente pero se mantienen por si acaso) ---

    public static String readContent(Context context, String path) {
        StringBuilder contentBuilder = new StringBuilder();
        try (InputStream is = new FileInputStream(new File(path));
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                contentBuilder.append(line);
            }
        } catch (Exception e) {
            Log.e("NoteIOHelper", "Error leyendo archivo: " + e.getMessage());
            return "";
        }
        return contentBuilder.toString();
    }

    public static boolean saveNote(Context context, String title, String bodyHtml, String checklistHtml, int color) {
        if (title == null || title.isEmpty()) {
            title = "Untitled";
        }

        File file = new File(context.getFilesDir(), title + ".txt");
        String fullContent = bodyHtml + checklistHtml;
        String uri = Uri.fromFile(file).toString();

        Nota nota = new Nota(title, fullContent, color, uri);
        JSONObject jsonObject = aJson(nota);
        if (jsonObject == null) return false;

        try (OutputStream os = new FileOutputStream(file)) {
            os.write(jsonObject.toString().getBytes());
            return true;
        } catch (Exception e) {
            Log.e("NoteIOHelper", "Error guardando nota: " + e.getMessage());
        }
        return false;
    }

    public static String extractChecklistData(String fullContent) {
        Pattern pattern = Pattern.compile("(<chk state=\".*?\">.*?</chk>)");
        Matcher matcher = pattern.matcher(fullContent);
        StringBuilder checklistData = new StringBuilder();
        while (matcher.find()) {
            checklistData.append(matcher.group(1));
        }
        return checklistData.toString();
    }

    public static String cleanHtmlForEditor(String fullContent) {
        return fullContent.replaceAll("(<(/)?[a-zA-Z]+>)|(<[a-zA-Z]+\\s*/>)", "");
    }
}
