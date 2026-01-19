package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout; // Ensure this is imported for contenedorAdjuntos
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.provider.MediaStore;

import android.text.Html;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Typeface;
import android.graphics.Color;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
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
    private TextView lblFecha, lblContador;
    private Button btnAtras, btnDeshacer, btnRehacer, btnGuardar, menu, añadir, paleta, textoEstilo;
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
    private DocumentFile archivoActualSAF; // Añade esta línea si no existe
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;


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
                // Añadimos a la lista para que guardarImagenesEnCarpetaNota la vea
                listaRutasFotos.add(uriDibujo.toString());
                agregarAdjuntoVisual(uriDibujo.toString(), "DIBUJO", "");
            }
        }
    });
    // 1. Variable para manejar la selección del fondo
    private final ActivityResultLauncher<String> seleccionarFondoLauncher = registerForActivityResult(
    new ActivityResultContracts.GetContent(),
    uri -> {
        if (uri != null) {
            try {
                // Opción A: Poner la imagen en el EditText (si es transparente)
                // Opción B: Ponerla en el Layout principal (Recomendado)
                
                // Carga simple usando Bitmap para ajustar al fondo
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                
                // Asumiendo que 'txtNota' o su padre es lo que quieres cambiar
                // findViewById(R.id.main).setBackground(drawable); // Si tienes ID en el root
                background.setBackground(drawable); 
            } catch (Exception e) {
                e.printStackTrace();
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
    // 1. Cargar Preferencias GENERALES (Solo para Tema y Material You)
    SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);

    // A. Aplicar Tema
    int temaGuardado = prefs.getInt("tema_elegido", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    AppCompatDelegate.setDefaultNightMode(temaGuardado);

    // B. Aplicar Colores Dinámicos
    if (prefs.getBoolean("material_theme_activado", false)) {
        DynamicColors.applyToActivityIfAvailable(this);
    }
    
    // 2. Configuración de UI
    androidx.activity.EdgeToEdge.enable(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.editor); 

    inicializarVistas(); 
    manejarIntent(); 
    
    establecerFecha();
    configurarBotones();

    // 3. Lógica de carga
    // NOTA: Ya no configuramos el color aquí. 'cargarNotaSAF()' se encarga de
    // leer el HTML y pintar el fondo automáticamente.
    if (uriArchivoActual != null) {
        cargarNotaSAF();
    } else {
        txtTitulo.setText("Nueva Nota");
    }

    // 4. Inicializar historial
    if (historial.isEmpty()) {
        historial.add(txtNota.getText().toString());
        posicionHistorial = 0;
    }
    
    // 5. Launchers de Imágenes
    seleccionarImagenLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                insertarFotoEnNota(uri);
                Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
            }
        }
    );

    tomarFotoLauncher = registerForActivityResult(
        new ActivityResultContracts.TakePicture(),
        success -> {
            if (success && uriFotoActual != null) {
                insertarFotoEnNota(uriFotoActual);
                Toast.makeText(this, "Foto añadida", Toast.LENGTH_SHORT).show();
            }
        }
    );
    }

    private void manejarIntent() {
    Intent intent = getIntent();
    // Buscamos la URI con ambas llaves por seguridad
    String dataRecibida = intent.getStringExtra("uri_archivo");
    if (dataRecibida == null) {
        dataRecibida = intent.getStringExtra("nombre_archivo");
    }

    SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
    carpetaUriPadre = prefs.getString("carpeta_uri", null);

    if (dataRecibida != null && !dataRecibida.isEmpty()) {
        uriArchivoActual = Uri.parse(dataRecibida);
        
        // 1. Leer el archivo físico
        String contenidoCargado = leerArchivo(uriArchivoActual);
        
        // 2. Extraer el color que guardó la burbuja y aplicarlo al fondo
        int color = extraerColorDeHtml(contenidoCargado);
        aplicarColorFondoDinamico(color);
        
        // 3. Limpiar el HTML (quitar el <div>) y poner el texto en el editor
        String textoLimpio = contenidoCargado.replaceAll("<div[^>]*>", "").replace("</div>", "");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            txtNota.setText(Html.fromHtml(textoLimpio, Html.FROM_HTML_MODE_LEGACY));
        } else {
            txtNota.setText(Html.fromHtml(textoLimpio));
        }
        
        // 4. Poner el nombre del archivo en el título
        DocumentFile df = DocumentFile.fromSingleUri(this, uriArchivoActual);
        if (df != null && df.getName() != null) {
            txtTitulo.setText(df.getName().replace(".txt", ""));
        }
    }
    }
    @Override
    protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent); // Actualizamos el intent de la actividad
    manejarIntent();   // Volvemos a procesar la información
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
        textoEstilo = findViewById(R.id.text_style);
        contenedorAdjuntos = findViewById(R.id.contenedorAdjuntos);
        lblContador = findViewById(R.id.lblContador);
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
            // 1. LÓGICA DE HISTORIAL (Undo/Redo)
            handlerHistorial.removeCallbacks(runnableHistorial);
            runnableHistorial = () -> registrarCambio(s.toString());
            handlerHistorial.postDelayed(runnableHistorial, 500);

            // 2. LÓGICA DE AUTOGUARDADO
            handlerHistorial.removeCallbacks(runnableAutoguardado);
            runnableAutoguardado = () -> guardarNotaSilenciosamente();
            handlerHistorial.postDelayed(runnableAutoguardado, 2000);
        }
        actualizarContador(s.toString());
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (esCambioProgramatico) return;

        int start = txtNota.getSelectionStart();
        int end = txtNota.getSelectionEnd();

        // Si el usuario está escribiendo (no borrando)
        if (start == end && start > 0) {
            // Bloqueamos temporalmente para evitar que el setSpan dispare el Watcher otra vez
            esCambioProgramatico = true;

            // NEGRITA
            if (isBoldActive) {
                s.setSpan(new StyleSpan(Typeface.BOLD), start - 1, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // CURSIVA
            if (isItalicActive) {
                s.setSpan(new StyleSpan(Typeface.ITALIC), start - 1, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // SUBRAYADO
            if (isUnderlineActive) {
                s.setSpan(new UnderlineSpan(), start - 1, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            esCambioProgramatico = false;
        }
    }
    });
        
        txtTitulo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                renombrarNota();
            }
        });
        

