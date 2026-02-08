package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class InicioActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private MaterialButton btnSeleccionar, btnSiguiente;
    private String carpetaUriString;
    private MaterialSwitch swNotificaciones, swMicrofono, swCamara;
private final ActivityResultLauncher<String> requestPermissionLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            // Aquí podrías resetear el switch si fuera necesario
        }
    });
    
    private final ActivityResultLauncher<Intent> launcherSelectorCarpeta = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Otorgar permisos persistentes para que la app no pierda el acceso al reiniciar
                        final int takeFlags = result.getData().getFlags() & 
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);

                        // Guardar en SharedPreferences
                        sharedPreferences.edit().putString("carpeta_uri", uri.toString()).apply();
                        carpetaUriString = uri.toString();
                        
                        btnSiguiente.setEnabled(true);
                        Toast.makeText(this, "Carpeta seleccionada correctamente", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sharedPreferences = getSharedPreferences("MisNotasPrefs", MODE_PRIVATE);
        carpetaUriString = sharedPreferences.getString("carpeta_uri", null);

        // Si ya hay una carpeta configurada, saltamos directamente a MainActivity
        if (carpetaUriString != null) {
            irAMain();
            return;
        }

        setContentView(R.layout.activity_inicio);

        btnSeleccionar = findViewById(R.id.btnSeleccionarCarpeta); // Asegúrate de poner este ID en tu XML
        btnSiguiente = findViewById(R.id.btnSiguiente); // El botón dentro del DockedToolbarLayout
        swNotificaciones = findViewById(R.id.swNotificaciones);
        swMicrofono = findViewById(R.id.swMicrofono);
        swCamara = findViewById(R.id.swCamara);

        btnSeleccionar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            launcherSelectorCarpeta.launch(intent);
        });

        btnSiguiente.setEnabled(false); // Desactivado hasta que elijan carpeta
        btnSiguiente.setOnClickListener(v -> irAMain());
        swNotificaciones.setOnCheckedChangeListener((v, isChecked) -> {
    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
    }
    swNotificaciones.setThumbIconResource(isChecked ? R.drawable.round_check : R.drawable.close_24px);
});

swMicrofono.setOnCheckedChangeListener((v, isChecked) -> {
    if (isChecked) {
        requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
    }
    swMicrofono.setThumbIconResource(isChecked ? R.drawable.round_check : R.drawable.close_24px);
});

swCamara.setOnCheckedChangeListener((v, isChecked) -> {
    if (isChecked) {
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
    }
    swCamara.setThumbIconResource(isChecked ? R.drawable.round_check : R.drawable.close_24px);
});
    }

    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Cerramos InicioActivity para que no puedan volver con el botón atrás
    }
}