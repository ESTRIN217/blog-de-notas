package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity {

    private EditText txtNota, txtTitulo;
    private TextView lblFecha;
    private ImageView btnAtras, btnDeshacer, btnRehacer, btnGuardar, menu;
    private View background;

    private ArrayList<String> historial = new ArrayList<>();
    private int posicionHistorial = -1;
    private boolean esCambioProgramatico = false;
    
    // Variables para manejar archivos con SAF
    private Uri uriArchivoActual; 
    private String carpetaUriPadre;

    // --- Permiso para ventana flotante ---
    private final ActivityResultLauncher<Intent> launcherPermisoOverlay = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    iniciarServicioFlotante();
                } else {
                    Toast.makeText(this, "Permiso denegado. No se puede iniciar el modo flotante.", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);

        // A. Aplicar Tema
        int temaGuardado = prefs.getInt("tema_elegido", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(temaGuardado);

        // B. Aplicar Colores Dinámicos
        if (prefs.getBoolean("material_theme_activado", false)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor); 

        manejarIntent(); 
        inicializarVistas();
        
        establecerFecha();
        configurarBotones();

        // Cargar nota si la URI existe, sino es una nota nueva
        if (uriArchivoActual != null) {
            cargarNotaSAF();
        } else {
            txtTitulo.setText("Nueva Nota");
        }

        // Inicializar historial para Deshacer/Rehacer
        if (historial.isEmpty()) {
            historial.add(txtNota.getText().toString());
            posicionHistorial = 0;
        }
        aplicarPreferenciasVisuales();
    }

    private void manejarIntent() {
        String dataRecibida = getIntent().getStringExtra("nombre_archivo");
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        carpetaUriPadre = prefs.getString("carpeta_uri", null);

        if (dataRecibida != null && !dataRecibida.isEmpty()) {
            uriArchivoActual = Uri.parse(dataRecibida);
        }
    }

    private void inicializarVistas() {
        txtNota = findViewById(R.id.txtNota);
        txtTitulo = findViewById(R.id.txtTitulo);
        lblFecha = findViewById(R.id.fecha);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnDeshacer = findViewById(R.id.btnDeshacer);
        btnRehacer = findViewById(R.id.btnRehacer);
        btnAtras = findViewById(R.id.btnAtras);
        menu = findViewById(R.id.menu);
        background = findViewById(R.id.background);
    }

    private void establecerFecha() {
        String fechaHoy = new SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.getDefault()).format(new Date());
        lblFecha.setText(fechaHoy);
    }

    private void configurarBotones() {
        btnGuardar.setOnClickListener(v -> guardarNota());
        
        btnDeshacer.setOnClickListener(v -> deshacer());
        
        btnRehacer.setOnClickListener(v -> rehacer());
        
        btnAtras.setOnClickListener(v -> {
            guardarNota(); 
            finish();
        });
        
        menu.setOnClickListener(this::mostrarMenu);

        txtNota.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                if (!esCambioProgramatico) registrarCambio(s.toString());
            }
        });
        
        txtTitulo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                renombrarNota();
            }
        });
    }

    private void cargarNotaSAF() {
        try (InputStream is = getContentResolver().openInputStream(uriArchivoActual);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea).append("\n");
            }
            
            esCambioProgramatico = true;
            txtNota.setText(sb.toString().trim());
            esCambioProgramatico = false;
            
            DocumentFile docFile = DocumentFile.fromSingleUri(this, uriArchivoActual);
            if (docFile != null && docFile.getName() != null) {
                txtTitulo.setText(docFile.getName().replace(".txt", ""));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al abrir la nota", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarNota() {
        String contenido = txtNota.getText().toString();
        String titulo = txtTitulo.getText().toString().trim();

        if (contenido.isEmpty() && titulo.isEmpty()) return;
        if (titulo.isEmpty()) titulo = "Sin_titulo";

        try {
            // Si es una nota nueva, creamos el archivo en la carpeta autorizada
            if (uriArchivoActual == null) {
                if (carpetaUriPadre == null) {
                    Toast.makeText(this, "Error: No hay carpeta seleccionada", Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri rootUri = Uri.parse(carpetaUriPadre);
                DocumentFile rootDoc = DocumentFile.fromTreeUri(this, rootUri);
                DocumentFile nuevoArchivo = rootDoc.createFile("text/plain", titulo + ".txt");
                if (nuevoArchivo != null) {
                    uriArchivoActual = nuevoArchivo.getUri();
                }
            }

            // Escribir contenido (Modo "wt" para truncar y escribir)
            try (OutputStream os = getContentResolver().openOutputStream(uriArchivoActual, "wt")) {
                os.write(contenido.getBytes());
                Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_LONG).show();
        }
    }

    private void renombrarNota() {
        String nuevoTitulo = txtTitulo.getText().toString().trim();
        if (uriArchivoActual != null && !nuevoTitulo.isEmpty()) {
            try {
                DocumentFile file = DocumentFile.fromSingleUri(this, uriArchivoActual);
                if (file != null) {
                    file.renameTo(nuevoTitulo + ".txt");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void eliminarNotaFisicamente() {
        if (uriArchivoActual != null) {
            try {
                DocumentFile file = DocumentFile.fromSingleUri(this, uriArchivoActual);
                if (file != null && file.delete()) {
                    Toast.makeText(this, "Nota eliminada", Toast.LENGTH_SHORT).show();
                    uriArchivoActual = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void mostrarMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.editormenu, popup.getMenu());
        try {
        java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
        field.setAccessible(true);
        Object menuPopupHelper = field.get(popup);
        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
        java.lang.reflect.Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
        setForceShowIcon.invoke(menuPopupHelper, true);
        } catch (Exception e) { e.printStackTrace(); }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.opcion_compartir) {
                compartirNota();
                return true;
            } else if (itemId == R.id.opcion_borrar) {
                eliminarNotaFisicamente(); 
                finish(); 
                return true;
            } else if (itemId == R.id.limpiar) {
                txtNota.setText("");
                return true;
            } else if (itemId == R.id.pip) { 
                iniciarModoFlotante();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void compartirNota() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, txtNota.getText().toString());
        startActivity(Intent.createChooser(intent, "Compartir nota vía"));
    }

    // --- MODO FLOTANTE ---
    private void iniciarModoFlotante() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            iniciarServicioFlotante();
        } else {
            solicitarPermisoOverlay();
        }
    }

    private void solicitarPermisoOverlay() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        Toast.makeText(this, "Permite 'Superponer a otras apps'", Toast.LENGTH_LONG).show();
        launcherPermisoOverlay.launch(intent);
    }

    private void iniciarServicioFlotante() {
        Intent serviceIntent = new Intent(this, FloatingService.class);
        serviceIntent.putExtra("contenido_nota", txtNota.getText().toString());
        serviceIntent.putExtra("uri_archivo", uriArchivoActual != null ? uriArchivoActual.toString() : "");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        finish();
    }

    // --- LÓGICA DESHACER/REHACER ---
    private void registrarCambio(String texto) {
        while (posicionHistorial < historial.size() - 1) {
            historial.remove(historial.size() - 1);
        }
        historial.add(texto);
        posicionHistorial++;
    }

    private void deshacer() {
        if (posicionHistorial > 0) {
            posicionHistorial--;
            actualizarTexto(historial.get(posicionHistorial));
        }
    }

    private void rehacer() {
        if (posicionHistorial < historial.size() - 1) {
            posicionHistorial++;
            actualizarTexto(historial.get(posicionHistorial));
        }
    }

    private void actualizarTexto(String texto) {
        esCambioProgramatico = true;
        txtNota.setText(texto);
        try {
            txtNota.setSelection(txtNota.getText().length());
        } catch (Exception ignored) {}
        esCambioProgramatico = false;
    }
    private void aplicarPreferenciasVisuales() {
    SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
    
    // 1. Aplicar Tamaño de Fuente al cuerpo de la nota
    float fontSize = prefs.getFloat("editor_font_size", 16f);
    if (txtNota != null) {
        txtNota.setTextSize(fontSize);
    }

    // 2. Configurar colores según el modo
    int bgMode = prefs.getInt("editor_bg_mode", 0);
    int colorFondo;
    int colorTexto;

    switch (bgMode) {
        case 1: // Modo Papel (Crema)
            colorFondo = 0xFFFFF8E1; 
            colorTexto = 0xFF3E2723; // Marrón oscuro para mejor lectura
            break;
        case 2: // Modo Negro Puro (OLED)
            colorFondo = 0xFF000000;
            colorTexto = 0xFFFFFFFF; // Blanco puro
            break;
        default: 
            // Si es modo sistema, no forzamos colores y salimos
            return; 
    }

    // 3. Aplicar a los componentes (Validando que no sean nulos)
    if (background != null) {
        background.setBackgroundColor(colorFondo);
    }
    
    if (txtNota != null) {
        txtNota.setTextColor(colorTexto);
        // Quitamos el fondo propio del EditText para que sea transparente
        txtNota.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }

    // NUEVO: Ajustar también Título y Fecha para que no se pierdan con el fondo
    if (txtTitulo != null) {
        txtTitulo.setTextColor(colorTexto);
        txtTitulo.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }
    if (lblFecha != null) {
        lblFecha.setTextColor(colorTexto);
    }
    }
}