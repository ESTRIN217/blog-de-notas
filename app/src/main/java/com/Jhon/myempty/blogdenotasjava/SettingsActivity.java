package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    // Vistas
    private ImageView btnAtras;
    private RadioGroup grupoTema;
    private RadioButton rbClaro, rbOscuro, rbSistema;
    private MaterialSwitch switchMaterialTheme;
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
        androidx.activity.EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings); 

        inicializarVistas();
        cargarPreferencias();
        configurarListeners();
    }

    private void inicializarVistas() {
        btnAtras = findViewById(R.id.btnAtrasSettings);
        grupoTema = findViewById(R.id.grupoTema);
        rbClaro = findViewById(R.id.rbClaro);
        rbOscuro = findViewById(R.id.rbOscuro);
        rbSistema = findViewById(R.id.rbSistema);
        switchMaterialTheme = findViewById(R.id.switchMaterialTheme);
        novedades = findViewById(R.id.novedades);
    }

    private void cargarPreferencias() {
        int temaGuardado = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (temaGuardado == AppCompatDelegate.MODE_NIGHT_NO) rbClaro.setChecked(true);
        else if (temaGuardado == AppCompatDelegate.MODE_NIGHT_YES) rbOscuro.setChecked(true);
        else rbSistema.setChecked(true);

        switchMaterialTheme.setChecked(prefs.getBoolean(KEY_MATERIAL_SWITCH, false));
    } // <-- Aquí estaba el error (había una llave extra cerrando la clase)

    private void configurarListeners() {
        btnAtras.setOnClickListener(v -> finish());

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