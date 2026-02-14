package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.Jhon.myempty.blogdenotasjava.InicioActivity;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    // Vistas
    private MaterialButtonToggleGroup toggleGrupoTema;
    private MaterialSwitch switchMaterialTheme;
    private MaterialToolbar toolbar;
    private MaterialCardView novedades, sobre, setup;

    // Preferencias
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MisPreferencias";
    private static final String KEY_THEME = "tema_elegido";
    private static final String KEY_MATERIAL_SWITCH = "material_theme_activado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

    // 1. Configuración Previa (Tema y Colores) - DEBE ir antes de super.onCreate
    int temaGuardado = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    AppCompatDelegate.setDefaultNightMode(temaGuardado);

    if (prefs.getBoolean(KEY_MATERIAL_SWITCH, true)) {
        DynamicColors.applyToActivityIfAvailable(this);
    }
    
    // Habilitar EdgeToEdge también se recomienda antes de super o justo después
    androidx.activity.EdgeToEdge.enable(this);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings); 

    // 2. Inicializar las vistas primero - AHORA las variables ya no son null
    inicializarVistas(); 

    // 3. Configurar el estado del Switch después de inicializarlo
    boolean isMaterial = prefs.getBoolean(KEY_MATERIAL_SWITCH, true);
    if (switchMaterialTheme != null) { // Verificación de seguridad
        switchMaterialTheme.setChecked(isMaterial);
        switchMaterialTheme.setThumbIconResource(isMaterial ? R.drawable.round_check : R.drawable.close_24px);
    }

    cargarPreferencias();
    configurarListeners();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.topAppBar);
        toggleGrupoTema = findViewById(R.id.toggleGrupoTema);
        switchMaterialTheme = findViewById(R.id.switchMaterialTheme);
        novedades = findViewById(R.id.novedades);
        sobre = findViewById(R.id.sobre);
        setup = findViewById(R.id.setup);
    }

    private void cargarPreferencias() {
        int temaGuardado = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
if (temaGuardado == AppCompatDelegate.MODE_NIGHT_NO) {
    toggleGrupoTema.check(R.id.btnTemaClaro);
} else if (temaGuardado == AppCompatDelegate.MODE_NIGHT_YES) {
    toggleGrupoTema.check(R.id.btnTemaOscuro);
} else {
    toggleGrupoTema.check(R.id.btnTemaSistema);
}

        switchMaterialTheme.setChecked(prefs.getBoolean(KEY_MATERIAL_SWITCH, true));
    } // <-- Aquí estaba el error (había una llave extra cerrando la clase)

    private void configurarListeners() {
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        toggleGrupoTema.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
    // Es importante verificar isChecked para que la lógica solo corra una vez
    if (isChecked) {
        int modoSeleccionado;
        
        if (checkedId == R.id.btnTemaClaro) {
            modoSeleccionado = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (checkedId == R.id.btnTemaOscuro) {
            modoSeleccionado = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            modoSeleccionado = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }

        // Aplicar cambio si es diferente al actual
        if (prefs.getInt(KEY_THEME, -1) != modoSeleccionado) {
            prefs.edit().putInt(KEY_THEME, modoSeleccionado).apply();
            AppCompatDelegate.setDefaultNightMode(modoSeleccionado);
            Toast.makeText(this, getThemeToastMessage(modoSeleccionado), Toast.LENGTH_SHORT).show();
        }
    }
    });

        switchMaterialTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
    // 1. Verificar si el valor es realmente diferente al guardado
    // Esto evita que recreate() se dispare cuando el onCreate inicializa el switch
    boolean valorActual = prefs.getBoolean(KEY_MATERIAL_SWITCH, true);
    if (isChecked == valorActual) return; 

    // 2. Guardar preferencia
    prefs.edit().putBoolean(KEY_MATERIAL_SWITCH, isChecked).apply();

    // 3. Actualizar el icono visualmente
    switchMaterialTheme.setThumbIconResource(isChecked ? R.drawable.round_check : R.drawable.close_24px);

    Toast.makeText(this, isChecked ? "Colores dinámicos activados" : "Desactivados", Toast.LENGTH_SHORT).show();

    // 4. Recrear con un ligero delay para que se vea la animación del switch
    new android.os.Handler().postDelayed(this::recreate, 250);
        });

        novedades.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangelogActivity.class));
        });
        sobre.setOnClickListener(v -> {
            startActivity(new Intent(this, SobreActivity.class));
        });
        setup.setOnClickListener(v -> {
            startActivity(new Intent(this, InicioActivity.class));
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