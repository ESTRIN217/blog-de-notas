package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static boolean saveNote(Context context, Uri uri, String bodyHtml, String checklistHtml, int color, String backgroundName, String backgroundImageUri) {
        if (uri == null) return false;

        String title = "Untitled";
        DocumentFile file = DocumentFile.fromSingleUri(context, uri);
        if (file != null && file.getName() != null) {
            String name = file.getName();
            if (name.endsWith(".txt")) {
                title = name.substring(0, name.length() - 4);
            } else {
                title = name;
            }
        }

        String fullContent = bodyHtml + checklistHtml;
        String fecha = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        String uriString = uri.toString();

        Nota nota = new Nota(title, fullContent, fecha, color, uriString);
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

    public static String extractChecklistData(String fullContent) {
        Pattern pattern = Pattern.compile("(<chk state=\".*?\">.*?</chk>)");
        Matcher matcher = pattern.matcher(fullContent);
        StringBuilder checklistData = new StringBuilder();
        while (matcher.find()) {
            checklistData.append(matcher.group(1));
        }
        return checklistData.toString();
    }

    public static int extractColor(String fullContent) {
        // This is a placeholder.
        return Color.WHITE;
    }

    public static String cleanHtmlForEditor(String fullContent) {
        // This is a placeholder.
        return fullContent.replaceAll("(<(/)?[a-zA-Z]+>)|(<[a-zA-Z]+\\s*/>)", "");
    }
}