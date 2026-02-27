package com.Jhon.myempty.blogdenotasjava;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.UriPermission;
import android.net.Uri;
import android.util.Log;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.File;

public class MainActivity extends AppCompatActivity {

  private RecyclerView recyclerNotas;
  private FloatingActionButton btnNuevaNota;
  private EditText buscar;
  private MaterialButton modoVistaTargeta, btnMenu, orden;
  private DrawerLayout drawerLayout;
  private NavigationView navigationView;

  private List<Nota> listaDeNotasCompleta;
  private NotaAdapter adaptador;

  private static final String PREFS_NAME = "MisPreferencias";
  private static final String KEY_THEME = "tema_elegido";
  private static final String KEY_VISTA_GRID = "vista_en_cuadricula";

  private SharedPreferences sharedPreferences;
  private boolean esModoCuadricula = false;
  private String carpetaUriString;
  // Valores: 0 = Modificación, 1 = Creación, 2 = Nombre
  private int criterioOrdenActual = 0;

  private final ActivityResultLauncher<Intent> activityLauncherEditor =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> cargarNotas() // Al volver del editor, recargamos la lista
          );

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // 1. Configuración de Temas (Debe ir ANTES de super.onCreate)
    sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    if (sharedPreferences.getBoolean("material_theme_activado", true)) {
      DynamicColors.applyToActivityIfAvailable(this);
    }
    int temaGuardado =
        sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    AppCompatDelegate.setDefaultNightMode(temaGuardado);

    super.onCreate(savedInstanceState);

    // 2. Configuración de Pantalla Completa (Edge-to-Edge)
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    String uriDesdeExtra = getIntent().getStringExtra("extra_carpeta_uri");
    
    if (uriDesdeExtra != null) {
        // Guardamos la URI que nos enviaron
        sharedPreferences.edit().putString("carpeta_uri", uriDesdeExtra).apply();
        carpetaUriString = uriDesdeExtra;
    } else {
        // Si no hay extra, cargamos la que ya estaba guardada
        carpetaUriString = sharedPreferences.getString("carpeta_uri", null);
    }

    // 3. Validación de seguridad para evitar entrar sin carpeta
    if (carpetaUriString == null) {
        Log.e("ERROR", "No hay URI ni en extra ni en Prefs, redirigiendo...");
        Intent intent = new Intent(this, InicioActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return;
    }
    setContentView(R.layout.activity_main);

    // 3. Inicializar componentes
    inicializarVistas();
    // 1. Configurar el Callback (Solo una vez)
ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
    
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        if (adaptador == null) return false;
        
        int fromPos = viewHolder.getBindingAdapterPosition(); // Usa BindingAdapterPosition
        int toPos = target.getBindingAdapterPosition();

        adaptador.moverNota(fromPos, toPos);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            viewHolder.itemView.setAlpha(0.7f);
            viewHolder.itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start();
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setAlpha(1.0f);
        viewHolder.itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
        
        guardarOrdenPersonalizado();
    }
};

// 2. Unirlo al RecyclerView (Solo una vez)
ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
itemTouchHelper.attachToRecyclerView(recyclerNotas);
    configurarListeners();

    // 4. Recuperar URI y Preferencias
    carpetaUriString = sharedPreferences.getString("carpeta_uri", null);
    esModoCuadricula = sharedPreferences.getBoolean(KEY_VISTA_GRID, false);

