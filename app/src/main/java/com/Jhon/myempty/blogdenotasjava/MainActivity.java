package com.Jhon.myempty.blogdenotasjava;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.view.View;
import android.text.Html;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat; // IMPORTANTE: Esta librería controla los bordes
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerNotas;
    private FloatingActionButton btnNuevaNota;
    private EditText buscar;
    private Button btnSettings, modoVistaTargeta;

    private List<Nota> listaDeNotasCompleta;
    private NotaAdapter adaptador;

    private static final String PREFS_NAME = "MisPreferencias";
    private static final String KEY_THEME = "tema_elegido";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 123;
    
    private String carpetaUriString;
    private boolean esModoCuadricula = false; // Variable para controlar el estado
    private static final String KEY_VISTA_GRID = "vista_en_cuadricula"; // Para guardar la preferencia

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Configuración de Temas (Antes de super.onCreate e inflar vistas)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Aplicar Material You si está activado
        if (prefs.getBoolean("material_theme_activado", false)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        
        // Aplicar Modo Oscuro/Claro
        int temaGuardado = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(temaGuardado);

        super.onCreate(savedInstanceState);

        // 2. ACTIVAR MODO BORDE A BORDE (EDGE-TO-EDGE)
        // Esto le dice a la ventana: "No pintes barras negras, deja que mi XML controle el fondo"
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // 3. Inicialización normal
        inicializarVistas();
        configurarListeners();
        
        // Cargar URI guardada
        carpetaUriString = prefs.getString("carpeta_uri", null);
        
        // 4. Permisos
        verificarPermisos();
    }

    private void inicializarVistas() {
        recyclerNotas = findViewById(R.id.recyclerNotas);
        recyclerNotas.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        btnNuevaNota = findViewById(R.id.btnNuevaNota);
        buscar = findViewById(R.id.buscar);
        btnSettings = findViewById(R.id.btnSettings);
        modoVistaTargeta = findViewById(R.id.modoVistaTargeta);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    esModoCuadricula = prefs.getBoolean(KEY_VISTA_GRID, false); // Por defecto falso (Lista)

    // APLICAR LA VISTA INICIAL (Sin animación la primera vez)
    aplicarModoVista();
    }

    private void configurarListeners() {
        btnNuevaNota.setOnClickListener(v -> abrirEditor(""));

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
        
        // --- NUEVO: LISTENER PARA CAMBIAR VISTA ---
    modoVistaTargeta.setOnClickListener(v -> {
        // 1. Invertir el valor (si era true pasa a false, y viceversa)
        esModoCuadricula = !esModoCuadricula;
        
        // 2. Guardar en preferencias para recordarlo
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VISTA_GRID, esModoCuadricula)
            .apply();

        // 3. Aplicar cambios visuales
        aplicarModoVista();
    });


        configurarBuscador();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (carpetaUriString != null) {
            cargarLista();
        } else {
            solicitarAccesoCarpeta();
        }
    }

    private void verificarPermisos() {
        // NOTA: Con SAF (Storage Access Framework) que usas abajo, 
        // este permiso es técnico redundante en Android 10+, pero no hace daño dejarlo.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST_CODE);
        }
    }

    private void solicitarAccesoCarpeta() {
        Toast.makeText(this, "Selecciona la carpeta donde guardas tus notas", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();

                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);

                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString("carpeta_uri", uri.toString());
                editor.apply();

                carpetaUriString = uri.toString();
                cargarLista();
            }
        }
    }

    private void cargarLista() {
        if (carpetaUriString == null) return;

        try {
            Uri treeUri = Uri.parse(carpetaUriString);
            DocumentFile root = DocumentFile.fromTreeUri(this, treeUri);
            
            listaDeNotasCompleta = new ArrayList<>();
            if (root != null) {
                DocumentFile[] files = root.listFiles();
                for (DocumentFile file : files) {
                    if (file.getName() != null && file.getName().endsWith(".txt")) {
                        String titulo = file.getName().replace(".txt", "");
                        String extracto = obtenerResumenSAF(file.getUri());
                        String fecha = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(new Date(file.lastModified()));

                        listaDeNotasCompleta.add(new Nota(titulo, extracto, fecha, file.getUri().toString()));
                    }
                }
            }

            adaptador = new NotaAdapter(listaDeNotasCompleta, 
                nota -> {
                    // Click normal: Abrir editor
                    abrirEditor(nota.getNombreArchivo());
                },
                (view, nota) -> {
                    // Click largo: Mostrar menú
                    mostrarMenuOpciones(view, nota);
                }
            );
            recyclerNotas.setAdapter(adaptador);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar archivos SAF", Toast.LENGTH_SHORT).show();
        }
    }

    private String obtenerResumenSAF(Uri fileUri) {
    StringBuilder sb = new StringBuilder();
    try (InputStream is = getContentResolver().openInputStream(fileUri);
         BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
        
        String linea;
        int lineasContadas = 0;

        // Leer hasta 6 líneas o hasta que se acabe el archivo
        while ((linea = br.readLine()) != null && lineasContadas < 10) {
            sb.append(linea).append("\n");
            lineasContadas++;
        }

        String contenidoBruto = sb.toString().trim();
        if (contenidoBruto.isEmpty()) return "Nota vacía";

        // VITAL: Convertir de HTML a texto plano para que el resumen no tenga etiquetas
        String textoLimpio;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textoLimpio = Html.fromHtml(contenidoBruto, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            textoLimpio = Html.fromHtml(contenidoBruto).toString();
        }

        return textoLimpio.trim();

    } catch (Exception e) {
        return "";
    }
    }

    private void configurarBuscador() {
        buscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarLista(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filtrarLista(String texto) {
        if (listaDeNotasCompleta == null) return;

        List<Nota> listaFiltrada = new ArrayList<>();
        String query = texto.toLowerCase().trim();
        for (Nota nota : listaDeNotasCompleta) {
            if (nota.getTitulo().toLowerCase().contains(query)) {
                listaFiltrada.add(nota);
            }
        }
        if (adaptador != null) {
        adaptador.actualizarLista(listaFiltrada);
    }
        recyclerNotas.setAdapter(adaptador);
    }

    private void abrirEditor(String uriString) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("nombre_archivo", uriString);
        startActivity(intent);
    }
    private void mostrarMenuOpciones(View view, Nota nota) {
    android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
    popup.getMenuInflater().inflate(R.menu.menu_item_nota, popup.getMenu());
    
    // Forzar que se vean los iconos (Opcional, truco para Android moderno)
    try {
        java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
        field.setAccessible(true);
        Object menuPopupHelper = field.get(popup);
        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
        java.lang.reflect.Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
        setForceShowIcon.invoke(menuPopupHelper, true);
    } catch (Exception e) { e.printStackTrace(); }

    popup.setOnMenuItemClickListener(item -> {
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
    
    popup.show();
    }
    private void compartirNotaDesdeLista(Nota nota) {
    String contenido = obtenerTextoDeArchivo(Uri.parse(nota.getNombreArchivo()));
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_TEXT, nota.getTitulo() + "\n\n" + contenido);
    sendIntent.setType("text/plain");
    startActivity(Intent.createChooser(sendIntent, "Compartir nota vía:"));
    }
    private void eliminarNotaDesdeLista(Nota nota) {
    try {
        Uri uri = Uri.parse(nota.getNombreArchivo());
        DocumentFile archivo = DocumentFile.fromSingleUri(this, uri);
        
        if (archivo != null && archivo.delete()) {
            Toast.makeText(this, "Nota eliminada", Toast.LENGTH_SHORT).show();
            // Recargar la lista para que desaparezca visualmente
            cargarLista(); 
        } else {
            Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_SHORT).show();
        }
    } catch (Exception e) {
        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show();
    }
    }
    private void abrirNotaFlotante(Nota nota) {
    String contenido = obtenerTextoDeArchivo(Uri.parse(nota.getNombreArchivo()));
    
    Intent serviceIntent = new Intent(this, FloatingService.class);
    serviceIntent.putExtra("contenido_nota", contenido);
    serviceIntent.putExtra("uri_archivo", nota.getNombreArchivo());
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent);
    } else {
        startService(serviceIntent);
    }
    // Minimizamos la app para ver la ventana flotante
    moveTaskToBack(true);
    }