// En configurarListeners añade esto:
    añadir.setOnClickListener(v -> {
    BottomSheetDialog bottomSheetInsertar = new BottomSheetDialog(this);
    // 1. Inflamos la vista del menú
    View layout = getLayoutInflater().inflate(R.layout.bottom_sheet_insertar, null);
    bottomSheetInsertar.setContentView(layout);
    
    // 2. Vinculamos las vistas USANDO 'layout.'
    // Asumo que son View, LinearLayout o ImageView según tu XML
    View btnFoto = layout.findViewById(R.id.ins_foto);
    View btnCamara = layout.findViewById(R.id.ins_camara); // ID corregido
    View btnAudio = layout.findViewById(R.id.ins_audio);
    View btnDibujo = layout.findViewById(R.id.ins_dibujo); // ID corregido

    // 3. Configurar Listeners

    // --- FOTO ---
    btnFoto.setOnClickListener(view -> {
        seleccionarImagenLauncher.launch("image/*");
        bottomSheetInsertar.dismiss(); // Cerramos el menú al elegir
    });

    // --- CÁMARA ---
    btnCamara.setOnClickListener(view -> {
        abrirCamara();
        bottomSheetInsertar.dismiss();
    });

    // --- AUDIO ---
    btnAudio.setOnClickListener(view -> {
        // Al tener un 'if', las llaves {} son obligatorias
        if (chequearPermisosAudio()) {
            mostrarGrabadoraVisual();
        } else {
            requestPermissionsAudio();
        }
        bottomSheetInsertar.dismiss();
    });

    // --- DIBUJO ---
    btnDibujo.setOnClickListener(view -> {
        // Al declarar variables, las llaves {} son obligatorias
        Intent intent = new Intent(this, DibujoActivity.class);
        dibujoLauncher.launch(intent); // Corregido: .launch() en vez de .layout()
        bottomSheetInsertar.dismiss();
    });

    bottomSheetInsertar.show();
    });
    paleta.setOnClickListener(v -> {
    BottomSheetDialog bottomSheetPaleta = new BottomSheetDialog(this);
    View layout = getLayoutInflater().inflate(R.layout.bottom_sheet_paleta, null);
    bottomSheetPaleta.setContentView(layout);

    LinearLayout contenedorColores = layout.findViewById(R.id.contenedorColoresFondo);
    View btnDefault = layout.findViewById(R.id.color_default);
    View btnGaleria = layout.findViewById(R.id.btnAbrirGaleriaFondo);
    View btnPapel = layout.findViewById(R.id.fondo_papel);

    // 1. Botón "Default" usando el contenedor estándar de Material 3
    btnDefault.setOnClickListener(view -> {
        int colorSistema = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainer);
        aplicarColorFondoDinamico(colorSistema);
        bottomSheetPaleta.dismiss();
    });

    // 2. Lista de Atributos Material 3 (Pasteles automáticos que cambian en modo oscuro)
    int[] atributosMaterial = {
        com.google.android.material.R.attr.colorErrorContainer,      // Rojo/Rosa
        com.google.android.material.R.attr.colorPrimaryContainer,    // Azul
        com.google.android.material.R.attr.colorSecondaryContainer,  // Turquesa/Grisáceo
        com.google.android.material.R.attr.colorTertiaryContainer,   // Verde/Amarillo
        com.google.android.material.R.attr.colorSurfaceVariant,      // Gris Neutro
        com.google.android.material.R.attr.colorSurfaceInverse,    // Contraste alto
        com.google.android.material.R.attr.colorSurfaceContainerHighest // Gris oscuro
    };

    for (int attr : atributosMaterial) {
        // Obtenemos el color real del tema
        int colorResuelto = MaterialColors.getColor(this, attr, Color.LTGRAY);

        View circulo = new View(this);
        int size = (int) (50 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(margin, 0, margin, 0);
        circulo.setLayoutParams(params);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(colorResuelto);
        shape.setStroke(3, Color.parseColor("#40000000")); // Borde sutil
        circulo.setBackground(shape);

        // USAR aplicarColorFondoDinamico es fundamental aquí
        circulo.setOnClickListener(view -> {
            aplicarColorFondoDinamico(colorResuelto); 
            bottomSheetPaleta.dismiss();
        });

        contenedorColores.addView(circulo);
    }

    // 3. Fondo de Papel (usando un color crema cálido)
    if (btnPapel != null) {
        btnPapel.setOnClickListener(view -> {
            int colorPapel = Color.parseColor("#FFF8E1");
            aplicarColorFondoDinamico(colorPapel);
            bottomSheetPaleta.dismiss();
        });
    }

    bottomSheetPaleta.show();
    });
    // Dentro de tu onCreate o donde configures los botones
    textoEstilo.setOnClickListener(v -> {
    BottomSheetDialog dialogTextStyle = new BottomSheetDialog(this);
    View layout = getLayoutInflater().inflate(R.layout.bottom_sheet_textstyle, null);
    dialogTextStyle.setContentView(layout);

    // --- 1. REFERENCIAS DE TAMAÑO ---
    View btnSmall = layout.findViewById(R.id.size_small);
    View btnNormal = layout.findViewById(R.id.size_normal);
    View btnLarge = layout.findViewById(R.id.size_large);

    // --- 2. REFERENCIAS DE ESTILO ---
    ImageView btnBold = layout.findViewById(R.id.format_bold);
    ImageView btnItalic = layout.findViewById(R.id.format_italic);
    ImageView btnUnderline = layout.findViewById(R.id.format_underlined);
    ImageView btnClear = layout.findViewById(R.id.format_clear);
    ImageView btnClose = layout.findViewById(R.id.closed);
    
    if (isBoldActive) btnBold.setColorFilter(Color.BLUE);
    if (isItalicActive) btnItalic.setColorFilter(Color.BLUE);
    if (isUnderlineActive) btnUnderline.setColorFilter(Color.BLUE);

    // --- LÓGICA DE TAMAÑOS ---
    btnSmall.setOnClickListener(view -> txtNota.setTextSize(14));
    btnNormal.setOnClickListener(view -> txtNota.setTextSize(18));
    btnLarge.setOnClickListener(view -> txtNota.setTextSize(24));

    btnBold.setOnClickListener(view -> {
    isBoldActive = !isBoldActive;
    // Opcional: Cambiar color del botón para indicar que está activo
    if (isBoldActive) {
        btnBold.setColorFilter(Color.BLUE);
    } else {
        btnBold.clearColorFilter();
    }
    });

// --- LÓGICA DE CURSIVA (Corregida) ---
    btnItalic.setOnClickListener(view -> {
    isItalicActive = !isItalicActive;
    });

    // --- LÓGICA DE SUBRAYADO ---
    btnUnderline.setOnClickListener(view -> {
        isUnderlineActive = !isUnderlineActive;
    });

    // --- LÓGICA DE LIMPIAR FORMATO ---
    btnClear.setOnClickListener(view -> {
        txtNota.setTypeface(null, android.graphics.Typeface.NORMAL);
        txtNota.setTextSize(18); // Tamaño por defecto
        txtNota.setPaintFlags(txtNota.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        Toast.makeText(this, "Formato restaurado", Toast.LENGTH_SHORT).show();
    });

    // --- CERRAR ---
    btnClose.setOnClickListener(view -> dialogTextStyle.dismiss());

    dialogTextStyle.show();
    });
    }
    // Método para extraer el color hexadecimal del HTML
    private int extraerColorDeHtml(String htmlContent) {
    try {
        // Buscamos el patrón: background-color: #XXXXXX;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("background-color:\\s*(#[0-9A-Fa-f]{6,8})");
        java.util.regex.Matcher matcher = pattern.matcher(htmlContent);

        if (matcher.find()) {
            // Si encontramos el color, lo convertimos a entero
            return android.graphics.Color.parseColor(matcher.group(1));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    // Si no se encuentra color o falla, devolvemos TRANSPARENT o WHITE
    return android.graphics.Color.TRANSPARENT; 
    }

    private void cargarNotaSAF() {
    if (uriArchivoActual == null) return;

    archivoActualSAF = DocumentFile.fromSingleUri(this, uriArchivoActual);

    // 1. Limpieza de la interfaz
    contenedorAdjuntos.removeAllViews();
    listaRutasAudios.clear();
    listaRutasFotos.clear();
    listaRutasDibujos.clear();

    StringBuilder contentBuilder = new StringBuilder();
    try {
        // 2. Leer el archivo (HTML)
        try (InputStream is = getContentResolver().openInputStream(uriArchivoActual);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = br.readLine()) != null) {
                contentBuilder.append(line);
            }
        }
        
        String stringFinal = contentBuilder.toString();
        
        // 3. LÓGICA DE FONDO DINÁMICO
        // Extraemos el color y dejamos que 'aplicarColorFondoDinamico' decida el contraste
        int colorDetectado = extraerColorDeHtml(stringFinal);
        aplicarColorFondoDinamico(colorDetectado);

        // 4. Convertir HTML a texto visual
        esCambioProgramatico = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            txtNota.setText(Html.fromHtml(stringFinal, Html.FROM_HTML_MODE_LEGACY));
        } else {
            txtNota.setText(Html.fromHtml(stringFinal));
        }
        actualizarContador(txtNota.getText().toString());
        esCambioProgramatico = false;
        
        // 5. Configurar Título y Adjuntos
        if (archivoActualSAF != null && archivoActualSAF.getName() != null) {
            String nombreNota = archivoActualSAF.getName();
            txtTitulo.setText(nombreNota.replace(".txt", ""));

            if (carpetaUriPadre != null) {
                Uri rootUri = Uri.parse(carpetaUriPadre);
                DocumentFile carpetaRaiz = DocumentFile.fromTreeUri(this, rootUri);
                if (carpetaRaiz != null) {
                    cargarAdjuntosDesdeCarpeta(carpetaRaiz, nombreNota);
                }
            }
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
    // Validaciones iniciales
    if (uriArchivoActual == null && txtTitulo.getText().toString().trim().isEmpty() && txtNota.getText().toString().trim().isEmpty()) {
        return;
    }

    // 1. Obtener HTML del texto
    String contenidoHtml = Html.toHtml(txtNota.getText());
    String titulo = txtTitulo.getText().toString().trim();
    if (titulo.isEmpty()) titulo = "Sin_titulo";

    // --- NUEVA LÓGICA: COLOR DENTRO DEL HTML ---
    // a) Obtener el color actual del 'background'
    int colorActual = android.graphics.Color.TRANSPARENT;
    if (background.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
        colorActual = ((android.graphics.drawable.ColorDrawable) background.getBackground()).getColor();
    }

    // b) Convertir color a Hexadecimal (ej: #FFAFA8)
    // El 0xFFFFFF & colorActual asegura que el formato sea correcto
    String hexColor = String.format("#%06X", (0xFFFFFF & colorActual));

    // c) Envolver el contenido en un DIV con el estilo background-color
    // Este string es lo que realmente se escribirá en el archivo
    String htmlParaGuardar = "<div style='background-color:" + hexColor + ";'>" + contenidoHtml + "</div>";


    try {
        DocumentFile rootDoc = null;
        if (carpetaUriPadre != null) {
            Uri rootUri = Uri.parse(carpetaUriPadre);
            rootDoc = DocumentFile.fromTreeUri(this, rootUri);
        }

        // --- BLOQUE 1: CREACIÓN DE ARCHIVO Y CARPETA ---
        if (uriArchivoActual == null) {
            if (rootDoc == null) return;
            
            // Crear .txt
            DocumentFile nuevoArchivo = rootDoc.createFile("text/plain", titulo + ".txt");
            
            // Crear carpeta _resources
            String nombreCarpeta = titulo + "_resources";
            if (rootDoc.findFile(nombreCarpeta) == null) {
                rootDoc.createDirectory(nombreCarpeta);
            }

            if (nuevoArchivo != null) {
                uriArchivoActual = nuevoArchivo.getUri();
            }
        }

        // --- BLOQUE 2: GUARDAR EL HTML COMPLETO ---
        if (uriArchivoActual != null) {
            try (OutputStream os = getContentResolver().openOutputStream(uriArchivoActual, "wt")) {
                // AQUÍ USAMOS LA VERSIÓN CON EL COLOR INCRUSTADO
                os.write(htmlParaGuardar.getBytes());
            }

            archivoActualSAF = DocumentFile.fromSingleUri(this, uriArchivoActual);

            // --- BLOQUE 3: GUARDAR IMÁGENES (SharedPreferences ELIMINADO) ---
            if (archivoActualSAF != null && rootDoc != null) {
                 guardarImagenesEnCarpetaNota(rootDoc, archivoActualSAF.getName());
            }
        }

    } catch (Exception e) {
        Log.e("GUARDADO", "Error fatal: " + e.getMessage());
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
    private String leerArchivo(Uri uri) {
    StringBuilder contentBuilder = new StringBuilder();
    try (InputStream is = getContentResolver().openInputStream(uri);
         BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
        String line;
        while ((line = br.readLine()) != null) {
            contentBuilder.append(line);
        }
    } catch (Exception e) {
        e.printStackTrace();
        return ""; // Devuelve vacío si falla
    }
    return contentBuilder.toString();
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
    private void agregarAdjuntoVisual(String ruta, String tipo, String tag) {
    View vistaAdjunto = getLayoutInflater().inflate(R.layout.item_adjunto, null);
    ImageView miniatura = vistaAdjunto.findViewById(R.id.miniatura);
    ImageView btnEliminar = vistaAdjunto.findViewById(R.id.btnEliminar);
    ImageView btnEditar = vistaAdjunto.findViewById(R.id.btnEditar); // NUEVO

    // Cargar miniatura (puedes usar Glide o tu método actual)
    miniatura.setImageURI(Uri.parse(ruta));

    // Lógica del botón Editar
    btnEditar.setOnClickListener(v -> {
        if (tipo.equals("DIBUJO")) {
            // Si es dibujo, lo mandamos a DibujoActivity pasándole la URI
            Intent intent = new Intent(this, DibujoActivity.class);
            intent.putExtra("uri_dibujo_editar", ruta);
            dibujoLauncher.launch(intent);
        } else if (tipo.equals("FOTO")) {
            // Si es foto, podrías mandarla a un editor básico o al mismo de dibujo
            Intent intent = new Intent(this, DibujoActivity.class);
            intent.putExtra("uri_foto_editar", ruta); 
            dibujoLauncher.launch(intent);
        }
        // Al editar, eliminamos la versión vieja para que la nueva la reemplace
        contenedorAdjuntos.removeView(vistaAdjunto);
        listaRutasFotos.remove(ruta);
    });

    btnEliminar.setOnClickListener(v -> {
        // 1. Quitar de la vista y de la lista temporal
        contenedorAdjuntos.removeView(vistaAdjunto);
        listaRutasFotos.remove(ruta);

        // 2. ELIMINACIÓN FÍSICA: Intentar borrar el archivo real
        try {
            Uri uriABorrar = Uri.parse(ruta);
            // Si la URI es de nuestro proveedor de archivos (SAF), intentamos borrarlo
            DocumentFile archivo = DocumentFile.fromSingleUri(this, uriABorrar);
            if (archivo != null && archivo.exists()) {
                archivo.delete(); 
                Toast.makeText(this, "Archivo eliminado", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    contenedorAdjuntos.addView(vistaAdjunto);
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
            listaRutasAudios.remove(rutaAudio);
            // Optionally delete the physical file: new File(rutaAudio).delete(); 
        });
    }

    // Helper for Image/Drawing attachment view setup
    private void setupImageAttachmentView(View viewImage, String rutaImagen, String tagInText) {
        ImageView imgAdjunta = viewImage.findViewById(R.id.miniatura);
        ImageButton btnEliminarFoto = viewImage.findViewById(R.id.btnEliminar);

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
        });
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
        agregarAdjuntoVisual(rutaAudio, "AUDIO", "");
    }
    
    // The method that adds a photo to the editor after selection/capture
    private void insertarFotoEnNota(Uri uriFoto) {
    String rutaFoto = uriFoto.toString();
    listaRutasFotos.add(rutaFoto); 
    // Ya no insertamos texto en txtNota
    agregarAdjuntoVisual(rutaFoto, "FOTO", ""); 
    }

    private void insertarDibujoEnNota(Uri uriDibujo) {
    String rutaDibujo = uriDibujo.toString();
    listaRutasFotos.add(rutaDibujo); // Usamos la misma lista para facilitar el guardado
    agregarAdjuntoVisual(rutaDibujo, "DIBUJO", "");
    }
    
    private void guardarImagenesEnCarpetaNota(DocumentFile carpetaPadre, String nombreNota) {
    if (listaRutasFotos.isEmpty()) return; // Si no hay nada nuevo, salimos

    // Construir nombre de carpeta
    String nombreCarpeta = nombreNota.replace(".txt", "") + "_resources";

    DocumentFile carpetaRecursos = carpetaPadre.findFile(nombreCarpeta);
    // Si no existe (raro, porque la creamos arriba), la creamos
    if (carpetaRecursos == null) {
        carpetaRecursos = carpetaPadre.createDirectory(nombreCarpeta);
    }

    if (carpetaRecursos == null) {
        Log.e("GUARDADO", "ERROR CRÍTICO: No existe la carpeta " + nombreCarpeta);
        return; 
    }

    for (String ruta : listaRutasFotos) {
        // Evitamos guardar lo que ya está en resources (por si acaso)
        if (ruta.contains("_resources")) continue;

        try {
            Uri uriOrigen = Uri.parse(ruta);
            String extension = "png"; // Por defecto
            String mime = "image/png";
            
            // Detección simple para audio vs imagen
            if (ruta.endsWith("3gp")) { 
                extension = "3gp"; 
                mime = "audio/3gpp"; 
            }

            String fileName = "FILE_" + System.currentTimeMillis() + "." + extension;
            DocumentFile nuevoArchivo = carpetaRecursos.createFile(mime, fileName);

            if (nuevoArchivo != null) {
                try (InputStream in = getContentResolver().openInputStream(uriOrigen);
                     OutputStream out = getContentResolver().openOutputStream(nuevoArchivo.getUri())) {
                    
                    byte[] buffer = new byte[4096]; // Buffer un poco más grande
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    Log.d("GUARDADO", "Archivo guardado: " + fileName);
                }
            }
        } catch (Exception e) {
            Log.e("GUARDADO", "Fallo al copiar: " + e.getMessage());
        }
    }
    
    // Solo limpiamos la lista después de intentar guardar todo
    listaRutasFotos.clear(); 
    }
    
    private void cargarAdjuntosDesdeCarpeta(DocumentFile carpetaPadre, String nombreNota) {
    // Reconstruir el nombre de la carpeta de recursos
    String nombreCarpeta = nombreNota.replace(".txt", "") + "_resources";
    
    // Buscar la carpeta dentro de la raíz
    DocumentFile carpetaRecursos = carpetaPadre.findFile(nombreCarpeta);
    
    if (carpetaRecursos != null && carpetaRecursos.isDirectory()) {
        // Listar todos los archivos dentro
        for (DocumentFile archivo : carpetaRecursos.listFiles()) {
            
            // Filtro de seguridad: que el archivo exista y tenga tipo
            if (archivo.exists() && archivo.getUri() != null) {
                String tipoMime = archivo.getType();
                String uriString = archivo.getUri().toString();

                // Caso 1: Imágenes o Dibujos
                if (tipoMime != null && tipoMime.startsWith("image/")) {
                    runOnUiThread(() -> {
                        listaRutasFotos.add(uriString); // Añadimos a la lista para no perder referencia
                        agregarAdjuntoVisual(uriString, "FOTO", "");
                    });
                }
                // Caso 2: Audios (opcional, por si quieres que carguen también)
                else if (tipoMime != null && tipoMime.startsWith("audio/") || archivo.getName().endsWith(".3gp")) {
                    runOnUiThread(() -> {
                         listaRutasAudios.add(uriString);
                         agregarAdjuntoVisual(uriString, "AUDIO", "");
                    });
                }
            }
        }
    } else {
        Log.d("CARGA", "No se encontró carpeta de recursos: " + nombreCarpeta);
    }
    }
    private void aplicarColorFondoDinamico(int color) {
    // 1. CORRECCIÓN: Si el color recibido es transparente (0), asignamos blanco (o el del tema)
    if (color == Color.TRANSPARENT) {
        // Opción PRO para Material You: 
        color = com.google.android.material.color.MaterialColors.getColor(txtNota, com.google.android.material.R.attr.colorSurfaceContainer);
    }

    // 2. Aplicar el color al layout principal
    background.setBackgroundColor(color);

    // 3. Calcular luminancia (0.0 es negro puro, 1.0 es blanco puro)
    double luminancia = androidx.core.graphics.ColorUtils.calculateLuminance(color);

    // 4. Determinar color de contraste
    // Si el fondo es claro (> 0.5), texto oscuro (#1C1B1F es "Black" suave de Material)
    // Si el fondo es oscuro (< 0.5), texto blanco
    int colorInterfaz = (luminancia > 0.5) ? Color.parseColor("#1C1B1F") : Color.WHITE;
    if (lblContador != null) lblContador.setTextColor(colorInterfaz);

    // 5. Aplicar a todos los elementos de texto
    txtNota.setTextColor(colorInterfaz);
    txtTitulo.setTextColor(colorInterfaz);
    
    // Verificamos nulos por seguridad
    if (lblFecha != null) lblFecha.setTextColor(colorInterfaz);
    
    // 6. Aplicar a los iconos (importante para que no desaparezcan)
    if (btnAtras != null) btnAtras.setColorFilter(colorInterfaz);
    
    // 7. Estética final
    txtNota.setHintTextColor(colorInterfaz);
    
    // Nota: El alpha en el texto principal puede dificultar la lectura bajo el sol.
    // Yo recomendaría dejarlo en 1.0f (opaco) o 0.9f como máximo.
    txtNota.setAlpha(1.0f); 
    }
    private void actualizarContador(String texto) {
    // 1. Contar caracteres (incluyendo espacios)
    int caracteres = texto.length();

    // 2. Contar palabras
    // trim() quita espacios al inicio/final. 
    // split("\\s+") divide por cualquier tipo de espacio (espacio, tab, enter).
    String[] palabras = texto.trim().split("\\s+");
    int numPalabras = texto.trim().isEmpty() ? 0 : palabras.length;

    // 3. Mostrar en el TextView
    lblContador.setText(numPalabras + " palabras | " + caracteres + " caracteres");
    }
}