package com.Jhon.myempty.blogdenotasjava;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String KEY_VISTA_GRID = "vista_en_cuadricula";

    private SharedPreferences sharedPreferences;
    private boolean esModoCuadricula = false;
    private String carpetaUriString;

    // 1. LAUNCHERS MODERNOS (Reemplazan a onActivityResult)
    private final ActivityResultLauncher<Intent> launcherSelectorCarpeta = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        final int takeFlags = result.getData().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);

                        sharedPreferences.edit().putString("carpeta_uri", uri.toString()).apply();
                        carpetaUriString = uri.toString();
                        cargarNotas(); // Recargar lista
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> activityLauncherEditor = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> cargarNotas() // Al volver del editor, recargamos la lista
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Configuración previa a la UI
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        if (sharedPreferences.getBoolean("material_theme_activado", false)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        int temaGuardado = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(temaGuardado);

        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        inicializarVistas();
        configurarListeners();

        // Recuperar URI y Modo Vista
        carpetaUriString = sharedPreferences.getString("carpeta_uri", null);
        esModoCuadricula = sharedPreferences.getBoolean(KEY_VISTA_GRID, false);
        
        aplicarModoVista(); // Aplicar diseño inicial
        checkStoragePermissions();
    }

    private void inicializarVistas() {
        recyclerNotas = findViewById(R.id.recyclerNotas);
        btnNuevaNota = findViewById(R.id.btnNuevaNota);
        buscar = findViewById(R.id.buscar);
        btnSettings = findViewById(R.id.btnSettings);
        modoVistaTargeta = findViewById(R.id.modoVistaTargeta);
        listaDeNotasCompleta = new ArrayList<>();
    }

    private void configurarListeners() {
        btnNuevaNota.setOnClickListener(v -> abrirEditor(""));

        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        modoVistaTargeta.setOnClickListener(v -> {
            esModoCuadricula = !esModoCuadricula;
            sharedPreferences.edit().putBoolean(KEY_VISTA_GRID, esModoCuadricula).apply();
            aplicarModoVista();
        });

        buscar.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarLista(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void checkStoragePermissions() {
        if (carpetaUriString == null) {
            Toast.makeText(this, "Selecciona una carpeta para tus notas", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            launcherSelectorCarpeta.launch(intent);
        } else {
            cargarNotas();
        }
    }

    // --- MÉTODO PRINCIPAL DE CARGA (OPTIMIZADO CON HILO Y ORDENAMIENTO) ---
    private void cargarNotas() {
        if (carpetaUriString == null) return;

        Uri treeUri = Uri.parse(carpetaUriString);
        DocumentFile root = DocumentFile.fromTreeUri(this, treeUri);

        if (root != null && root.canRead()) {
            new Thread(() -> {
                List<Nota> notasTemp = new ArrayList<>();
                DocumentFile[] archivos = root.listFiles();

                // 1. Ordenar por fecha (Más reciente primero)
                Arrays.sort(archivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                for (DocumentFile file : archivos) {
                    if (file.isFile() && file.getName() != null && file.getName().endsWith(".txt")) {
                        String titulo = file.getName().replace(".txt", "");
                        // Usamos tu método optimizado que lee solo 10 líneas
                        String extracto = obtenerResumenSAF(file.getUri());
                        String fecha = sdf.format(new Date(file.lastModified()));

                        notasTemp.add(new Nota(titulo, extracto, fecha, file.getUri().toString()));
                    }
                }

                // 2. Actualizar UI en el hilo principal
                runOnUiThread(() -> {
                    listaDeNotasCompleta.clear();
                    listaDeNotasCompleta.addAll(notasTemp);

                    if (adaptador == null) {
                        adaptador = new NotaAdapter(listaDeNotasCompleta,
                                // Click Normal
                                nota -> abrirEditor(nota.getUri()),
                                // Click Largo (Menú Opciones)
                                (view, nota) -> mostrarMenuOpciones(view, nota)
                        );
                        recyclerNotas.setAdapter(adaptador);
                        aplicarModoVista(); 
                    } else {
                        adaptador.actualizarLista(listaDeNotasCompleta);
                    }
                });
            }).start();
        }
    }

    private String obtenerResumenSAF(Uri fileUri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(fileUri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            
            String linea;
            int lineasContadas = 0;
            while ((linea = br.readLine()) != null && lineasContadas < 10) {
                sb.append(linea).append(" "); // Agregamos espacio en lugar de salto de línea para resumen
                lineasContadas++;
            }

            String contenidoBruto = sb.toString().trim();
            if (contenidoBruto.isEmpty()) return "Nota vacía";

            // Limpieza HTML
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                return Html.fromHtml(contenidoBruto, Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                return Html.fromHtml(contenidoBruto).toString();
            }
        } catch (Exception e) {
            return "";
        }
    }

    // --- ACCIONES DEL MENÚ CONTEXTUAL (RESTAURADOS) ---
    private void mostrarMenuOpciones(View view, Nota nota) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_item_nota, popup.getMenu());

        // Truco para mostrar iconos
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
        String contenido = obtenerTextoDeArchivo(Uri.parse(nota.getUri()));
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, nota.getTitulo() + "\n\n" + contenido);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Compartir nota vía:"));
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
            if (nota.getTitulo().toLowerCase().contains(query) || 
                nota.getContenido().toLowerCase().contains(query)) {
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
            modoVistaTargeta.setCompoundDrawablesWithIntrinsicBounds(R.drawable.outline_view_agenda, 0, 0, 0);
        } else {
            recyclerNotas.setLayoutManager(new LinearLayoutManager(this));
            modoVistaTargeta.setCompoundDrawablesWithIntrinsicBounds(R.drawable.grid_view, 0, 0, 0);
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
}