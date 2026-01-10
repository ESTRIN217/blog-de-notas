package com.Jhon.myempty.blogdenotasjava;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.Jhon.myempty.blogdenotasjava.ChangelogActivity;
import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.content.Intent;

public class SettingsActivity extends AppCompatActivity {

    // Vistas
    private ImageView btnAtras;
    private RadioGroup grupoTema;
    private RadioButton rbClaro, rbOscuro, rbSistema;
    private SwitchMaterial switchMaterialTheme;
    private Slider sliderTamano;
    private TextView txtMuestra;
    private RadioGroup radioGroupColores;
    private MaterialButton novedades;

    // Preferencias
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MisPreferencias";
    private static final String KEY_THEME = "tema_elegido";
    private static final String KEY_MATERIAL_SWITCH = "material_theme_activado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 1. Configuración Previa (Tema y Colores)
        int temaGuardado = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(temaGuardado);

        if (prefs.getBoolean(KEY_MATERIAL_SWITCH, false)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings); // Asegúrate que el XML se llame 'settings.xml'

        inicializarVistas();
        cargarPreferencias();
        configurarListeners();
    }

    private void inicializarVistas() {
        // Botones Generales
        btnAtras = findViewById(R.id.btnAtrasSettings);
        
        // Sección Tema
        grupoTema = findViewById(R.id.grupoTema);
        rbClaro = findViewById(R.id.rbClaro);
        rbOscuro = findViewById(R.id.rbOscuro);
        rbSistema = findViewById(R.id.rbSistema);
        switchMaterialTheme = findViewById(R.id.switchMaterialTheme);

        // Sección Editor (NUEVO)
        sliderTamano = findViewById(R.id.sliderTamano);
        txtMuestra = findViewById(R.id.txtMuestraTamano);
        radioGroupColores = findViewById(R.id.radioGroupColores);
        novedades = findViewById(R.id.novedades);
    }

    private void cargarPreferencias() {
        // 1. Cargar Tema
        int temaGuardado = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (temaGuardado == AppCompatDelegate.MODE_NIGHT_NO) rbClaro.setChecked(true);
        else if (temaGuardado == AppCompatDelegate.MODE_NIGHT_YES) rbOscuro.setChecked(true);
        else rbSistema.setChecked(true);

        switchMaterialTheme.setChecked(prefs.getBoolean(KEY_MATERIAL_SWITCH, false));

        // 2. Cargar Tamaño Fuente
        float tamanoGuardado = prefs.getFloat("editor_font_size", 16f);
        sliderTamano.setValue(tamanoGuardado);
        txtMuestra.setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanoGuardado);
        txtMuestra.setText("Texto de ejemplo (" + (int)tamanoGuardado + "sp)");

        // 3. Cargar Color Fondo
        int colorMode = prefs.getInt("editor_bg_mode", 0);
        switch (colorMode) {
            case 1: ((RadioButton)findViewById(R.id.rbPapel)).setChecked(true); break;
            case 2: ((RadioButton)findViewById(R.id.rbNegro)).setChecked(true); break;
            default: ((RadioButton)findViewById(R.id.rbBlanco)).setChecked(true); break;
        }
    }

    private void configurarListeners() {
        btnAtras.setOnClickListener(v -> finish());

        // --- Lógica de TEMA ---
        grupoTema.setOnCheckedChangeListener((group, checkedId) -> {
            int modoSeleccionado;
            if (checkedId == R.id.rbClaro) modoSeleccionado = AppCompatDelegate.MODE_NIGHT_NO;
            else if (checkedId == R.id.rbOscuro) modoSeleccionado = AppCompatDelegate.MODE_NIGHT_YES;
            else modoSeleccionado = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

            if (prefs.getInt(KEY_THEME, -1) != modoSeleccionado) {
                prefs.edit().putInt(KEY_THEME, modoSeleccionado).apply();
                AppCompatDelegate.setDefaultNightMode(modoSeleccionado);
                Toast.makeText(this, getThemeToastMessage(modoSeleccionado), Toast.LENGTH_SHORT).show();
            }
        });

        switchMaterialTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_MATERIAL_SWITCH, isChecked).apply();
            Toast.makeText(this, isChecked ? "Colores dinámicos activados" : "Desactivados", Toast.LENGTH_SHORT).show();
            recreate();
        });

        // --- Lógica de EDITOR (Fuente y Color) ---
        sliderTamano.addOnChangeListener((slider, value, fromUser) -> {
            txtMuestra.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
            txtMuestra.setText("Texto de ejemplo (" + (int)value + "sp)");
            prefs.edit().putFloat("editor_font_size", value).apply();
        });

        radioGroupColores.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0;
            if (checkedId == R.id.rbPapel) mode = 1;
            else if (checkedId == R.id.rbNegro) mode = 2;
            prefs.edit().putInt("editor_bg_mode", mode).apply();
        });
        novedades.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangelogActivity.class));
        });
    }

    private String getThemeToastMessage(int mode) {
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_NO: return "Tema Claro activado";
            case AppCompatDelegate.MODE_NIGHT_YES: return "Tema Oscuro activado";
            default: return "Tema del Sistema activado";
        }
    }
}