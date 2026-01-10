package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.PopupMenu; // Importar PopupMenu

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileOutputStream;

public class DibujoActivity extends AppCompatActivity {

    private LienzoView lienzo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialogo_dibujo); // Usamos tu XML nuevo

        // 1. Vincular vistas
        lienzo = findViewById(R.id.lienzo);
        ImageView btnAtras = findViewById(R.id.back);
        ImageView btnUndo = findViewById(R.id.undo);
        ImageView btnRedo = findViewById(R.id.redo);
        ImageView btnMore = findViewById(R.id.more);

        // 2. Configurar listeners básicos
        btnAtras.setOnClickListener(v -> finish());
        // Dentro de onCreate
        ImageView btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> guardarYSalir());
        
        btnUndo.setOnClickListener(v -> {
            lienzo.deshacer();
            Toast.makeText(this, "Deshecho", Toast.LENGTH_SHORT).show();
        });

        btnRedo.setOnClickListener(v -> {
            lienzo.rehacer();
            Toast.makeText(this, "Rehecho", Toast.LENGTH_SHORT).show();
        });
        ImageView btnPen = findViewById(R.id.btnPen);
ImageView btnEraser = findViewById(R.id.btnEraser);

btnPen.setOnClickListener(v -> {
    if (v.isSelected()) {
        abrirSelectorPincel();
    } else {
        deseleccionarTodo();
        v.setSelected(true);
        ((ImageView) v).setColorFilter(Color.BLUE);
        lienzo.setModo("PEN");
    }
});

btnEraser.setOnClickListener(v -> {
    deseleccionarTodo();
    v.setSelected(true);
    ((ImageView) v).setColorFilter(Color.BLUE);
    lienzo.setModo("ERASER");
});

        btnMore.setOnClickListener(v -> {
    PopupMenu popup = new PopupMenu(this, v);
    popup.getMenuInflater().inflate(R.menu.dibujomenu, popup.getMenu());
    
    // Forzar que se vean los iconos (Opcional, truco para Android moderno)
    try {
        java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
        field.setAccessible(true);
        Object menuPopupHelper = field.get(popup);
        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
        java.lang.reflect.Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
        setForceShowIcon.invoke(menuPopupHelper, true);
    } catch (Exception e) { e.printStackTrace(); }

    popup.getMenu().add(0, 1001, 0, "Configurar Pincel").setIcon(R.drawable.outline_palette); // Añadimos la opción de pincel programáticamente con icono
    popup.getMenu().add(0, 1002, 1, "Nuevo Dibujo").setIcon(R.drawable.outline_add_box); // Añadir opción para limpiar el lienzo


    popup.setOnMenuItemClickListener(item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.borrar) { // El ID original del menú
            lienzo.nuevoDibujo();
            Toast.makeText(this, "Lienzo borrado", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.copiar) {
            // Implementar lógica para copiar dibujo
            Toast.makeText(this, "Funcionalidad de copiar aún no implementada", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.enviar) {
            // Implementar lógica para enviar dibujo
            Toast.makeText(this, "Funcionalidad de enviar aún no implementada", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == 1001) { // Nuestro ID personalizado para "Configurar Pincel"
            abrirHerramientasPincel();
            return true;
        } else if (itemId == 1002) { // Nuestro ID personalizado para "Nuevo Dibujo"
            lienzo.nuevoDibujo();
            Toast.makeText(this, "Nuevo dibujo iniciado", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    });
    popup.show();
});
    }

    private void guardarYSalir() {
    try {
        Bitmap bitmap = lienzo.getDibujo();
        if (bitmap == null) {
            finish();
            return;
        }

        // Usar formato PNG para mantener transparencia o JPG para menor peso
        String nombre = "dibujo_" + System.currentTimeMillis() + ".png";
        File file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), nombre);
        
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); 
            out.flush();
        }

        Uri uriResult = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent resultIntent = new Intent();
        resultIntent.setData(uriResult);
        setResult(RESULT_OK, resultIntent);
        finish();
    } catch (Exception e) {
        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
    }
    private void abrirHerramientasPincel() {
    com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
        new com.google.android.material.bottomsheet.BottomSheetDialog(this);
    
    View vista = getLayoutInflater().inflate(R.layout.layout_selector_pincel, null);
    
    // 1. Configurar el Slider de Grosor
    com.google.android.material.slider.Slider slider = vista.findViewById(R.id.sliderGrosor);
    slider.setValue(lienzo.getGrosorActual()); // Necesitas crear este getter en LienzoView
    slider.addOnChangeListener((slider1, value, fromUser) -> {
        lienzo.setGrosor(value);
    });

    // 2. Configurar los Colores
    LinearLayout contenedor = vista.findViewById(R.id.contenedorColores);
    int[] misColores = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN};

    for (int col : misColores) {
        View circulo = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
        params.setMargins(10, 10, 10, 10);
        circulo.setLayoutParams(params);
        
        // Crear un círculo visual
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(col);
        circulo.setBackground(shape);

        circulo.setOnClickListener(v -> {
            lienzo.setColor(col);
            dialog.dismiss(); // Cerramos al elegir color
        });
        contenedor.addView(circulo);
    }

    dialog.setContentView(vista);
    dialog.show();
    }
    private void abrirSelectorPincel() {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View layout = getLayoutInflater().inflate(R.layout.layout_selector_pincel, null);
    
    // Slider de grosor
    Slider slider = layout.findViewById(R.id.sliderGrosor);
    slider.setValue(lienzo.getGrosorActual());
    slider.addOnChangeListener((s, value, fromUser) -> lienzo.setGrosor(value));

    // Colores (Estilo Keep)
    LinearLayout contenedor = layout.findViewById(R.id.contenedorColores);
    int[] colores = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
    
    for (int col : colores) {
        View circulo = new View(this);
        int dim = (int)(40 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dim, dim);
        p.setMargins(12, 0, 12, 0);
        circulo.setLayoutParams(p);
        
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(col);
        circulo.setBackground(shape);
        
        circulo.setOnClickListener(v -> {
            lienzo.setColor(col);
            dialog.dismiss();
        });
        contenedor.addView(circulo);
    }

    dialog.setContentView(layout);
    dialog.show();
    }
    private void deseleccionarTodo() {
    int[] ids = {R.id.btnSelect, R.id.btnPen, R.id.btnMarker, R.id.btnEraser, R.id.btnGrid};
    for (int id : ids) {
        View v = findViewById(id);
        if (v instanceof ImageView) { // Verificación de seguridad
            v.setSelected(false);
            ((ImageView) v).clearColorFilter();
        }
    }
    }
}