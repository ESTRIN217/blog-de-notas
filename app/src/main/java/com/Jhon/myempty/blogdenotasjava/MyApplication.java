package com.Jhon.myempty.blogdenotasjava;

import android.app.Application;
import android.content.SharedPreferences; // <--- ESTA ES LA LÍNEA QUE FALTABA
import com.google.android.material.color.DynamicColors;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Cargamos las preferencias
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        
        // Si el usuario tiene activo el switch de Material You, se aplica a toda la app
        if (prefs.getBoolean("material_theme_activado", false)) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}