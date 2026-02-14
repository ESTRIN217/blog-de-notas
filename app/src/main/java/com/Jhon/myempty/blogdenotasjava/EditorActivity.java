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
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.Toast;
import android.widget.LinearLayout;;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.provider.MediaStore;

import android.text.Html;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Typeface;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;

import com.Jhon.myempty.blogdenotasjava.SimpleAdapter;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import android.media.MediaRecorder;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import android.widget.Chronometer;
import android.os.SystemClock;
import android.media.MediaPlayer;
import android.widget.ProgressBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.Path;
import android.view.LayoutInflater; // Added for LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.List;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.annotation.NonNull;
import java.io.FileOutputStream;


public class EditorActivity extends AppCompatActivity {

    private EditText txtNota, txtTitulo;
    private TextView lblFecha, lblContador;
    private MaterialButton btnAtras, btnDeshacer, btnRehacer, btnGuardar, menu, añadir, paleta, textoEstilo , textToSpeech;
    private View background;
    private RecyclerView contenedorAdjuntos;

    private ArrayList<String> historial = new ArrayList<>();
    private int posicionHistorial = -1;
    private boolean esCambioProgramatico = false;
    
    // Variables para manejar archivos con SAF
    private Uri uriArchivoActual; 
    private String carpetaUriPadre;
    private android.os.Handler handlerHistorial = new android.os.Handler();
    private Runnable runnableHistorial;
    private Runnable runnableAutoguardado;
    private Uri uriFotoCamara; // Para guardar temporalmente la foto de la cámara
    private MediaRecorder mediaRecorder;
    private String rutaArchivoAudio = null;
    private boolean estaGrabando = false;
    private static final int REQUEST_AUDIO_PERMISSION_CODE = 200;
    private MediaPlayer mediaPlayer;
    private android.os.Handler handlerAudio = new android.os.Handler();
    private DocumentFile archivoActualSAF; // Añade esta línea si no existe
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;
    private View vistaArrastrada;
    private SimpleAdapter adapterAdjuntos;
    private TextToSpeech mTTS;
    private boolean isTtsInitialized = false;
    private String currentBackgroundName = "default"; 
    private String currentBackgroundUri = null;
    private String archivoAudioTemporal;

// --- REGISTRO DE LAUNCHERS (DEBEN IR AQUÍ, FUERA DE MÉTODOS) ---

    private final ActivityResultLauncher<String> seleccionarImagenLauncher = registerForActivityResult(
    new ActivityResultContracts.GetContent(),
    uri -> {
        if (uri != null) {
            insertarFotoEnNota(uri);
            Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
        }
    }
    );

private final ActivityResultLauncher<Uri> tomarFotoLauncher = registerForActivityResult(
    new ActivityResultContracts.TakePicture(),
    success -> {
        if (success && uriFotoCamara != null) {
            insertarFotoEnNota(uriFotoCamara);
            Toast.makeText(this, "Foto añadida", Toast.LENGTH_SHORT).show();
        }
    }
);

public final ActivityResultLauncher<Intent> dibujoLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uriDibujo = result.getData().getData();
            if (uriDibujo != null) {
                String extra = result.getData().getStringExtra("tipo");
                int tipoItem = ("DIBUJO".equals(extra)) ? ItemAdjunto.TIPO_DIBUJO : ItemAdjunto.TIPO_IMAGEN;
                
                // USAMOS EL ADAPTER (Ya no agregarAdjuntoVisual ni listas paralelas)
                adapterAdjuntos.agregarItem(new ItemAdjunto(tipoItem, uriDibujo.toString()));
                contenedorAdjuntos.setVisibility(View.VISIBLE);
            }
        }
    });

private final ActivityResultLauncher<String> seleccionarFondoLauncher = registerForActivityResult(
    new ActivityResultContracts.GetContent(),
    uri -> {
        if (uri != null) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                currentBackgroundUri = uri.toString();
                currentBackgroundName = "custom_image";
                aplicarImagenFondoDinamico(uri);
            } catch (Exception e) {
                Toast.makeText(this, "Error al cargar fondo", Toast.LENGTH_SHORT).show();
            }
        }
    }
);

