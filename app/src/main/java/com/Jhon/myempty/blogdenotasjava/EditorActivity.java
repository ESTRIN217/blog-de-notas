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
import android.widget.LinearLayout; // Ensure this is imported for contenedorAdjuntos

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;
import android.media.MediaRecorder;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.Chronometer;
import android.os.SystemClock;
import android.media.MediaPlayer;
import android.widget.ProgressBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.LayoutInflater; // Added for LayoutInflater
import android.widget.ImageButton; // Added for ImageButton

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.io.IOException;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EditorActivity extends AppCompatActivity {

    private EditText txtNota, txtTitulo;
    private TextView lblFecha;
    private ImageView btnAtras, btnDeshacer, btnRehacer, btnGuardar, menu, añadir, paleta, textoFormato;
    private View background;
    private LinearLayout contenedorAdjuntos; // Changed to LinearLayout

    private ArrayList<String> historial = new ArrayList<>();
    private int posicionHistorial = -1;
    private boolean esCambioProgramatico = false;
    
    // Variables para manejar archivos con SAF
    private Uri uriArchivoActual; 
    private String carpetaUriPadre;
    private android.os.Handler handlerHistorial = new android.os.Handler();
    private Runnable runnableHistorial;
    private Runnable runnableAutoguardado;
    // 1. Declarar los lanzadores al inicio de la clase
    private ActivityResultLauncher<String> seleccionarImagenLauncher;
    private ActivityResultLauncher<Uri> tomarFotoLauncher;
    private Uri uriFotoCamara; // Para guardar temporalmente la foto de la cámara
    private MediaRecorder mediaRecorder;
    private String rutaArchivoAudio = null;
    private boolean estaGrabando = false;
    private static final int REQUEST_AUDIO_PERMISSION_CODE = 200;
    private MediaPlayer mediaPlayer;
    private android.os.Handler handlerAudio = new android.os.Handler();
    private ArrayList<String> listaRutasAudios = new ArrayList<>();
    private Uri uriFotoActual;
    private ArrayList<String> listaRutasFotos = new ArrayList<>(); // Nueva lista para persistencia de fotos
    private ArrayList<String> listaRutasDibujos = new ArrayList<>(); // NEW: List for drawings


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
    private final ActivityResultLauncher<Intent> dibujoLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uriDibujo = result.getData().getData();
            if (uriDibujo != null) {
                insertarDibujoEnNota(uriDibujo); // Use the new method
            }
        }
    }
    );
    // Chequear permisos
    private boolean chequearPermisosAudio() {
    int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
    return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsAudio() {
    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    // Mostrar el diálogo visual
    private void mostrarGrabadoraVisual() {
    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
    View view = getLayoutInflater().inflate(R.layout.bottom_sheet_grabadora, null);
    bottomSheetDialog.setContentView(view);

    // 1. Vincular Vistas
    TextView txtEstado = view.findViewById(R.id.txtEstadoGrabacion);
    Chronometer cronometro = view.findViewById(R.id.cronometro);
    FloatingActionButton fabGrabar = view.findViewById(R.id.fabGrabar);
    ImageView btnCancelar = view.findViewById(R.id.btnCancelarAudio);
    ImageView btnGuardar = view.findViewById(R.id.btnGuardarAudio);

    // 2. Lógica del Botón Principal (Grabar/Parar)
    fabGrabar.setOnClickListener(v -> {
        if (!estaGrabando) {
            // --- EMPEZAR GRABACIÓN ---
            iniciarGrabacion(); // Llama a tu método lógico
            
            // Cambios Visuales
            cronometro.setBase(SystemClock.elapsedRealtime());
            cronometro.start();
            fabGrabar.setImageResource(R.drawable.outline_stop_circle); // Cambia icono a Stop
            fabGrabar.setBackgroundTintList(getColorStateList(R.color.md_theme_error)); // Rojo (opcional)
            txtEstado.setText("Grabando...");
            
            // Ocultar botones secundarios mientras graba
            btnGuardar.setVisibility(View.INVISIBLE);
            btnCancelar.setVisibility(View.INVISIBLE);
            
        } else {
            // --- PARAR GRABACIÓN ---
            detenerGrabacion(); // Llama a tu método lógico
            
            // Cambios Visuales
            cronometro.stop();
            fabGrabar.setImageResource(R.drawable.round_mic); // Vuelve a Mic
            fabGrabar.setBackgroundTintList(getColorStateList(R.color.md_theme_primary)); // Color original
            txtEstado.setText("Audio capturado");
            
            // Mostrar opciones Finales
            btnGuardar.setVisibility(View.VISIBLE);
            btnCancelar.setVisibility(View.VISIBLE);
            fabGrabar.setVisibility(View.INVISIBLE); // Ocultamos el mic para obligar a guardar o cancelar
        }
    });

    // 3. Botón Cancelar (Borrar lo grabado y salir)
    btnCancelar.setOnClickListener(v -> {
        // Aquí podrías agregar lógica para borrar el archivo físico si el usuario se arrepiente
        if (rutaArchivoAudio != null && new File(rutaArchivoAudio).exists()) {
            new File(rutaArchivoAudio).delete();
        }
        rutaArchivoAudio = null; 
        bottomSheetDialog.dismiss();
        Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show();
    });

    // 4. Botón Guardar (Confirmar)
    btnGuardar.setOnClickListener(v -> {
    bottomSheetDialog.dismiss();
    Toast.makeText(this, "Audio adjuntado", Toast.LENGTH_SHORT).show();
    insertarAudioEnNota(rutaArchivoAudio); // Use the new method
    });
    
    // Evitar que se cierre tocando afuera mientras graba
    bottomSheetDialog.setCancelable(false); 
    bottomSheetDialog.show();
    }

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

        inicializarVistas(); // Initialize views before handling intent
        manejarIntent(); 
        
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
        // 2. Inicializar en onCreate
        seleccionarImagenLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    insertarFotoEnNota(uri); // Use the new method
                    Toast.makeText(this, "Imagen seleccionada: " + uri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                    }
            }
        );

        tomarFotoLauncher = registerForActivityResult(
        new ActivityResultContracts.TakePicture(),
        success -> {
        if (success && uriFotoActual != null) {
            insertarFotoEnNota(uriFotoActual); // Use the new method
            Toast.makeText(this, "Foto añadida", Toast.LENGTH_SHORT).show();
        }
    }
    );
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
        añadir = findViewById(R.id.añadir);
        paleta = findViewById(R.id.paleta);
        textoFormato = findViewById(R.id.formato_text);
        contenedorAdjuntos = findViewById(R.id.contenedorAdjuntos);
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!esCambioProgramatico) {
            // Cancelar el registro anterior si el usuario sigue escribiendo
            handlerHistorial.removeCallbacks(runnableHistorial);
            
            // Programar el nuevo registro en 500ms
            runnableHistorial = () -> registrarCambio(s.toString());
            handlerHistorial.postDelayed(runnableHistorial, 500);
            // NUEVO: Programar AUTOGUARDADO en 2 segundos
            handlerHistorial.removeCallbacks(runnableAutoguardado);
            runnableAutoguardado = () -> guardarNotaSilenciosamente();
            handlerHistorial.postDelayed(runnableAutoguardado, 2000);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        txtTitulo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                renombrarNota();
            }
        });
        

