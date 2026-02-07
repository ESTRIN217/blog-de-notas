package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.net.Uri;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;

public class SobreActivity extends AppCompatActivity {

    private MaterialButton btnAtras, github;
    private TextView version;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
    
    // Habilitar EdgeToEdge también se recomienda antes de super o justo después
    androidx.activity.EdgeToEdge.enable(this);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sobre); 

    // 2. Inicializar las vistas primero - AHORA las variables ya no son null
    inicializarVistas(); 

    configurarListeners();
    }

    private void inicializarVistas() {
        btnAtras = findViewById(R.id.btnAtras);
        version = findViewById(R.id.version);
        github = findViewById(R.id.github);
    }

    private void configurarListeners() {
        btnAtras.setOnClickListener(v -> finish());
        try {
            // 2. Obtener la información del paquete
            String nombreVersion = getPackageManager()
            .getPackageInfo(getPackageName(), 0).versionName;
            // 3. Mostrarla en el TextView
            version.setText(nombreVersion);
        } catch (Exception e) {
            e.printStackTrace();
            version.setText("1.0.0"); // Valor por defecto si algo falla
            }
        github.setOnClickListener(v -> {
    try {
        // Use 'this' instead of 'context' as 'context' is not defined here
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ESTRIN217"));
        this.startActivity(intent);
    } catch (Exception e) {
        // Use 'this' instead of 'context' for the Toast
        Toast.makeText(this, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show();
    }
        });

    }
}