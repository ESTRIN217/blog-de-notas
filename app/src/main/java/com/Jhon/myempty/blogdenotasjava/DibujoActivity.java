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
import android.graphics.BitmapFactory;
import androidx.activity.EdgeToEdge;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;

public class DibujoActivity extends AppCompatActivity {

  private LienzoView lienzo;
  private ImageView btnSelect, btnMaker, btnResaltador, btnAtras, btnUndo, btnRedo, btnMore, btnPen, btnEraser;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    androidx.activity.EdgeToEdge.enable(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialogo_dibujo); // Usamos tu XML nuevo

    // 1. Vincular vistas
    lienzo = findViewById(R.id.lienzo);
    btnAtras = findViewById(R.id.back);
    btnUndo = findViewById(R.id.undo);
    btnRedo = findViewById(R.id.redo);
    btnMore = findViewById(R.id.more);
    btnPen = findViewById(R.id.btnPen);
    btnEraser = findViewById(R.id.btnEraser);
    btnSelect = findViewById(R.id.btnSelect);
    btnMaker = findViewById(R.id.btnMarker);
    btnResaltador = findViewById(R.id.btnResaltador);

    if (getIntent().hasExtra("uri_dibujo_editar") || getIntent().hasExtra("uri_foto_editar")) {
      String ruta = getIntent().getStringExtra("uri_dibujo_editar");
      if (ruta == null) ruta = getIntent().getStringExtra("uri_foto_editar");

      if (ruta != null) {
        // Usamos post para asegurar que el lienzo ya tenga dimensiones antes de cargar el fondo
        String finalRuta = ruta;
        lienzo.post(
            () -> {
              try {
                Uri uri = Uri.parse(finalRuta);
                java.io.InputStream is = getContentResolver().openInputStream(uri);
                android.graphics.Bitmap bitmapExistente =
                    android.graphics.BitmapFactory.decodeStream(is);
                if (is != null) is.close();

                if (bitmapExistente != null) {
                  lienzo.cargarFondo(bitmapExistente);
                }
              } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al cargar imagen para editar", Toast.LENGTH_SHORT)
                    .show();
              }
            });
      }
    }

    // 2. Configurar listeners básicos
    btnAtras.setOnClickListener(v -> finish());
    // Dentro de onCreate
    ImageView btnDone = findViewById(R.id.btnDone);
    btnDone.setOnClickListener(v -> guardarYSalir());

    btnUndo.setOnClickListener(
        v -> {
          lienzo.deshacer();
          Toast.makeText(this, "Deshecho", Toast.LENGTH_SHORT).show();
        });

    btnRedo.setOnClickListener(
        v -> {
          lienzo.rehacer();
          Toast.makeText(this, "Rehecho", Toast.LENGTH_SHORT).show();
        });

    // --- LÓGICA DE HERRAMIENTAS ---

    // 1. Botón SELECCIONAR (Mano)
    btnSelect.setOnClickListener(v -> {
    lienzo.setModo("SELECTION");
    actualizarEstiloBotones(btnSelect);
    Toast.makeText(this, "Modo Selección", Toast.LENGTH_SHORT).show();
    });

    // 2. Botón MARKER (Marcador / Rotulador grueso)
    btnMaker.setOnClickListener(v -> {
    lienzo.setColor(Color.BLACK); // Opcional: forzar negro o dejar el actual
    lienzo.setModo("MARKER");
    actualizarEstiloBotones(btnMaker);
    });

    // 3. Botón RESALTADOR (Transparente)
    btnResaltador.setOnClickListener(v -> {
    // Para resaltador, solemos querer amarillo por defecto, 
    // pero puedes dejar que el usuario elija color.
    lienzo.setColor(Color.YELLOW); 
    lienzo.setModo("RESALTADOR");
    actualizarEstiloBotones(btnResaltador);
    });

    // 4. Actualizar también el listener del btnPen existente para usar la nueva lógica visual
    btnPen.setOnClickListener(v -> {
    if (v.isSelected()) {
        abrirSelectorPincel(); // Si ya estaba activo, abre menú
    } else {
        lienzo.setModo("PEN");
        actualizarEstiloBotones(btnPen);
    }
    });

    // 5. Actualizar el Borrador
    btnEraser.setOnClickListener(v -> {
    lienzo.setModo("ERASER");
    actualizarEstiloBotones(btnEraser);
    });

    btnMore.setOnClickListener(
        v -> {
          PopupMenu popup = new PopupMenu(this, v);
          popup.getMenuInflater().inflate(R.menu.dibujomenu, popup.getMenu());

          // Forzar que se vean los iconos (Opcional, truco para Android moderno)
          try {
            java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(popup);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            java.lang.reflect.Method setForceShowIcon =
                classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);
          } catch (Exception e) {
            e.printStackTrace();
          }

          popup.setOnMenuItemClickListener(
              item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.borrar) { // El ID original del menú
                  lienzo.nuevoDibujo();
                  Toast.makeText(this, "Lienzo borrado", Toast.LENGTH_SHORT).show();
                  return true;
                } else if (itemId == R.id.copiar) {
                  // Implementar lógica para copiar dibujo
                  Toast.makeText(
                          this, "Funcionalidad de copiar aún no implementada", Toast.LENGTH_SHORT)
                      .show();
                  return true;
                } else if (itemId == R.id.enviar) {
                  // Implementar lógica para enviar dibujo
                  Toast.makeText(
                          this, "Funcionalidad de enviar aún no implementada", Toast.LENGTH_SHORT)
                      .show();
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

  private void abrirSelectorPincel() {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View layout = getLayoutInflater().inflate(R.layout.layout_selector_pincel, null);

    // Slider de grosor
    Slider slider = layout.findViewById(R.id.sliderGrosor);
    slider.setValue(lienzo.getGrosorActual());
    slider.addOnChangeListener((s, value, fromUser) -> lienzo.setGrosor(value));

    // Colores (Estilo Keep)
    LinearLayout contenedor = layout.findViewById(R.id.contenedorColores);
    int[] colores = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN};

    for (int col : colores) {
      View circulo = new View(this);
      int dim = (int) (40 * getResources().getDisplayMetrics().density);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dim, dim);
      p.setMargins(12, 0, 12, 0);
      circulo.setLayoutParams(p);

      GradientDrawable shape = new GradientDrawable();
      shape.setShape(GradientDrawable.OVAL);
      shape.setColor(col);
      circulo.setBackground(shape);

      circulo.setOnClickListener(
          v -> {
            lienzo.setColor(col);
            dialog.dismiss();
          });
      contenedor.addView(circulo);
    }

    dialog.setContentView(layout);
    dialog.show();
  }

    private void deseleccionarTodo() {
    int[] ids = {R.id.btnSelect, R.id.btnPen, R.id.btnMarker, R.id.btnEraser,};
    for (int id : ids) {
      View v = findViewById(id);
      if (v instanceof ImageView) { // Verificación de seguridad
        v.setSelected(false);
        ((ImageView) v).clearColorFilter();
      }
    }
    }
    private void actualizarEstiloBotones(View botonActivo) {
    // Lista de todos los botones de herramientas
    View[] botones = {findViewById(R.id.btnPen), btnMaker, btnResaltador, btnSelect, findViewById(R.id.btnEraser)};

    for (View btn : botones) {
        if (btn == null) continue;

        if (btn == botonActivo) {
            btn.setSelected(true);
            // Si es ImageView, le ponemos tinte azul
            if (btn instanceof ImageView) {
                ((ImageView) btn).setColorFilter(Color.BLUE);
            }
        } else {
            btn.setSelected(false);
            if (btn instanceof ImageView) {
                ((ImageView) btn).clearColorFilter();
            }
        }
    }
    }
}