// En configurarListeners añade esto:
    añadir.setOnClickListener(v -> {
    BottomSheetDialog bottomSheetInsertar = new BottomSheetDialog(this);
    View layout = getLayoutInflater().inflate(R.layout.bottom_sheet_insertar, null);
    bottomSheetInsertar.setContentView(layout);

    com.google.android.material.navigation.NavigationView nav = layout.findViewById(R.id.navigationInsertar);
    
    nav.setNavigationItemSelectedListener(item -> {
        int id = item.getItemId();
        bottomSheetInsertar.dismiss(); // Cerramos el panel al elegir una opción

        if (id == R.id.ins_foto) {
            seleccionarImagenLauncher.launch("image/*");
        } else if (id == R.id.ins_camara) {
            abrirCamara(); // This method already calls tomarFotoLauncher
        } else if (id == R.id.ins_audio) {
            if (chequearPermisosAudio()) {
                mostrarGrabadoraVisual();
            } else {
                requestPermissionsAudio();
            }
        // ... dentro del listener del menú insertar ...
        } else if (id == R.id.ins_dibujo) {
            Intent intent = new Intent(this, DibujoActivity.class);
            dibujoLauncher.launch(intent);
        }
        return true;
    });

    bottomSheetInsertar.show();
    });
    }

    private void cargarNotaSAF() {
        if (uriArchivoActual == null) return;

        // Clear previous attachments and lists
        contenedorAdjuntos.removeAllViews();
        listaRutasAudios.clear();
        listaRutasFotos.clear();
        listaRutasDibujos.clear();

        StringBuilder rawContentBuilder = new StringBuilder(); // To hold original content
        try (InputStream is = getContentResolver().openInputStream(uriArchivoActual);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = br.readLine()) != null) {
                rawContentBuilder.append(line).append("\n");
            }
            
            String content = rawContentBuilder.toString();
            StringBuilder contentWithoutTags = new StringBuilder();

            // Regex to find all attachment tags
            Pattern pattern = Pattern.compile("\\[\\[(AUDIO|FOTO|DIBUJO):\\s*(.*?)\\]\\]");
            Matcher matcher = pattern.matcher(content);

            int lastIndex = 0;
            while (matcher.find()) {
                String fullTag = matcher.group(0); // The entire [[TYPE:path]] string
                String type = matcher.group(1);
                String path = matcher.group(2);

                // Append text before this tag
                contentWithoutTags.append(content.substring(lastIndex, matcher.start()));

                // Add to respective list and display visual card
                switch (type) {
                    case "AUDIO":
                        listaRutasAudios.add(path);
                        agregarAdjuntoVisual(path, "AUDIO", fullTag);
                        break;
                    case "FOTO":
                        listaRutasFotos.add(path);
                        agregarAdjuntoVisual(path, "FOTO", fullTag);
                        break;
                    case "DIBUJO": // Handle drawings similarly
                        listaRutasDibujos.add(path);
                        agregarAdjuntoVisual(path, "DIBUJO", fullTag);
                        break;
                }
                lastIndex = matcher.end();
            }
            // Append any remaining text after the last tag
            contentWithoutTags.append(content.substring(lastIndex));

            esCambioProgramatico = true;
            txtNota.setText(contentWithoutTags.toString().trim());
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
        guardarNotaSilenciosamente();
        Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show(); // Show toast only on explicit save
    }

    private void guardarNotaSilenciosamente() {
        if (uriArchivoActual == null && txtTitulo.getText().toString().trim().isEmpty() && txtNota.getText().toString().trim().isEmpty()) {
            return; // Don't save empty new notes
        }

        String contenidoParaGuardar = txtNota.getText().toString();
        String titulo = txtTitulo.getText().toString().trim();

        if (titulo.isEmpty()) titulo = "Sin_titulo";

        try {
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

            try (OutputStream os = getContentResolver().openOutputStream(uriArchivoActual, "wt")) {
                os.write(contenidoParaGuardar.getBytes());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al autoguardar", Toast.LENGTH_LONG).show();
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
                contenedorAdjuntos.removeAllViews();
                listaRutasAudios.clear();
                listaRutasFotos.clear();
                listaRutasDibujos.clear();
                return true;
            } else if (itemId == R.id.pip) { 
                iniciarModoFlotante();
                return true;
            }else if (itemId == R.id.insertar_fecha) { // <--- NUEVO BLOQUE
            insertarFechaHoraEnCursor();
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
    // Si estamos deshaciendo/rehaciendo, no registramos el cambio como uno nuevo
    if (esCambioProgramatico) return;

    // Eliminar cambios futuros si estábamos en medio del historial
    while (historial.size() > posicionHistorial + 1) {
        historial.remove(historial.size() - 1);
    }

    // NUEVO: Limitar memoria (Máximo 50 estados)
    if (historial.size() >= 50) {
        historial.remove(0); // Borra el estado más antiguo
        posicionHistorial--;  // Ajusta la posición actual
    }

    historial.add(texto);
    posicionHistorial++;
    }

    private void deshacer() {
    if (posicionHistorial > 0) {
        esCambioProgramatico = true;
        posicionHistorial--;
        txtNota.setText(historial.get(posicionHistorial));
        txtNota.setSelection(txtNota.getText().length()); // Mover cursor al final
        esCambioProgramatico = false;
    }
    }

    private void rehacer() {
    if (posicionHistorial < historial.size() - 1) {
        esCambioProgramatico = true;
        posicionHistorial++;
        txtNota.setText(historial.get(posicionHistorial));
        txtNota.setSelection(txtNota.getText().length());
        esCambioProgramatico = false;
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
    private void insertarFechaHoraEnCursor() {
    // 1. Obtener fecha y hora actual con el formato deseado
    String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
    
    // 2. Obtener la posición del cursor
    int start = Math.max(txtNota.getSelectionStart(), 0);
    int end = Math.max(txtNota.getSelectionEnd(), 0);
    
    // 3. Insertar (o reemplazar si hay texto seleccionado)
    // Agregamos un espacio antes y después para que no quede pegado
    String textoAInsertar = " " + fechaHora + " "; 
    
    txtNota.getText().replace(Math.min(start, end), Math.max(start, end), textoAInsertar);
    }
    
    @Override
    protected void onPause() {
    super.onPause();
    // Forzamos el guardado al salir de la pantalla
    guardarNotaSilenciosamente();
    }
    private void iniciarGrabacion() {
    // 1. Definir dónde se guardará
    // Usamos la carpeta de la app para no complicarnos con permisos de Android 10+
    String nombreArchivo = "audio_" + System.currentTimeMillis() + ".3gp";
    rutaArchivoAudio = getExternalFilesDir(null).getAbsolutePath() + "/" + nombreArchivo;

    // 2. Configurar el MediaRecorder
    mediaRecorder = new MediaRecorder();
    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    mediaRecorder.setOutputFile(rutaArchivoAudio);

    try {
        mediaRecorder.prepare();
        mediaRecorder.start();
        estaGrabando = true;
        Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
        e.printStackTrace();
        Toast.makeText(this, "Error al iniciar grabación", Toast.LENGTH_SHORT).show();
    }
    }

    private void detenerGrabacion() {
    if (estaGrabando && mediaRecorder != null) {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            estaGrabando = false;
            
            Toast.makeText(this, "Audio guardado en: " + rutaArchivoAudio, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }
    
    private void abrirCamara() {
    try {
        // 1. Crear el archivo donde se guardará la foto
        String nombreFoto = "foto_" + System.currentTimeMillis() + ".jpg";
        File directorio = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "");
        if (!directorio.exists()) {
            directorio.mkdirs();
        }
        File imagenFile = new File(directorio, nombreFoto);
        
        // 2. Obtener la URI segura usando el FileProvider
        uriFotoActual = androidx.core.content.FileProvider.getUriForFile(
                this, 
                getPackageName() + ".fileprovider", 
                imagenFile
        );

        // 3. Lanzar la cámara
        tomarFotoLauncher.launch(uriFotoActual);
        
    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show();
    }
    }
    
    // New unified method to add visual attachments
    private void agregarAdjuntoVisual(String ruta, String tipo, String tagInText) {
        contenedorAdjuntos.setVisibility(View.VISIBLE);
        LayoutInflater inflater = getLayoutInflater();
        View attachmentView = null;

        switch (tipo) {
            case "AUDIO":
                attachmentView = inflater.inflate(R.layout.item_audio_adjunto, contenedorAdjuntos, false);
                setupAudioAttachmentView(attachmentView, ruta, tagInText);
                break;
            case "FOTO":
            case "DIBUJO": // Drawings will use the same layout as photos for now
                attachmentView = inflater.inflate(R.layout.item_foto_adjunta, contenedorAdjuntos, false);
                setupImageAttachmentView(attachmentView, ruta, tagInText);
                break;
        }

        if (attachmentView != null) {
            contenedorAdjuntos.addView(attachmentView);
        }
    }
    
    // Helper for Audio attachment view setup
    private void setupAudioAttachmentView(View viewAudio, String rutaAudio, String tagInText) {
        ImageView btnPlay = viewAudio.findViewById(R.id.btnPlayAudio);
        ImageView btnEliminar = viewAudio.findViewById(R.id.btnEliminarAudio);
        ProgressBar progressBar = viewAudio.findViewById(R.id.progressAudio);
        // TextView txtDuracion = viewAudio.findViewById(R.id.txtDuracion); // If you want to show duration

        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.play_circle_outline);
            } else {
                try {
                    if (mediaPlayer == null) {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(rutaAudio);
                        mediaPlayer.prepare();
                        mediaPlayer.setOnCompletionListener(mp -> {
                            btnPlay.setImageResource(R.drawable.play_circle_outline);
                            progressBar.setProgress(0);
                            if (mediaPlayer != null) { // Check before releasing
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                        });
                    }
                    mediaPlayer.start();
                    btnPlay.setImageResource(R.drawable.pause_circle_outline);
                    actualizarProgresoAudio(progressBar);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error al reproducir", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEliminar.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            contenedorAdjuntos.removeView(viewAudio);
            if (contenedorAdjuntos.getChildCount() == 0) {
                contenedorAdjuntos.setVisibility(View.GONE);
            }
            // Remove tag from main text
            removeTagFromText(tagInText);
            // Also remove from internal list
            listaRutasAudios.remove(rutaAudio);
            // Optionally delete the physical file: new File(rutaAudio).delete(); 
        });
    }

    // Helper for Image/Drawing attachment view setup
    private void setupImageAttachmentView(View viewImage, String rutaImagen, String tagInText) {
        ImageView imgAdjunta = viewImage.findViewById(R.id.imgAdjunta);
        ImageButton btnEliminarFoto = viewImage.findViewById(R.id.btnEliminarFoto);

        try {
            Uri uri = Uri.parse(rutaImagen);
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            is.close();

            int anchoContenedor = contenedorAdjuntos.getWidth();
            if (anchoContenedor <= 0) {
                // Fallback to screen width minus some margin if layout is not yet measured
                anchoContenedor = getResources().getDisplayMetrics().widthPixels - (int) (32 * getResources().getDisplayMetrics().density); 
            }
            
            // Adjust for padding (LinearLayout has 16dp horizontal padding)
            int padding = (int) (16 * getResources().getDisplayMetrics().density); 
            int targetWidth = anchoContenedor - padding * 2; 

            if (targetWidth <= 0) targetWidth = 800; // Fallback if calculation yields non-positive width

            float ratio = (float) bitmap.getHeight() / bitmap.getWidth();
            int targetHeight = (int) (targetWidth * ratio);
            
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            imgAdjunta.setImageBitmap(scaledBitmap);
            
        } catch (Exception e) {
            e.printStackTrace();
            imgAdjunta.setImageResource(android.R.drawable.ic_menu_gallery); // Fallback icon
            Toast.makeText(this, "Error al cargar imagen/dibujo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        btnEliminarFoto.setOnClickListener(v -> {
            contenedorAdjuntos.removeView(viewImage);
            if (contenedorAdjuntos.getChildCount() == 0) {
                contenedorAdjuntos.setVisibility(View.GONE);
            }
            // Remove tag from main text
            removeTagFromText(tagInText);
            // Also remove from internal list
            if (tagInText.contains("[[FOTO:")) {
                listaRutasFotos.remove(rutaImagen);
            } else if (tagInText.contains("[[DIBUJO:")) {
                listaRutasDibujos.remove(rutaImagen);
            }
            // Optionally delete the physical file: new File(rutaImagen).delete();
        });
    }

    // Helper to remove a specific tag from the main EditText
    private void removeTagFromText(String tag) {
        String currentText = txtNota.getText().toString();
        int startIndex = currentText.indexOf(tag);
        if (startIndex != -1) {
            int endIndex = startIndex + tag.length();
            esCambioProgramatico = true;
            txtNota.getText().delete(startIndex, endIndex);
            esCambioProgramatico = false;
        }
    }


// Método auxiliar para mover la barrita
    private void actualizarProgresoAudio(ProgressBar bar) {
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
        int current = mediaPlayer.getCurrentPosition();
        int total = mediaPlayer.getDuration();
        
        // Calcular porcentaje
        if (total > 0) {
            bar.setProgress((current * 100) / total);
        }
        
        // Repetir cada 100ms
        handlerAudio.postDelayed(() -> actualizarProgresoAudio(bar), 100);
    }
    }
    
    // The method that adds the audio to the editor after recording
    private void insertarAudioEnNota(String rutaAudio) {
        listaRutasAudios.add(rutaAudio); // Add to internal tracking list
        String tag = "\n[[AUDIO:" + rutaAudio + "]]\n"; // Ensure tag format matches regex
        insertarTextoEnCursor(tag);
        agregarAdjuntoVisual(rutaAudio, "AUDIO", tag);
    }
    
    // The method that adds a photo to the editor after selection/capture
    private void insertarFotoEnNota(Uri uriFoto) {
        String rutaFoto = uriFoto.toString();
        listaRutasFotos.add(rutaFoto); // Add to internal tracking list
        String tag = "\n[[FOTO:" + rutaFoto + "]]\n"; // Ensure tag format matches regex
        insertarTextoEnCursor(tag);
        agregarAdjuntoVisual(rutaFoto, "FOTO", tag);
    }

    // The method that adds a drawing to the editor after creation
    private void insertarDibujoEnNota(Uri uriDibujo) {
        String rutaDibujo = uriDibujo.toString();
        listaRutasDibujos.add(rutaDibujo); // Add to internal tracking list
        String tag = "\n[[DIBUJO:" + rutaDibujo + "]]\n"; // Ensure tag format matches regex
        insertarTextoEnCursor(tag);
        agregarAdjuntoVisual(rutaDibujo, "DIBUJO", tag);
    }
    
    // Helper to insert text at cursor position
    private void insertarTextoEnCursor(String text) {
        int start = Math.max(txtNota.getSelectionStart(), 0);
        int end = Math.max(txtNota.getSelectionEnd(), 0);
        txtNota.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
    }
}