private final ActivityResultLauncher<Intent> launcherPermisoOverlay = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            iniciarServicioFlotante();
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
    MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarAudio);
    MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarAudio);

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
    if (rutaArchivoAudio != null) {
        insertarAudioEnNota(rutaArchivoAudio);
        bottomSheetDialog.dismiss();
        Toast.makeText(this, "Audio adjuntado", Toast.LENGTH_SHORT).show();
    }
    });
    
    bottomSheetDialog.setOnCancelListener(dialog -> {
    if (estaGrabando) detenerGrabacion();
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
    if (prefs.getBoolean("material_theme_activado", true)) {
        DynamicColors.applyToActivityIfAvailable(this);
    }
    
    // 2. Configuración de UI
    androidx.activity.EdgeToEdge.enable(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.editor); 

    inicializarVistas(); 
    configurarListaAdjuntos();
    
    inicializarHistorial();
    inicializarTTS();
    
    manejarIntent();
    establecerFecha();
    configurarBotones();

    // 3. Lógica de carga
    if (uriArchivoActual != null) {
        cargarNotaSAF();
    } else {
        txtTitulo.setText("Nueva Nota");
    }
    
    }
    
    private void inicializarHistorial() {
    // Verifica que la vista exista y que el historial esté limpio
    if (txtNota != null && historial != null && historial.isEmpty()) {
        historial.add(txtNota.getText().toString());
        posicionHistorial = 0;
    }
    }
    
    private void inicializarTTS() {
    mTTS = new TextToSpeech(this, status -> {
        if (status == TextToSpeech.SUCCESS) {
            // Usamos Locale de java.util
            Locale spanish = new Locale("es", "ES");
            int result = mTTS.setLanguage(spanish);
            
            if (result == TextToSpeech.LANG_MISSING_DATA 
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback al idioma del sistema
                mTTS.setLanguage(Locale.getDefault());
            }
            isTtsInitialized = true;
        } else {
            Log.e("TTS", "La inicialización falló");
        }
    });
    }

    private void manejarIntent() {
    Intent intent = getIntent();
    String dataRecibida = intent.getStringExtra("uri_archivo");
    if (dataRecibida == null) {
        dataRecibida = intent.getStringExtra("nombre_archivo");
    }

    SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
    carpetaUriPadre = prefs.getString("carpeta_uri", null);

    if (dataRecibida != null && !dataRecibida.isEmpty()) {
        uriArchivoActual = Uri.parse(dataRecibida);
        
        // 1. Leer el contenido completo usando el Helper
        // (Asegúrate de que el método en tu Helper se llame readContent)
        String contenidoCargado = NoteIOHelper.readContent(this, uriArchivoActual);
        
        // 2. Extraer y aplicar color (Corregido el punto y coma)
        int color = NoteIOHelper.extractColor(contenidoCargado); 
        aplicarColorFondoDinamico(color);
        
        // 3. Extraer nombre e imagen de fondo (Para que no se pierdan al editar)
        currentBackgroundName = NoteIOHelper.extractBackgroundName(contenidoCargado);
        currentBackgroundUri = NoteIOHelper.extractBackgroundImageUri(contenidoCargado);
        
        if (currentBackgroundUri != null) aplicarImagenFondoDinamico(Uri.parse(currentBackgroundUri));

        // 4. Limpiar el HTML y poner el texto en el editor
        String textoLimpio = NoteIOHelper.cleanHtmlForEditor(contenidoCargado);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            txtNota.setText(Html.fromHtml(textoLimpio, Html.FROM_HTML_MODE_LEGACY));
        } else {
            txtNota.setText(Html.fromHtml(textoLimpio));
        }
        
        // 5. Poner el nombre del archivo en el título
        DocumentFile df = DocumentFile.fromSingleUri(this, uriArchivoActual);
        if (df != null && df.getName() != null) {
            txtTitulo.setText(df.getName().replace(".txt", ""));
        }
        
        // 6. Cargar Checklist (Si lo tienes implementado)
        String checklistData = NoteIOHelper.extractChecklistData(contenidoCargado);
        if (!checklistData.isEmpty()) {
            // Aquí llamarías a tu método procesarChecklist(checklistData)
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
        contenedorAdjuntos.setLayoutManager(new LinearLayoutManager(this));contenedorAdjuntos.setAdapter(new SimpleAdapter(this));
        textToSpeech = findViewById(R.id.text_to_speech);
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
        

    añadir.setOnClickListener(v -> {
    BottomSheetDialog bottomSheetInsertar = new BottomSheetDialog(this);
    // 1. Inflamos la vista del menú
    View layout = getLayoutInflater().inflate(R.layout.bottom_sheet_insertar, null);
    bottomSheetInsertar.setContentView(layout);
    
    MaterialButton btnFoto = layout.findViewById(R.id.ins_foto);
    MaterialButton btnCamara = layout.findViewById(R.id.ins_camara);
    MaterialButton btnAudio = layout.findViewById(R.id.ins_audio);
    MaterialButton btnDibujo = layout.findViewById(R.id.ins_dibujo);
    MaterialButton btnCheck = layout.findViewById(R.id.check_box);//
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
        if (chequearPermisosAudio()) {
            mostrarGrabadoraVisual();
        } else {
            requestPermissionsAudio();
        }
        bottomSheetInsertar.dismiss();
    });

    // --- DIBUJO ---
    btnDibujo.setOnClickListener(view -> {
        Intent intent = new Intent(this, DibujoActivity.class);
        dibujoLauncher.launch(intent);
        bottomSheetInsertar.dismiss();
    });
    btnCheck.setOnClickListener(view -> {
    // 1. Crear el objeto de datos con el tipo correcto (ej: TIPO_CHECK)
    // Usamos el constructor: new ItemAdjunto(tipo, contenido)
    ItemAdjunto nuevoCheck = new ItemAdjunto(ItemAdjunto.TIPO_CHECK, "");
    nuevoCheck.setMarcado(false);

    // 2. Obtener el adaptador y agregar el objeto
    SimpleAdapter adapter = (SimpleAdapter) contenedorAdjuntos.getAdapter();
    if (adapter != null) {
        adapter.agregarItem(nuevoCheck);
        
        // 3. Hacer scroll hasta el nuevo elemento y asegurar visibilidad
        contenedorAdjuntos.scrollToPosition(adapter.getItemCount() - 1);
        contenedorAdjuntos.setVisibility(View.VISIBLE);
    }
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
        aplicarImagenFondoDinamico(null);
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
        // 4. Botón Galería: Abrir el selector de archivos
        if (btnGaleria != null) {
    btnGaleria.setOnClickListener(view -> {
        seleccionarFondoLauncher.launch("image/*"); 
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
    MaterialButton btnSmall = layout.findViewById(R.id.size_small);
    MaterialButton btnNormal = layout.findViewById(R.id.size_normal);
    MaterialButton btnLarge = layout.findViewById(R.id.size_large);

    // --- 2. REFERENCIAS DE ESTILO ---
    MaterialButton btnBold = layout.findViewById(R.id.format_bold);
    MaterialButton btnItalic = layout.findViewById(R.id.format_italic);
    MaterialButton btnUnderline = layout.findViewById(R.id.format_underlined);
    MaterialButton btnClear = layout.findViewById(R.id.format_clear);
    MaterialButton btnClose = layout.findViewById(R.id.closed);
    
    if (isBoldActive) btnBold.setCheckable(true);
    if (isItalicActive) btnItalic.setCheckable(true);
    if (isUnderlineActive) btnUnderline.setCheckable(true);

    // --- LÓGICA DE TAMAÑOS ---
    btnSmall.setOnClickListener(view -> txtNota.setTextSize(14));
    btnNormal.setOnClickListener(view -> txtNota.setTextSize(18));
    btnLarge.setOnClickListener(view -> txtNota.setTextSize(24));

    btnBold.setOnClickListener(view -> {
    isBoldActive = !isBoldActive;
    // Opcional: Cambiar color del botón para indicar que está activo
    if (isBoldActive) {
        btnBold.setCheckable(true);
    } else {
        btnBold.setCheckable(false);
    }
    });

    btnItalic.setOnClickListener(view -> {
    isItalicActive = !isItalicActive;
    if (isItalicActive) {
        btnItalic.setCheckable(true);
    } else {
        btnItalic.setCheckable(false);
    }
    });

    // --- LÓGICA DE SUBRAYADO ---
    btnUnderline.setOnClickListener(view -> {
        isUnderlineActive = !isUnderlineActive;
        if (isUnderlineActive) {
        btnUnderline.setCheckable(true);
    } else {
        btnUnderline.setCheckable(false);
    }
    });

    // --- LÓGICA DE LIMPIAR FORMATO ---
    btnClear.setOnClickListener(view -> {
        txtNota.setTypeface(null, android.graphics.Typeface.NORMAL);
        txtNota.setPaintFlags(txtNota.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        Toast.makeText(this, "Formato restaurado", Toast.LENGTH_SHORT).show();
    });

    // --- CERRAR ---
    btnClose.setOnClickListener(view -> dialogTextStyle.dismiss());

    dialogTextStyle.show();
    });
    textToSpeech.setOnClickListener(view -> {
    if (!isTtsInitialized || mTTS == null) {
        Toast.makeText(this, "El motor de voz aún no está listo", Toast.LENGTH_SHORT).show();
        return;
    }

    if (mTTS.isSpeaking()) {
        // Si ya está hablando, lo callamos (STOP)
        mTTS.stop();
        Toast.makeText(this, "Lectura detenida", Toast.LENGTH_SHORT).show();
    } else {
        // Si está callado, leemos el texto (PLAY)
        String textoALeer = txtNota.getText().toString(); 

        if (!textoALeer.isEmpty()) {
            // QUEUE_FLUSH: Interrumpe lo que esté diciendo y empieza esto nuevo
            mTTS.speak(textoALeer, TextToSpeech.QUEUE_FLUSH, null, null);
            Toast.makeText(this, "Leyendo nota...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay texto para leer", Toast.LENGTH_SHORT).show();
        }
    }
});
    }

    private void cargarNotaSAF() {
    if (uriArchivoActual == null) return;
    archivoActualSAF = DocumentFile.fromSingleUri(this, uriArchivoActual);

    // 1. Limpieza de interfaz (Ahora a través del adaptador)
    SimpleAdapter adapter = (SimpleAdapter) contenedorAdjuntos.getAdapter();
    if (adapter != null) {
        adapter.getListaDatos().clear();
        adapter.notifyDataSetChanged();
    }

    try {
        String fullContent = NoteIOHelper.readContent(this, uriArchivoActual);

        // 3. Aplicar Color y Nombre de Fondo
        int color = NoteIOHelper.extractColor(fullContent);
        currentBackgroundName = NoteIOHelper.extractBackgroundName(fullContent);
        aplicarColorFondoDinamico(color);

        // 4. Aplicar Imagen de Fondo (si existe)
        currentBackgroundUri = NoteIOHelper.extractBackgroundImageUri(fullContent);
        if (currentBackgroundUri != null && !currentBackgroundUri.isEmpty()) {
            aplicarImagenFondoDinamico(Uri.parse(currentBackgroundUri));
        }

        // 5. Cargar Checklist al Adaptador
        String checklistData = NoteIOHelper.extractChecklistData(fullContent);
        if (!checklistData.isEmpty() && adapter != null) {
            java.util.regex.Pattern patternItem = java.util.regex.Pattern.compile("<chk state=\"(true|false)\">(.*?)</chk>");
            java.util.regex.Matcher matcherItem = patternItem.matcher(checklistData);
            
            while (matcherItem.find()) {
                boolean estaMarcado = Boolean.parseBoolean(matcherItem.group(1));
                String textoTarea = matcherItem.group(2);
                
                // Creamos el objeto y lo añadimos al adaptador
                ItemAdjunto checkItem = new ItemAdjunto(ItemAdjunto.TIPO_CHECK, textoTarea);
                checkItem.setMarcado(estaMarcado);
                adapter.agregarItem(checkItem);
            }
        }

        // ... (Paso 6: Texto limpio se mantiene igual) ...

        // 7. Cargar Adjuntos Multimedia
        if (archivoActualSAF != null && archivoActualSAF.getName() != null) {
            String nombreNota = archivoActualSAF.getName();
            txtTitulo.setText(nombreNota.replace(".txt", ""));

            if (carpetaUriPadre != null) {
                DocumentFile carpetaRaiz = DocumentFile.fromTreeUri(this, Uri.parse(carpetaUriPadre));
                // Este método ya lo refactorizamos para usar adapter.agregarItem
                if (carpetaRaiz != null) cargarAdjuntosDesdeCarpeta(carpetaRaiz, nombreNota);
            }
        }
        
        // Mostrar contenedor si hay algo
        if (adapter != null && adapter.getItemCount() > 0) {
            contenedorAdjuntos.setVisibility(View.VISIBLE);
        }

    } catch (Exception e) {
        Log.e("CARGA_SAF", "Error: " + e.getMessage());
    }
    }

    private void guardarNota() {
        guardarNotaSilenciosamente();
        Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show(); // Show toast only on explicit save
    }

    private void guardarNotaSilenciosamente() {
    String titulo = txtTitulo.getText().toString().trim();
    if (uriArchivoActual == null && titulo.isEmpty() && txtNota.getText().toString().isEmpty()) return;
    if (titulo.isEmpty()) titulo = "Sin_titulo";

    // 1. Obtener datos desde el Adaptador
    SimpleAdapter adapter = (SimpleAdapter) contenedorAdjuntos.getAdapter();
    List<ItemAdjunto> listaDeAdjuntos = adapter.getListaDatos();

    // 2. Generar el HTML del Checklist filtrando solo los tipos CHECK
    StringBuilder sb = new StringBuilder();
    for (ItemAdjunto item : listaDeAdjuntos) {
        if (item.getTipo() == ItemAdjunto.TIPO_CHECK) {
            String state = item.isMarcado() ? "true" : "false";
            String text = item.getContenido()
                    .replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
            sb.append("<chk state=\"").append(state).append("\">")
              .append(text).append("</chk>");
        }
    }
    String checklistHtml = sb.toString();

    // 3. Preparar el resto de datos
    String bodyHtml = Html.toHtml(txtNota.getText());
    int colorActual = obtenerColorActual();

    try {
        DocumentFile rootDoc = (carpetaUriPadre != null) ? 
                DocumentFile.fromTreeUri(this, Uri.parse(carpetaUriPadre)) : null;

        // Creación de archivo si es nuevo
        if (uriArchivoActual == null && rootDoc != null) {
            DocumentFile nuevo = rootDoc.createFile("text/plain", titulo + ".txt");
            if (nuevo != null) uriArchivoActual = nuevo.getUri();
        }

        if (uriArchivoActual != null) {
            // Guardar archivo .txt principal
            boolean exito = NoteIOHelper.saveNote(this, uriArchivoActual, bodyHtml, 
                    checklistHtml, colorActual, currentBackgroundName, currentBackgroundUri);

            // Guardar recursos multimedia (Fotos, Audios, Dibujos)
            if (exito && rootDoc != null) {
                archivoActualSAF = DocumentFile.fromSingleUri(this, uriArchivoActual);
                // Este método usa el adaptador internamente como vimos antes
                guardarAdjuntosEnCarpetaNota(rootDoc, archivoActualSAF.getName());
            }
        }
    } catch (Exception e) {
        Log.e("GUARDADO", "Error: " + e.getMessage());
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
    String[] opciones = {"Enviar como texto", "Enviar como PDF"};

    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Compartir nota")
            .setItems(opciones, (dialog, which) -> {
                if (which == 0) {
                    compartirComoTexto();
                } else {
                    compartirComoPDF();
                }
            })
            .show();
}

private void compartirComoTexto() {
    String texto = txtNota.getText().toString();
    if (texto.isEmpty()) {
        Toast.makeText(this, "La nota está vacía", Toast.LENGTH_SHORT).show();
        return;
    }
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_TEXT, texto);
    startActivity(Intent.createChooser(intent, "Compartir texto vía"));
}

private void compartirComoPDF() {
    String contenido = txtNota.getText().toString();
    if (contenido.isEmpty()) {
        Toast.makeText(this, "No hay contenido para generar PDF", Toast.LENGTH_SHORT).show();
        return;
    }

    // 1. Crear el documento PDF
    android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
    // Tamaño A4 (595 x 842 puntos)
    android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
    android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);

    android.graphics.Canvas canvas = page.getCanvas();
    android.graphics.Paint paint = new android.graphics.Paint();
    paint.setTextSize(12);

    // Dibujar el texto (manejo básico de saltos de línea)
    int x = 50, y = 50;
    for (String line : contenido.split("\n")) {
        canvas.drawText(line, x, y, paint);
        y += paint.descent() - paint.ascent();
    }

    document.finishPage(page);

    // 2. Guardar el archivo en el cache para compartir
    try {
        File cachePath = new File(getCacheDir(), "pdf_temp");
        cachePath.mkdirs();
        File file = new File(cachePath, "Nota_" + System.currentTimeMillis() + ".pdf");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.close();

        // 3. Compartir el archivo generado
        Uri contentUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(intent, "Compartir PDF vía"));

    } catch (java.io.IOException e) {
        Toast.makeText(this, "Error al crear PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
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
    Intent intent = new Intent(this, FloatingService.class);
    
    // 1. Obtenemos el texto principal
    String textoCuerpo = txtNota.getText().toString(); 
    
    // 2. Generamos el HTML del checklist usando los DATOS del adaptador
    StringBuilder checklistHtml = new StringBuilder();
    checklistHtml.append("<div id='checklist_data' style='display:none;'>");
    
    if (adapterAdjuntos != null) { // Asegúrate de usar el nombre correcto de tu variable de adaptador
        for (ItemAdjunto item : adapterAdjuntos.getListaDatos()) { // Iteramos sobre objetos ItemAdjunto
            if (item.getTipo() == ItemAdjunto.TIPO_CHECK) {
                String isChecked = item.isMarcado() ? "true" : "false"; //
                String texto = item.getContenido(); //
                
                checklistHtml.append("<chk state=\"").append(isChecked).append("\">")
                             .append(texto).append("</chk>");
            }
        }
    }
    checklistHtml.append("</div>");

    // 3. Empaquetamos todo el HTML
    String contenidoCompleto = "<div>" + textoCuerpo + checklistHtml.toString() + "</div>";
    
    intent.putExtra("contenido_nota", contenidoCompleto);
    intent.putExtra("uri_archivo", uriArchivoActual != null ? uriArchivoActual.toString() : "");
    
    startService(intent);
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
    if (estaGrabando) {
        detenerGrabacion(); 
    }
    guardarNotaSilenciosamente();
    }
    private void iniciarGrabacion() {
    // 1. Definir ruta
    String nombreArchivo = "audio_" + System.currentTimeMillis() + ".3gp";
    File file = new File(getExternalFilesDir(null), nombreArchivo);
    rutaArchivoAudio = file.getAbsolutePath();

    // 2. Configurar MediaRecorder (Compatibilidad con Android 10+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        mediaRecorder = new MediaRecorder(this);
    } else {
        mediaRecorder = new MediaRecorder();
    }

    try {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(rutaArchivoAudio);

        mediaRecorder.prepare();
        mediaRecorder.start();
        estaGrabando = true;
        
        // Opcional: podrías cambiar el icono del botón de grabar aquí
        Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show();
    } catch (IOException | IllegalStateException e) {
        Log.e("AUDIO", "Error al grabar: " + e.getMessage());
        Toast.makeText(this, "No se pudo iniciar la grabación", Toast.LENGTH_SHORT).show();
    }
    }

    private void detenerGrabacion() {
    if (estaGrabando && mediaRecorder != null) {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            estaGrabando = false;
            // No añadimos al adapter aquí, esperamos a que el usuario pulse "Guardar"
        } catch (RuntimeException stopException) {
            Log.e("AUDIO", "Grabación demasiado corta");
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
        uriFotoCamara = androidx.core.content.FileProvider.getUriForFile(
                this, 
                getPackageName() + ".fileprovider", 
                imagenFile
        );

        // 3. Lanzar la cámara
        tomarFotoLauncher.launch(uriFotoCamara);
        
    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show();
    }
    }
    
   
    
    private void insertarAudioEnNota(String ruta) {
    if (ruta != null && adapterAdjuntos != null) {
        ItemAdjunto nuevoAudio = new ItemAdjunto(ItemAdjunto.TIPO_AUDIO, ruta);
        adapterAdjuntos.agregarItem(nuevoAudio);
        contenedorAdjuntos.setVisibility(View.VISIBLE);
        // Opcional: guardar automáticamente al añadir
        guardarNotaSilenciosamente();
    }
    }
    
    // The method that adds a photo to the editor after selection/capture
    private void insertarFotoEnNota(Uri uriFoto) {
    // Ya no insertamos texto en txtNota
    adapterAdjuntos.agregarItem(new ItemAdjunto(ItemAdjunto.TIPO_IMAGEN, uriFoto.toString()));
    }

    private void insertarDibujoEnNota(Uri uriDibujo) {
    adapterAdjuntos.agregarItem(new ItemAdjunto(ItemAdjunto.TIPO_DIBUJO, uriDibujo.toString()));
    }
    
    private void guardarAdjuntosEnCarpetaNota(DocumentFile carpetaPadre, String nombreNota) {
    // 1. Obtener la lista de ítems desde el adaptador
    List<ItemAdjunto> adjuntos = ((SimpleAdapter) contenedorAdjuntos.getAdapter()).getListaDatos();
    
    if (adjuntos.isEmpty()) return;

    String nombreCarpeta = nombreNota.replace(".txt", "") + "_resources";
    DocumentFile carpetaRecursos = carpetaPadre.findFile(nombreCarpeta);
    
    if (carpetaRecursos == null) {
        carpetaRecursos = carpetaPadre.createDirectory(nombreCarpeta);
    }

    if (carpetaRecursos == null) {
        Log.e("GUARDADO", "ERROR: No se pudo crear la carpeta " + nombreCarpeta);
        return;
    }

    // 2. Recorrer los adjuntos del adaptador
    for (ItemAdjunto item : adjuntos) {
        // Saltamos los checks, ya que esos suelen guardarse dentro del texto del .txt
        if (item.getTipo() == ItemAdjunto.TIPO_CHECK) continue;

        String ruta = item.getContenido(); // La URI o ruta del archivo
        
        try {
            Uri uriOrigen = Uri.parse(ruta);
            
            // Verificamos si es una imagen, dibujo o audio para decidir el nombre
            String prefijo = "FILE_";
            if (item.getTipo() == ItemAdjunto.TIPO_AUDIO) prefijo = "AUDIO_";
            if (item.getTipo() == ItemAdjunto.TIPO_DIBUJO) prefijo = "DIBUJO_";

            // Intentar obtener el MimeType real
            String mimeType = getContentResolver().getType(uriOrigen);
            if (mimeType == null) {
                mimeType = (item.getTipo() == ItemAdjunto.TIPO_AUDIO) ? "audio/3gp" : "image/png";
            }

            String extension = mimeType.contains("/") ? mimeType.split("/")[1] : "bin";
            String fileName = prefijo + System.currentTimeMillis() + "." + extension;
            
            DocumentFile nuevoArchivo = carpetaRecursos.createFile(mimeType, fileName);

            if (nuevoArchivo != null) {
                try (InputStream in = getContentResolver().openInputStream(uriOrigen);
                     OutputStream out = getContentResolver().openOutputStream(nuevoArchivo.getUri())) {
                    
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                
                item.setContenido(nuevoArchivo.getUri().toString());
            }
        } catch (Exception e) {
            Log.e("GUARDADO", "Error al guardar adjunto: " + e.getMessage());
        }
    }
    }
    
    private void cargarAdjuntosDesdeCarpeta(DocumentFile carpetaPadre, String nombreNota) {
    String nombreCarpeta = nombreNota.replace(".txt", "") + "_resources";
    DocumentFile carpetaRecursos = carpetaPadre.findFile(nombreCarpeta);
    
    if (carpetaRecursos != null && carpetaRecursos.isDirectory()) {
        for (DocumentFile archivo : carpetaRecursos.listFiles()) {
            if (archivo.exists() && archivo.getUri() != null) {
                String tipoMime = archivo.getType();
                String uriString = archivo.getUri().toString();
                String nombre = archivo.getName() != null ? archivo.getName() : "";

                runOnUiThread(() -> {
                    ItemAdjunto nuevoItem = null;

                    // Caso 1: Imágenes o Dibujos (Detectamos por nombre si es dibujo)
                    if (tipoMime != null && tipoMime.startsWith("image/")) {
                        int tipo = nombre.startsWith("DIBUJO_") ? ItemAdjunto.TIPO_DIBUJO : ItemAdjunto.TIPO_IMAGEN;
                        nuevoItem = new ItemAdjunto(tipo, uriString);
                    } 
                    // Caso 2: Audios
                    else if ((tipoMime != null && tipoMime.startsWith("audio/")) || nombre.endsWith(".3gp")) {
                        nuevoItem = new ItemAdjunto(ItemAdjunto.TIPO_AUDIO, uriString);
                    }

                    // Si identificamos el archivo, lo mandamos al adaptador
                    if (nuevoItem != null) {
                        ((SimpleAdapter) contenedorAdjuntos.getAdapter()).agregarItem(nuevoItem);
                        contenedorAdjuntos.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    } else {
        Log.d("CARGA", "No se encontró carpeta de recursos: " + nombreCarpeta);
    }
    }
    private void aplicarColorFondoDinamico(int color) {
    // 1. Si el color es transparente o 0, usamos el color por defecto del tema
    if (color == Color.TRANSPARENT || color == 0) {
        color = com.google.android.material.color.MaterialColors.getColor(background, com.google.android.material.R.attr.colorSurfaceContainer);
    }

    // 2. Aplicar al fondo
    background.setBackgroundColor(color);

    // 3. Calcular luminancia para contraste
    double luminancia = androidx.core.graphics.ColorUtils.calculateLuminance(color);
    int colorInterfaz = (luminancia > 0.5) ? Color.parseColor("#1C1B1F") : Color.WHITE;

    // 4. Aplicar a textos
    if (lblContador != null) lblContador.setTextColor(colorInterfaz);
    txtNota.setTextColor(colorInterfaz);
    txtTitulo.setTextColor(colorInterfaz);
    if (lblFecha != null) lblFecha.setTextColor(colorInterfaz);
    txtNota.setHintTextColor(colorInterfaz);
    
    // 6. Ajustar la barra de estado (opcional, para que los iconos de batería/hora cambien)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        View decor = getWindow().getDecorView();
        if (luminancia > 0.5) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decor.setSystemUiVisibility(0);
        }
    }
    }
    
    private void aplicarImagenFondoDinamico(Uri uri) {
    if (uri == null) return;

    try {
        // 1. Abrir el flujo de la imagen desde la URI de SAF
        InputStream inputStream = getContentResolver().openInputStream(uri);
        Drawable drawable = Drawable.createFromStream(inputStream, uri.toString());

        if (drawable != null) {
            // 2. Si es un BitmapDrawable, podemos ajustar cómo se ve
            if (drawable instanceof BitmapDrawable) {
                // Opcional: Ajustar opacidad si la imagen es muy brillante para leer texto
                // drawable.setAlpha(150); // 0 a 255
            }

            // 3. Aplicar al layout principal (background es tu contenedor)
            background.setBackground(drawable);
            
            // 4. Guardar la URI actual en tu variable global para que persista al guardar
            currentBackgroundUri = uri.toString();
            
            // 5. IMPORTANTE: Re-calcular el contraste del texto sobre la imagen
            // Como no sabemos si la imagen es oscura o clara, una técnica segura
            // es poner un pequeño filtro oscuro o claro al texto si fuera necesario.
        }
    } catch (Exception e) {
        Log.e("FONDO_DINAMICO", "Error al cargar imagen de fondo: " + e.getMessage());
        // Si falla la imagen, al menos aplicamos un color sólido de respaldo
        if (background.getBackground() == null) {
            background.setBackgroundColor(Color.WHITE);
        }
    }
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
    private void configurarListaAdjuntos() {
    // Inicialización del adaptador
    adapterAdjuntos = new SimpleAdapter(this);
    contenedorAdjuntos.setLayoutManager(new LinearLayoutManager(this));
    contenedorAdjuntos.setAdapter(adapterAdjuntos);

    // Configuración del arrastre (Drag & Drop)
    ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        
        @Override
        public boolean onMove(@NonNull RecyclerView rv, 
                              @NonNull RecyclerView.ViewHolder dragged, 
                              @NonNull RecyclerView.ViewHolder target) {
            
            int from = dragged.getAbsoluteAdapterPosition();
            int to = target.getAbsoluteAdapterPosition();
            
            // Llama al método swap interno del adaptador
            if (adapterAdjuntos != null) {
                adapterAdjuntos.moverItem(from, to); 
            }
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // Sin implementación de deslizamiento para borrar
        }
    });
    
    helper.attachToRecyclerView(contenedorAdjuntos);
    }
    @Override
    protected void onDestroy() {
    // 1. Detener TextToSpeech
    if (mTTS != null) {
        mTTS.stop();
        mTTS.shutdown();
    }

    // 2. IMPORTANTE: Limpiar el audio del Adaptador
    if (adapterAdjuntos != null) {
        adapterAdjuntos.liberarRecursos(); 
    }

    // 3. Limpiar recursos locales si aún tienes grabación en la Activity
    if (mediaRecorder != null) {
        try {
            mediaRecorder.release();
        } catch (Exception e) {
            Log.e("onDestroy", "Error liberando recorder: " + e.getMessage());
        }
        mediaRecorder = null;
    }

    // Siempre al final
    super.onDestroy();
    }
    private int obtenerColorActual() {
    if (background.getBackground() instanceof ColorDrawable) {
        return ((ColorDrawable) background.getBackground()).getColor();
    }
    return Color.WHITE; // Color por defecto
    }
}