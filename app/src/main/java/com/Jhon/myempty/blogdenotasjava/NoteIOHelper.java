package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class NoteIOHelper {

    private static final String KEY_TITULO = "titulo";
    private static final String KEY_CONTENIDO = "contenido";
    private static final String KEY_FECHA = "fecha";
    private static final String KEY_COLOR = "color";
    private static final String KEY_URI = "uri";

    public static String readContent(Context context, Uri uri) {
        StringBuilder contentBuilder = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                contentBuilder.append(line);
            }
        } catch (Exception e) {
            Log.e("NoteIOHelper", "Error reading file: " + e.getMessage());
            return "";
        }
        return contentBuilder.toString();
    }

    public static boolean saveNote(Context context, Uri uri, Nota nota) {
        if (uri == null) return false;

        JSONObject jsonObject = aJson(nota);
        if (jsonObject == null) return false;

        try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (os != null) {
                os.write(jsonObject.toString().getBytes());
                return true;
            }
        } catch (Exception e) {
            Log.e("NoteIOHelper", "Error saving file: " + e.getMessage());
        }
        return false;
    }

    public static JSONObject aJson(Nota nota) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_TITULO, nota.getTitulo());
            jsonObject.put(KEY_CONTENIDO, nota.getContenido());
            jsonObject.put(KEY_FECHA, nota.getFecha());
            jsonObject.put(KEY_COLOR, nota.getColor());
            jsonObject.put(KEY_URI, nota.getUri());
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Nota aNota(JSONObject jsonObject) {
        try {
            String titulo = jsonObject.getString(KEY_TITULO);
            String contenido = jsonObject.getString(KEY_CONTENIDO);
            String fecha = jsonObject.getString(KEY_FECHA);
            int color = jsonObject.getInt(KEY_COLOR);
            String uri = jsonObject.getString(KEY_URI);
            return new Nota(titulo, contenido, fecha, color, uri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}