cargarNotas();
  }


  private void inicializarVistas() {
    recyclerNotas = findViewById(R.id.recyclerNotas);
    btnNuevaNota = findViewById(R.id.btnNuevaNota);
    buscar = findViewById(R.id.buscar);
    modoVistaTargeta = findViewById(R.id.modoVistaTargeta);
    listaDeNotasCompleta = new ArrayList<>();
    drawerLayout = findViewById(R.id.drawer_layout);
    navigationView = findViewById(R.id.navigation_view_start);
    btnMenu = findViewById(R.id.navegation_menu);
    orden = findViewById(R.id.orden);
  }

  private void configurarListeners() {
    btnNuevaNota.setOnClickListener(v -> abrirEditor(""));

    modoVistaTargeta.setOnClickListener(
        v -> {
          esModoCuadricula = !esModoCuadricula;
          sharedPreferences.edit().putBoolean(KEY_VISTA_GRID, esModoCuadricula).apply();
          aplicarModoVista();
        });

    buscar.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            filtrarLista(s.toString());
          }

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void afterTextChanged(Editable s) {}
        });
    btnMenu.setOnClickListener(
        v -> {
          drawerLayout.openDrawer(GravityCompat.START);
        });
    // Manejar clics en los items del menú lateral
    navigationView.setNavigationItemSelectedListener(
        item -> {
          int id = item.getItemId();

          if (id == R.id.settings) {
            // Tu lógica()
            startActivity(new Intent(this, SettingsActivity.class));
          } else if (id == R.id.nav_sobre) { 
            startActivity(new Intent(this, SobreActivity.class));
          }

          drawerLayout.closeDrawer(GravityCompat.START);
          return true;
        });
    orden.setOnClickListener(
        v -> {
          ordenarPor();
        });
  }

  // --- MÉTODO PRINCIPAL DE CARGA (OPTIMIZADO CON HILO Y ORDENAMIENTO) ---
  private void cargarNotas() {
    if (carpetaUriString == null) {
      Log.d("DEBUG", "URI es nula");
      return;
    }

    Uri treeUri = Uri.parse(carpetaUriString);
    DocumentFile root = DocumentFile.fromTreeUri(this, treeUri);

    if (root != null && root.canRead()) {
      new Thread(
              () -> {
                List<Nota> notasTemp = new ArrayList<>();
                DocumentFile[] archivos = root.listFiles();

                Log.d("DEBUG", "Archivos encontrados: " + (archivos != null ? archivos.length : 0));

if (criterioOrdenActual == 2) {
    // 1. Recuperar el string del orden guardado
    String ordenGuardado = sharedPreferences.getString("orden_personalizado", "");
    if (!ordenGuardado.isEmpty()) {
        List<String> ranking = Arrays.asList(ordenGuardado.split(","));

        // 2. Ordenar los archivos según su posición en la lista guardada
        Arrays.sort(archivos, (f1, f2) -> {
            int pos1 = ranking.indexOf(f1.getName().replace(".txt", ""));
            int pos2 = ranking.indexOf(f2.getName().replace(".txt", ""));
            
            // Si un archivo es nuevo y no está en el ranking, va al principio (-1)
            return Integer.compare(pos1, pos2);
        });
    }
} else {
    // Tus otros órdenes (Fecha mod, creación, etc.)
    Arrays.sort(archivos, (f1, f2) -> {
        switch (criterioOrdenActual) {
            case 0: return Long.compare(f2.lastModified(), f1.lastModified());
            case 1: return Long.compare(f1.lastModified(), f2.lastModified());
            default: return 0;
        }
    });
}

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                for (DocumentFile file : archivos) {
                  if (file.isFile() && file.getName() != null && file.getName().endsWith(".txt")) {

                    String jsonContent = NoteIOHelper.readContent(this, file.getUri());
                    try {
                        JSONObject jsonObject = new JSONObject(jsonContent);
                        Nota nota = NoteIOHelper.aNota(jsonObject);
                        if (nota != null) {
                            notasTemp.add(nota);
                        }
                    } catch (Exception e) {
                        Log.e("CARGA_NOTAS", "Error parseando JSON: " + e.getMessage());
                    }
                  }
                }

                // Actualizar UI en el hilo principal
                runOnUiThread(
                    () -> {
                      Log.d("DEBUG", "Notas procesadas para mostrar: " + notasTemp.size());
                      listaDeNotasCompleta.clear();
                      listaDeNotasCompleta.addAll(notasTemp);

                      if (adaptador == null) {

                        adaptador =
                            new NotaAdapter(
                                listaDeNotasCompleta,
                                // Click Normal
                                nota -> {
                                  // Si hay items seleccionados, el click normal funciona para
                                  // seleccionar/deseleccionar (Multiselect)
                                  if (adaptador.haySeleccion()) {
                                    adaptador.toggleSeleccion(nota, 0);
                                  } else {
                                    // Si no hay selección, comportamiento normal (abrir editor)
                                    abrirEditor(nota.getUri());
                                  }
                                },
                                // Click Largo
                                (view, nota, position) -> {
                                  // 1. Marcamos visualmente la nota
                                  adaptador.toggleSeleccion(nota,position);
                                  //mostrarMenuOpciones(view, nota);
                                });
                        recyclerNotas.setAdapter(adaptador);
                        aplicarModoVista();
                      } else {
                        adaptador.actualizarLista(listaDeNotasCompleta);
                      }
                    });
              })
          .start();
    } else {
      Log.e("DEBUG", "Error de acceso: root es nulo o no se puede leer");
    }
  }

  private String obtenerResumenSAF(Uri fileUri) {
    String jsonContent = NoteIOHelper.readContent(this, fileUri);
    if (jsonContent.isEmpty()) return "Nota vacía";

    try {
        JSONObject jsonObject = new JSONObject(jsonContent);
        String contenido = jsonObject.optString("contenido", "");

        String plainText;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            plainText = Html.fromHtml(contenido, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            plainText = Html.fromHtml(contenido).toString();
        }

        if (plainText.length() > 350) {
            return plainText.substring(0, 100) + "...";
        }
        return plainText.isEmpty() ? "Nota vacía" : plainText;
    } catch (Exception e) {
        return "Error al leer la nota";
    }
}


  private void mostrarMenuOpciones(View view, Nota nota) {
    android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
    popup.getMenuInflater().inflate(R.menu.menu_item_nota, popup.getMenu());

    // Truco para mostrar iconos
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
          int id = item.getItemId();
          if (id == R.id.action_share) {
            compartirNotaDesdeLista(nota);
            return true;
          } else if (id == R.id.action_floating) {
            abrirNotaFlotante(nota);
            return true;
          } else if (id == R.id.action_delete) {
            eliminarNotaDesdeLista(nota);
            return true;
          }
          return false;
        });
    popup.setOnDismissListener(
        menu -> {
          // Si solo querías resaltar mientras el menú estaba abierto:
          adaptador.limpiarSeleccion();
        });
    popup.show();
  }

  // 1. Este es el método que llamas desde el clic largo o menú de tu lista
  private void compartirNotaDesdeLista(Nota nota) {
    String[] opciones = {"Enviar como texto", "Enviar como PDF"};

    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle("Compartir: " + nota.getTitulo())
        .setItems(
            opciones,
            (dialog, which) -> {
              if (which == 0) {
                compartirComoTexto(nota);
              } else {
                compartirComoPDF(nota);
              }
            })
        .show();
  }

  // 2. Compartir Texto Plano
  private void compartirComoTexto(Nota nota) {
    String textoCompartir;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        textoCompartir = Html.fromHtml(nota.getContenido(), Html.FROM_HTML_MODE_LEGACY).toString();
    } else {
        textoCompartir = Html.fromHtml(nota.getContenido()).toString();
    }

    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_TEXT, nota.getTitulo() + "\n\n" + textoCompartir);
    startActivity(Intent.createChooser(intent, "Compartir nota vía:"));
}

  // 3. Compartir PDF
  private void compartirComoPDF(Nota nota) {
    String contenidoLimpio;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        contenidoLimpio = Html.fromHtml(nota.getContenido(), Html.FROM_HTML_MODE_LEGACY).toString();
    } else {
        contenidoLimpio = Html.fromHtml(nota.getContenido()).toString();
    }

    if (contenidoLimpio.trim().isEmpty()) {
        Toast.makeText(this, "La nota está vacía", Toast.LENGTH_SHORT).show();
        return;
    }

    // --- Generación del PDF ---
    android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
    // Tamaño A4 (595 x 842 puntos)
    android.graphics.pdf.PdfDocument.PageInfo pageInfo =
        new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
    android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);

    android.graphics.Canvas canvas = page.getCanvas();
    android.graphics.Paint paint = new android.graphics.Paint();

    // Configuración de márgenes y fuente
    int x = 50;
    int y = 60;
    int pageWidth = 500; // Ancho útil para el texto

    // Dibujar Título
    paint.setTextSize(20);
    paint.setTypeface(
        android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
    canvas.drawText(nota.getTitulo(), x, y, paint);

    y += 40; // Espacio después del título

    // Configurar cuerpo de texto
    paint.setTextSize(12);
    paint.setTypeface(android.graphics.Typeface.DEFAULT);

    // --- LÓGICA DE DIBUJO CON WRAPPING (AJUSTE DE LÍNEA) ---
    // TextPaint permite medir mejor el ancho del texto
    android.text.TextPaint textPaint = new android.text.TextPaint(paint);

    for (String parrafo : contenidoLimpio.split("\n")) {
      // StaticLayout es el truco profesional para que el texto salte de línea automáticamente si es
      // largo
      android.text.StaticLayout layout =
          new android.text.StaticLayout(
              parrafo,
              textPaint,
              pageWidth,
              android.text.Layout.Alignment.ALIGN_NORMAL,
              1.0f,
              0.0f,
              false);

      canvas.save();
      canvas.translate(x, y);
      layout.draw(canvas);
      canvas.restore();

      y += layout.getHeight() + 5; // Mover 'y' hacia abajo según el tamaño del párrafo

      if (y > 800) break; // Límite de la página simple
    }

    document.finishPage(page);

    // --- Guardar y Compartir ---
    try {
      File cachePath = new File(getCacheDir(), "pdf_temp");
      if (!cachePath.exists()) cachePath.mkdirs();

      String nombreSeguro = nota.getTitulo().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
      File file = new File(cachePath, nombreSeguro);

      try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
        document.writeTo(fos);
      }
      document.close();

      Uri contentUri =
          androidx.core.content.FileProvider.getUriForFile(
              this, getPackageName() + ".fileprovider", file);

      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("application/pdf");
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.putExtra(Intent.EXTRA_STREAM, contentUri);
      startActivity(Intent.createChooser(intent, "Compartir nota como PDF"));

    } catch (Exception e) {
      Log.e("PDF_ERROR", e.getMessage());
      Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show();
    }
  }

  private void eliminarNotaDesdeLista(Nota nota) {
    try {
      Uri uri = Uri.parse(nota.getUri());
      DocumentFile archivo = DocumentFile.fromSingleUri(this, uri);
      if (archivo != null && archivo.delete()) {
        Toast.makeText(this, "Nota eliminada", Toast.LENGTH_SHORT).show();
        cargarNotas(); // Recargar lista
      } else {
        Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show();
      }
    } catch (Exception e) {
      Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show();
    }
  }

  private void abrirNotaFlotante(Nota nota) {
    String contenido = obtenerTextoDeArchivo(Uri.parse(nota.getUri()));
    Intent serviceIntent = new Intent(this, FloatingService.class);
    serviceIntent.putExtra("contenido_nota", contenido);
    serviceIntent.putExtra("uri_archivo", nota.getUri());

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent);
    } else {
      startService(serviceIntent);
    }
    moveTaskToBack(true);
  }

  // --- MÉTODOS AUXILIARES ---
  private void abrirEditor(String uriString) {
    Intent intent = new Intent(this, EditorActivity.class);

    // CORRECCIÓN: Verificamos que no sea NULL antes de ver si está vacío
    if (uriString != null && !uriString.isEmpty()) {
      intent.putExtra("uri_archivo", uriString);
    }

    activityLauncherEditor.launch(intent);
  }

  private void filtrarLista(String texto) {
    if (listaDeNotasCompleta == null) return;
    List<Nota> listaFiltrada = new ArrayList<>();
    String query = texto.toLowerCase().trim();
    for (Nota nota : listaDeNotasCompleta) {
      if (nota.getTitulo().toLowerCase().contains(query)
          || nota.getContenido().toLowerCase().contains(query)) {
        listaFiltrada.add(nota);
      }
    }
    if (adaptador != null) {
      adaptador.actualizarLista(listaFiltrada);
    }
  }

  private void aplicarModoVista() {
    if (esModoCuadricula) {
      recyclerNotas.setLayoutManager(
          new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
      modoVistaTargeta.setIconResource(R.drawable.outline_view_agenda);
    } else {
      recyclerNotas.setLayoutManager(new LinearLayoutManager(this));
      modoVistaTargeta.setIconResource(R.drawable.grid_view);
    }
    if (adaptador != null) adaptador.notifyDataSetChanged();
  }

  private String obtenerTextoDeArchivo(Uri uri) {
    StringBuilder stringBuilder = new StringBuilder();
    try (InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line).append("\n");
      }
      // Limpieza HTML básica para compartir/flotante
      String raw = stringBuilder.toString();
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        return Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString();
      } else {
        return Html.fromHtml(raw).toString();
      }
    } catch (Exception e) {
      return "";
    }
  }

  private void ordenarPor() {
    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
    View view = getLayoutInflater().inflate(R.layout.button_sheet_orden, null);
    bottomSheetDialog.setContentView(view);

    // 1. Referencias
    MaterialButton btnModificacion = view.findViewById(R.id.fecha_de_modificacion);
    MaterialButton btnCreacion = view.findViewById(R.id.fecha_de_creacion);
    MaterialButton btnNombre = view.findViewById(R.id.personalizado);

    // 2. Lógica de clics
    btnModificacion.setOnClickListener(
        v -> {
          criterioOrdenActual = 0;
          cargarNotas(); // Recarga los archivos con el nuevo orden
          bottomSheetDialog.dismiss();
        });

    btnCreacion.setOnClickListener(
        v -> {
          criterioOrdenActual = 1;
          cargarNotas();
          bottomSheetDialog.dismiss();
        });

    btnNombre.setOnClickListener(
        v -> {
          criterioOrdenActual = 2;
          sharedPreferences.edit().putInt("criterio_orden", 2).apply();
          cargarNotas();
          bottomSheetDialog.dismiss();
        });

    bottomSheetDialog.show();
  }
  private void guardarOrdenPersonalizado() {
    if (adaptador == null) return;
    
    // Obtenemos la lista de notas tal cual quedó en el adaptador
    List<Nota> listaActual = adaptador.getListaNotas();
    StringBuilder sb = new StringBuilder();
    
    for (Nota n : listaActual) {
        // Usamos el nombre del archivo (extraído de la URI o título) como identificador
        sb.append(n.getTitulo()).append(","); 
    }
    
    // Guardamos el string tipo "nota1,nota3,nota2," en SharedPreferences
    sharedPreferences.edit().putString("orden_personalizado", sb.toString()).apply();
    
    // Forzamos que el criterio actual sea el 2 para que no se pierda al recargar
    criterioOrdenActual = 2;
    sharedPreferences.edit().putInt("criterio_orden", 2).apply();
}
}
