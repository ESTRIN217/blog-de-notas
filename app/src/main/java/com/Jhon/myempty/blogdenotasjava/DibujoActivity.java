package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.PopupMenu; // Importar PopupMenu
import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import android.graphics.BitmapFactory;
import androidx.activity.EdgeToEdge;
import android.content.Context;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;

public class DibujoActivity extends AppCompatActivity {

  private LienzoView lienzo;
  private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TabLayout tabLayout;
    private MaterialButton btnAtras, btnUndo, btnRedo, btnMore, btnDone, btnAccionDeseleccionar, btnAccionBorrarLienzo ;
    private Slider sliderGrosor;
    // ... tus otras variables
    private LinearLayout seccionColores, contenedorColores;
    
    // Lista de colores predefinidos (Negro, Rojo, Azul, Verde, Amarillo, Blanco, etc.)
    private final int[] PALETA_COLORES = {
            Color.BLACK, Color.RED, Color.BLUE, 0xFF008000, // Verde oscuro
            Color.YELLOW, 0xFFFF00FF, // Magenta
            0xFFFFA500, // Naranja
            Color.GRAY, Color.WHITE
    };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    androidx.activity.EdgeToEdge.enable(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialogo_dibujo);

    // 1. Vincular vistas
    lienzo = findViewById(R.id.lienzo);
    btnAtras = findViewById(R.id.back);
    btnUndo = findViewById(R.id.undo);
    btnRedo = findViewById(R.id.redo);
    btnMore = findViewById(R.id.more);
        btnDone = findViewById(R.id.btnDone);
        sliderGrosor = findViewById(R.id.sliderGrosor);
        tabLayout = findViewById(R.id.tabLayoutHerramientas);
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        seccionColores = findViewById(R.id.seccionColores);
        contenedorColores = findViewById(R.id.contenedorColores);
        btnAccionDeseleccionar = findViewById(R.id.btnAccionDeseleccionar);
        btnAccionBorrarLienzo = findViewById(R.id.btnAccionBorrarLienzo);
        
                // 2. Configurar BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        // Altura visible inicial (solo los tabs)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); 
        
        // 3. Configurar TABS (Las 5 herramientas)
        configurarTabs();

        // 4. Listeners de Tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                cambiarHerramienta(tab.getPosition());
                
                // Opcional: Expandir el sheet un poco para mostrar sliders si se selecciona herramienta
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                   // bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Si toca de nuevo, expandimos/colapsamos ajustes
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        // 5. Configurar Sliders y Botones superiores
        configurarControles();

