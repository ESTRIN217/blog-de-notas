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
import android.content.Context;

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
    // --- LÓGICA DE HERRAMIENTAS ACTUALIZADA ---

    // 1. Botón SELECCIONAR (Mano) - Este no necesita menú de pincel
    btnSelect.setOnClickListener(v -> {
        lienzo.setModo("SELECTION");
        actualizarEstiloBotones(btnSelect);
        Toast.makeText(this, "Modo Selección", Toast.LENGTH_SHORT).show();
    });

    // 2. Botón MARKER (Marcador) - AHORA CON SELECTOR
    btnMaker.setOnClickListener(v -> {
        if (v.isSelected()) {
            abrirSelectorPincel(); // Si ya estaba activo, abre menú
        } else {
            lienzo.setColor(Color.BLACK); // Color por defecto al elegirlo
            lienzo.setModo("MARKER");
            actualizarEstiloBotones(btnMaker);
        }
    });

    // 3. Botón RESALTADOR - AHORA CON SELECTOR
    btnResaltador.setOnClickListener(v -> {
        if (v.isSelected()) {
            abrirSelectorPincel(); // Si ya estaba activo, abre menú
        } else {
            lienzo.setColor(Color.YELLOW); // Color por defecto al elegirlo
            lienzo.setModo("RESALTADOR");
            actualizarEstiloBotones(btnResaltador);
        }
    });

    // 4. Botón PEN (Ya estaba bien, lo dejamos igual)
    btnPen.setOnClickListener(v -> {
        if (v.isSelected()) {
            abrirSelectorPincel();
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

    btnMore.setOnClickListener(v -> {
    PopupMenu popup = new PopupMenu(this, v);
    popup.getMenuInflater().inflate(R.menu.dibujomenu, popup.getMenu());

    // Truco para forzar iconos (El que ya tenías)
    try {
        java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
        field.setAccessible(true);
        Object menuPopupHelper = field.get(popup);
        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
        java.lang.reflect.Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
        setForceShowIcon.invoke(menuPopupHelper, true);
    } catch (Exception e) {
        e.printStackTrace();
    }

    popup.setOnMenuItemClickListener(item -> {
        int itemId = item.getItemId();

        if (itemId == R.id.mostrar_cuadricula) {
            // 1. Alternar Cuadrícula
            boolean estado = lienzo.toggleCuadricula();
            Toast.makeText(this, estado ? "Cuadrícula Activada" : "Cuadrícula Desactivada", Toast.LENGTH_SHORT).show();
            // (Opcional) Podrías cambiar el icono del item aquí si el menú no se cerrara
            return true;

        } else if (itemId == R.id.borrar) {
            // 2. Borrar
            lienzo.nuevoDibujo();
            Toast.makeText(this, "Lienzo borrado", Toast.LENGTH_SHORT).show();
            return true;

        } else if (itemId == R.id.copiar) {
            // 3. Copiar
            copiarAlPortapapeles();
            return true;

        } else if (itemId == R.id.enviar) {
            // 4. Enviar
            compartirImagen();
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
    // Método auxiliar para guardar el dibujo en caché y obtener su URI
    private Uri obtenerUriTemporal() {
    try {
        Bitmap bitmap = lienzo.getDibujo();
        if (bitmap == null) return null;

        // Guardar en la carpeta de caché (no se queda basura permanente)
        java.io.File cachePath = new java.io.File(getCacheDir(), "images");
        cachePath.mkdirs(); // Crear carpeta si no existe
        
        // Sobreescribimos siempre el mismo archivo temporal
        java.io.File newFile = new java.io.File(cachePath, "dibujo_temp.png");
        java.io.FileOutputStream stream = new java.io.FileOutputStream(newFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        stream.close();

        // Obtener URI con FileProvider
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
        
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
    }

    private void compartirImagen() {
    Uri contentUri = obtenerUriTemporal();
    if (contentUri != null) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Permiso temporal
        shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType("image/png");
        startActivity(Intent.createChooser(shareIntent, "Compartir dibujo con..."));
    } else {
        Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
    }
    }

    private void copiarAlPortapapeles() {
    Uri contentUri = obtenerUriTemporal();
    if (contentUri != null) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // Creamos un ClipData que contiene la URI de la imagen
        android.content.ClipData clip = android.content.ClipData.newUri(getContentResolver(), "Dibujo", contentUri);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Imagen copiada al portapapeles", Toast.LENGTH_SHORT).show();
    } else {
        Toast.makeText(this, "No se pudo copiar", Toast.LENGTH_SHORT).show();
    }
    }
}
