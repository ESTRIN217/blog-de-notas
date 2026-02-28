package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

  private RecyclerView recyclerNotas;
  private FloatingActionButton btnNuevaNota;
  private EditText buscar;
  private MaterialButton modoVistaTargeta, btnMenu, orden;
  private DrawerLayout drawerLayout;
  private NavigationView navigationView;

  private List<Nota> listaDeNotasCompleta;
  private NotaAdapter adaptador;

  private static final String KEY_VISTA_GRID = "vista_en_cuadricula";
  private static final String PREFS_NAME = "com.Jhon.myempty.blogdenotasjava.prefs";


  private SharedPreferences sharedPreferences;
  private boolean esModoCuadricula = false;
  // Valores: 0 = Modificación (desc), 1 = Modificación (asc), 2 = Personalizado
  private int criterioOrdenActual = 0;

  private final ActivityResultLauncher<Intent> activityLauncherEditor =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> cargarNotas() // Al volver del editor, recargamos la lista
      );

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    androidx.activity.EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);

    inicializarVistas();
    configurarItemTouchHelper();
    configurarListeners();
    
    // Cargar preferencias
    esModoCuadricula = sharedPreferences.getBoolean(KEY_VISTA_GRID, false);
    criterioOrdenActual = sharedPreferences.getInt("criterio_orden", 0);

    cargarNotas();
  }


  private void inicializarVistas() {
    sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    
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
  
  private void configurarItemTouchHelper() {
    ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {

      @Override
      public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        if (adaptador == null) return false;
        int fromPos = viewHolder.getBindingAdapterPosition();
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
        if (criterioOrdenActual == 2) {
            guardarOrdenPersonalizado();
        }
      }
    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
    itemTouchHelper.attachToRecyclerView(recyclerNotas);
  }


  private void configurarListeners() {
    btnNuevaNota.setOnClickListener(v -> abrirEditor(""));

    modoVistaTargeta.setOnClickListener(v -> {
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
        
    btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    
    navigationView.setNavigationItemSelectedListener(item -> {
      int id = item.getItemId();
      if (id == R.id.settings) {
        startActivity(new Intent(this, SettingsActivity.class));
      } else if (id == R.id.nav_sobre) {
        startActivity(new Intent(this, SobreActivity.class));
      }
      drawerLayout.closeDrawer(GravityCompat.START);
      return true;
    });
    
    orden.setOnClickListener(v -> mostrarDialogoOrden());
  }

  private void cargarNotas() {
    final File notesDir = new File(getFilesDir(), "notas");
    if (!notesDir.exists()) {
        if (!notesDir.mkdirs()) {
            Log.e("CARGA_NOTAS", "No se pudo crear la carpeta de notas.");
            Toast.makeText(this, "No se pudo crear la carpeta de notas.", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    if (!notesDir.isDirectory() || !notesDir.canRead()) {
        Log.e("CARGA_NOTAS", "Error de acceso: la carpeta es nula o no se puede leer.");
        Toast.makeText(this, "No se puede acceder a la carpeta de notas.", Toast.LENGTH_SHORT).show();
        return;
    }
    
    new Thread(() -> {
      File[] archivos = notesDir.listFiles();
      if (archivos == null) {
          Log.e("CARGA_NOTAS", "Fallo al listar los archivos de la carpeta.");
          return;
      }
      
      if (criterioOrdenActual == 2) { 
        String ordenGuardado = sharedPreferences.getString("orden_personalizado", "");
        if (!ordenGuardado.isEmpty()) {
          List<String> ranking = Arrays.asList(ordenGuardado.split(","));
          Map<String, File> fileMap = new HashMap<>();
          for (File file : archivos) {
            if (file.getName() != null) {
              fileMap.put(file.getName().replace(".txt", ""), file);
            }
          }
          ArrayList<File> archivosOrdenados = new ArrayList<>();
          for (String key : ranking) {
            if (fileMap.containsKey(key)) {
              archivosOrdenados.add(fileMap.get(key));
              fileMap.remove(key);
            }
          }
          archivosOrdenados.addAll(0, fileMap.values());
          archivos = archivosOrdenados.toArray(new File[0]);
        }
      } else { 
        Arrays.sort(archivos, (f1, f2) -> {
          switch (criterioOrdenActual) {
            case 0: return Long.compare(f2.lastModified(), f1.lastModified());
            case 1: return Long.compare(f1.lastModified(), f2.lastModified());
            default: return 0;
          }
        });
      }

      final List<Nota> notasTemp = new ArrayList<>();
      for (File file : archivos) {
        if (file.isFile() && file.getName() != null && file.getName().endsWith(".txt")) {
          String jsonContent = readContentFromFile(file);
          if (jsonContent.isEmpty()) continue;
          
          try {
            JSONObject jsonObject = new JSONObject(jsonContent);
            Nota nota = NoteIOHelper.aNota(jsonObject);
            if (nota != null) {
              nota.setUri(Uri.fromFile(file).toString()); 
              notasTemp.add(nota);
            }
          } catch (Exception e) {
            Log.e("CARGA_NOTAS", "Error parseando JSON: " + file.getName(), e);
          }
        }
      }

      runOnUiThread(() -> {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        
        listaDeNotasCompleta.clear();
        listaDeNotasCompleta.addAll(notasTemp);

        if (adaptador == null) {
          adaptador = new NotaAdapter(listaDeNotasCompleta,
              nota -> {
                if (adaptador.haySeleccion()) {
                  adaptador.toggleSeleccion(nota, 0);
                } else {
                  abrirEditor(nota.getUri());
                }
              },
              (view, nota, position) -> {
                adaptador.toggleSeleccion(nota, position);
              });
          recyclerNotas.setAdapter(adaptador);
          aplicarModoVista();
        } else {
          adaptador.actualizarLista(listaDeNotasCompleta);
        }
      });
    }).start();
  }
  
  private String readContentFromFile(File file) {
      StringBuilder stringBuilder = new StringBuilder();
      try (FileInputStream fis = new FileInputStream(file);
           BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
          String line;
          while ((line = reader.readLine()) != null) {
              stringBuilder.append(line);
              stringBuilder.append('\n');
          }
      } catch (IOException e) {
          Log.e("ReadFile", "Error leyendo archivo: " + file.getName(), e);
          return "";
      }
      return stringBuilder.toString();
  }

  private void abrirEditor(String uriString) {
    Intent intent = new Intent(this, EditorActivity.class);
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
        String titulo = nota.getTitulo() != null ? nota.getTitulo().toLowerCase() : "";
        String contenido = nota.getContenido() != null ? nota.getContenido().toLowerCase() : "";
        if (titulo.contains(query) || contenido.contains(query)) {
            listaFiltrada.add(nota);
        }
    }
    
    if (adaptador != null) {
      adaptador.actualizarLista(listaFiltrada);
    }
  }

  private void aplicarModoVista() {
    if (esModoCuadricula) {
      recyclerNotas.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
      modoVistaTargeta.setIconResource(R.drawable.outline_view_agenda);
    } else {
      recyclerNotas.setLayoutManager(new LinearLayoutManager(this));
      modoVistaTargeta.setIconResource(R.drawable.grid_view);
    }
  }
  
  private void mostrarDialogoOrden() {
    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
    View view = getLayoutInflater().inflate(R.layout.button_sheet_orden, null);
    bottomSheetDialog.setContentView(view);

    MaterialButton btnModificacion = view.findViewById(R.id.fecha_de_modificacion);
    MaterialButton btnCreacion = view.findViewById(R.id.fecha_de_creacion);
    MaterialButton btnPersonalizado = view.findViewById(R.id.personalizado);

    btnModificacion.setOnClickListener(v -> {
        actualizarCriterioOrden(0);
        bottomSheetDialog.dismiss();
    });

    btnCreacion.setOnClickListener(v -> {
        actualizarCriterioOrden(1);
        bottomSheetDialog.dismiss();
    });

    btnPersonalizado.setOnClickListener(v -> {
        actualizarCriterioOrden(2);
        bottomSheetDialog.dismiss();
    });

    bottomSheetDialog.show();
  }
  
  private void actualizarCriterioOrden(int nuevoCriterio) {
      if (criterioOrdenActual == nuevoCriterio) return;
      criterioOrdenActual = nuevoCriterio;
      sharedPreferences.edit().putInt("criterio_orden", criterioOrdenActual).apply();
      cargarNotas();
  }

  private void guardarOrdenPersonalizado() {
    if (adaptador == null) return;

    List<Nota> listaActual = adaptador.getListaNotas();
    StringBuilder sb = new StringBuilder();
    
    for (Nota n : listaActual) {
      String uriString = n.getUri();
      if (uriString == null || uriString.isEmpty()) continue;
      
      try {
        File file = new File(URI.create(uriString));
        String fileName = file.getName().replace(".txt", "");
        sb.append(fileName).append(",");
      } catch (Exception e) {
        Log.e("SaveOrder", "No se pudo obtener el nombre del archivo de la URI: " + uriString, e);
      }
    }

    if (sb.length() > 0) {
      sharedPreferences.edit().putString("orden_personalizado", sb.toString()).apply();
    }
  }
}