        // 6. Cargar imagen existente si venimos a editar
        manejarIntentsDeEdicion();
        cargarPaletaDeColores();
        // Seleccionar herramienta por defecto (Pluma = índice 1)
        TabLayout.Tab tabDefecto = tabLayout.getTabAt(1);
        if (tabDefecto != null) tabDefecto.select();
  }
    
        private void configurarTabs() {
        
        // Tab 0: Seleccionar
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.round_select_all)); 
        
        // Tab 1: Pluma (Pen)
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ink_pen_24px)); 
        
        // Tab 2: Marcador (Marker)
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ink_marker_24px)); 
        
        // Tab 3: Resaltador (Highlighter)
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ink_highlighter_24px)); 
        
        // Tab 4: Borrador (Eraser)
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ink_eraser_24px)); 
    }
    
        private void cambiarHerramienta(int posicion) {
        // 1. Resetear visibilidad (ocultar todo lo especial)
        seccionColores.setVisibility(View.VISIBLE);
        btnAccionDeseleccionar.setVisibility(View.GONE);
        btnAccionBorrarLienzo.setVisibility(View.GONE);
        sliderGrosor.setVisibility(View.VISIBLE);

        switch (posicion) {
            case 0: // Select
                lienzo.activarSeleccion();
                // En modo selección, ocultamos colores y mostramos "Deseleccionar"
                seccionColores.setVisibility(View.GONE);
                sliderGrosor.setVisibility(View.GONE); // Opcional: ocultar grosor
                btnAccionDeseleccionar.setVisibility(View.VISIBLE);
                break;

            case 1: // Pluma
                lienzo.activarPluma();
                sliderGrosor.setValue(5f);
                break;

            case 2: // Marcador
                lienzo.activarMarcador();
                sliderGrosor.setValue(10f);
                break;

            case 3: // Resaltador
                lienzo.activarResaltador();
                sliderGrosor.setValue(20f);
                break;

            case 4: // Borrador
                lienzo.activarBorrador();
                // En modo borrador, ocultamos colores y mostramos "Borrar Todo"
                seccionColores.setVisibility(View.GONE);
                btnAccionBorrarLienzo.setVisibility(View.VISIBLE);
                sliderGrosor.setValue(30f);
                break;
        }
    }

    private void configurarControles() {
        // Slider Grosor
        sliderGrosor.addOnChangeListener((slider, value, fromUser) -> {
            lienzo.setGrosor(value); 
        });

        // Botones superiores
        btnAtras.setOnClickListener(v -> finish());

        btnUndo.setOnClickListener(v -> {
            if (lienzo != null) lienzo.deshacer();
            Toast.makeText(this, "Deshecho", Toast.LENGTH_SHORT).show();
        });

        btnRedo.setOnClickListener(v -> {
            if (lienzo != null) lienzo.rehacer();
            Toast.makeText(this, "Rehecho", Toast.LENGTH_SHORT).show();
        });

        btnDone.setOnClickListener(v -> guardarYSalir());

        // Menú de opciones extra (Cuadrícula, Copiar, etc.)
        btnMore.setOnClickListener(v -> mostrarMenuMore(v));
        btnAccionDeseleccionar.setOnClickListener(v -> {
            if (lienzo != null) {
                // Asegúrate de tener este método en LienzoView o crea uno que llame a objetoSeleccionado = null
                lienzo.deseleccionarTodo(); // Necesitas crear este método puente en LienzoView
                Toast.makeText(this, "Selección eliminada", Toast.LENGTH_SHORT).show();
            }
        });

        btnAccionBorrarLienzo.setOnClickListener(v -> {
            if (lienzo != null) {
                lienzo.nuevoDibujo(); // Este ya existe
                Toast.makeText(this, "Lienzo borrado", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void mostrarMenuMore(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.dibujomenu, popup.getMenu());

        // Truco para mostrar iconos
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
                boolean estado = lienzo.toggleCuadricula();
                Toast.makeText(this, estado ? "Cuadrícula Activada" : "Cuadrícula Desactivada", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.borrar) {
                lienzo.nuevoDibujo();
                Toast.makeText(this, "Lienzo borrado", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.copiar) {
                copiarAlPortapapeles();
                return true;
            } else if (itemId == R.id.enviar) {
                compartirImagen();
                return true;
            }
            return false;
        });
        popup.show();
    }
    
    private void manejarIntentsDeEdicion() {
        if (getIntent().hasExtra("uri_dibujo_editar") || getIntent().hasExtra("uri_foto_editar")) {
            String ruta = getIntent().getStringExtra("uri_dibujo_editar");
            if (ruta == null) ruta = getIntent().getStringExtra("uri_foto_editar");

            if (ruta != null) {
                String finalRuta = ruta;
                lienzo.post(() -> {
                    try {
                        Uri uri = Uri.parse(finalRuta);
                        java.io.InputStream is = getContentResolver().openInputStream(uri);
                        Bitmap bitmapExistente = android.graphics.BitmapFactory.decodeStream(is);
                        if (is != null) is.close();

                        if (bitmapExistente != null) {
                            lienzo.cargarFondo(bitmapExistente);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al cargar imagen para editar", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

  private void guardarYSalir() {
        try {
            Bitmap bitmap = lienzo.getDibujo();
            if (bitmap == null) {
                finish();
                return;
            }
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

    // --- Métodos de Ayuda para Compartir ---
    private Uri obtenerUriTemporal() {
        try {
            Bitmap bitmap = lienzo.getDibujo();
            if (bitmap == null) return null;

            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs(); 
            
            File newFile = new File(cachePath, "dibujo_temp.png");
            FileOutputStream stream = new FileOutputStream(newFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

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
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
            android.content.ClipData clip = android.content.ClipData.newUri(getContentResolver(), "Dibujo", contentUri);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Imagen copiada al portapapeles", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No se pudo copiar", Toast.LENGTH_SHORT).show();
        }
    }
    private void cargarPaletaDeColores() {
        contenedorColores.removeAllViews();
        int tamaño = dpToPx(40); // Tamaño del círculo
        int margen = dpToPx(8);

        for (int color : PALETA_COLORES) {
            View vistaColor = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tamaño, tamaño);
            params.setMargins(0, 0, margen, 0);
            vistaColor.setLayoutParams(params);

            // Crear fondo circular dinámico
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            // Borde gris suave para que se vea el color blanco
            drawable.setStroke(dpToPx(1), 0xFFCCCCCC); 
            
            vistaColor.setBackground(drawable);
            vistaColor.setClickable(true);

            // Click en el color
            vistaColor.setOnClickListener(v -> {
                if (lienzo != null) {
                    lienzo.setNuevoColor(color); // Asegúrate de tener este método en LienzoView
                    Toast.makeText(this, "Color cambiado", Toast.LENGTH_SHORT).show();
                }
            });

            contenedorColores.addView(vistaColor);
        }
    }

    // Utilidad para convertir dp a px
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
