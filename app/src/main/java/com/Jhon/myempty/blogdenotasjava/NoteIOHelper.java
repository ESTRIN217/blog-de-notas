package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.CheckBox;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteIOHelper {

    // --- LÓGICA DE LECTURA (READING) ---

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

    // 1. Extraer el Color Hexadecimal (Fallback visual)
    public static int extractColor(String html) {
        try {
            // Busca data-bg-color primero, si no, busca style
            Pattern pattern = Pattern.compile("data-bg-color=['\"](#[0-9A-Fa-f]{6,8})['\"]");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return Color.parseColor(matcher.group(1));
            } else {
                // Fallback antiguo
                Pattern pOld = Pattern.compile("background-color:\\s*(#[0-9A-Fa-f]{6,8})");
                Matcher mOld = pOld.matcher(html);
                if (mOld.find()) return Color.parseColor(mOld.group(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Color.WHITE; 
    }

    // 2. Extraer el Nombre del Color (Compatibilidad Material)
    public static String extractBackgroundName(String html) {
        try {
            Pattern pattern = Pattern.compile("data-bg-name=['\"](.*?)['\"]");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // 3. Extraer la URI de la imagen de fondo
    public static String extractBackgroundImageUri(String html) {
        try {
            Pattern pattern = Pattern.compile("data-bg-image-uri=['\"](.*?)['\"]");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static String extractChecklistData(String html) {
        Pattern pattern = Pattern.compile("<div id='checklist_data'.*?>(.*?)</div>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String cleanHtmlForEditor(String html) {
        // 1. Quitar bloque de checklist
        String withoutChecklist = html.replaceAll("<div id='checklist_data'.*?>.*?</div>", "");
        // 2. Quitar el div contenedor principal (que tiene los estilos)
        // Usamos regex no-greedy (.*?) para quitar solo el div de apertura y el de cierre
        String content = withoutChecklist.replaceAll("^<div.*?>", "").replaceAll("</div>$", "");
        
        // Limpieza extra por seguridad si quedaron tags sueltos
        return content.trim();
    }

    // --- LÓGICA DE GUARDADO (SAVING) ---

    /**
     * Guarda la nota con todos los metadatos en el contenedor HTML principal.
     */
    public static boolean saveNote(Context context, Uri uri, String bodyHtml, 
                                   String checklistHtml, int backgroundColor, 
                                   String backgroundName, String backgroundImageUri) {
        if (uri == null) return false;

        String hexColor = String.format("#%06X", (0xFFFFFF & backgroundColor));
        
        // Preparamos los valores para que no sean null
        String safeBgName = (backgroundName != null) ? backgroundName : "default";
        String safeBgUri = (backgroundImageUri != null) ? backgroundImageUri : "";

        // Construimos el HTML.
        // Usamos 'data-attributes' para guardar la info sin afectar necesariamente el renderizado visual,
        // aunque añadimos style para que si se abre en un navegador se vea el color.
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<div id='note_container' ")
                   .append("style='background-color:").append(hexColor).append(";' ")
                   .append("data-bg-color='").append(hexColor).append("' ")
                   .append("data-bg-name='").append(safeBgName).append("' ")
                   .append("data-bg-image-uri='").append(safeBgUri).append("'>");
        
        htmlBuilder.append(bodyHtml);
        
        // Bloque Checklist Oculto
        htmlBuilder.append("<div id='checklist_data' style='display:none;'>")
                   .append(checklistHtml)
                   .append("</div>");
                   
        htmlBuilder.append("</div>"); // Cierre note_container

        try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (os != null) {
                os.write(htmlBuilder.toString().getBytes());
                return true;
            }
        } catch (Exception e) {
            Log.e("NoteIOHelper", "Error saving file: " + e.getMessage());
        }
        return false;
    }
    
}