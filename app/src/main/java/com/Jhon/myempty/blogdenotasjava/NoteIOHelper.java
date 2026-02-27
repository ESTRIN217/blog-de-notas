package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private static final String KEY_COLOR = "color";
    private static final String KEY_PATH = "path";

    public static String readContent(Context context, String path) {
        StringBuilder contentBuilder = new StringBuilder();
        try (InputStream is = new FileInputStream(new File(path));
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

    public static boolean saveNote(Context context, String title, String bodyHtml, String checklistHtml, int color, String backgroundName, String backgroundImageUri) {
        if (title == null || title.isEmpty()) {
            title = "Untitled";
        }

        File file = new File(context.getFilesDir(), title + ".txt");
        String fullContent = bodyHtml + checklistHtml;
        String path = file.getAbsolutePath();

        Nota nota = new Nota(title, fullContent, color, path);
        JSONObject jsonObject = aJson(nota);
        if (jsonObject == null) return false;

        try (OutputStream os = new FileOutputStream(file)) {
            os.write(jsonObject.toString().getBytes());
            return true;
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
            jsonObject.put(KEY_COLOR, nota.getColor());
            jsonObject.put(KEY_PATH, nota.getPath()); 
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
            int color = jsonObject.getInt(KEY_COLOR);
            String path = jsonObject.getString(KEY_PATH);
            return new Nota(titulo, contenido, fecha, color, path);
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