// Auxiliar para leer texto (ya tenías algo similar en obtenerResumenSAF, pero este lee todo)
    private String obtenerTextoDeArchivo(Uri uri) {
    StringBuilder stringBuilder = new StringBuilder();
    try (InputStream inputStream = getContentResolver().openInputStream(uri);
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
    } catch (Exception e) {
        return "";
    }
    return stringBuilder.toString();
    }
    private void aplicarModoVista() {
    if (esModoCuadricula) {
        // MODO CUADRÍCULA (2 columnas)
        // Usamos StaggeredGridLayoutManager para que las notas se ajusten como en Google Keep
        recyclerNotas.setLayoutManager(new androidx.recyclerview.widget.StaggeredGridLayoutManager(2, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL));
        
        // CAMBIAR ICONO: Si estoy en cuadrícula, muestro el icono de "Lista" para volver
        modoVistaTargeta.setCompoundDrawablesWithIntrinsicBounds(R.drawable.outline_view_agenda, 0, 0, 0); 
        
    } else {
        // MODO LISTA (1 columna)
        recyclerNotas.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        // CAMBIAR ICONO: Si estoy en lista, muestro el icono de "Cuadrícula" para cambiar
        // Asegúrate de tener este icono creado, por ejemplo: ic_grid_view
        modoVistaTargeta.setCompoundDrawablesWithIntrinsicBounds(R.drawable.grid_view, 0, 0, 0); 
    }
    
    // Importante: Si ya hay datos, recargamos el adaptador para que se reacomoden
    if (adaptador != null) {
        recyclerNotas.setAdapter(adaptador);
    }
    }
}