package com.Jhon.myempty.blogdenotasjava;

import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.color.DynamicColors;

public class InicioActivity extends AppCompatActivity {

  private SharedPreferences sharedPreferences;
  private MaterialButton btnSiguiente;
  private String carpetaUriString;
  private MaterialSwitch swNotificaciones, swMicrofono, swCamara;
  private final ActivityResultLauncher<String> requestPermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          isGranted -> {
            if (isGranted) {
              Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
              Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
              // Aquí podrías resetear el switch si fuera necesario
            }
          });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    androidx.activity.EdgeToEdge.enable(this);
    super.onCreate(savedInstanceState);

    sharedPreferences = getSharedPreferences("MisNotasPrefs", MODE_PRIVATE);

    setContentView(R.layout.activity_inicio);

    btnSiguiente = findViewById(R.id.btnSiguiente); // El botón dentro del DockedToolbarLayout
    swNotificaciones = findViewById(R.id.swNotificaciones);
    swMicrofono = findViewById(R.id.swMicrofono);
    swCamara = findViewById(R.id.swCamara);

    btnSiguiente.setOnClickListener(v -> irAMain());
    swNotificaciones.setOnCheckedChangeListener(
        (v, isChecked) -> {
          if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
          }
          swNotificaciones.setThumbIconResource(
              isChecked ? R.drawable.round_check : R.drawable.close_24px);
        });

    swMicrofono.setOnCheckedChangeListener(
        (v, isChecked) -> {
          if (isChecked) {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
          }
          swMicrofono.setThumbIconResource(
              isChecked ? R.drawable.round_check : R.drawable.close_24px);
        });

    swCamara.setOnCheckedChangeListener(
        (v, isChecked) -> {
          if (isChecked) {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
          }
          swCamara.setThumbIconResource(isChecked ? R.drawable.round_check : R.drawable.close_24px);
        });
  }

  private void irAMain() {
    if (carpetaUriString != null) {
        Intent intent = new Intent(InicioActivity.this, MainActivity.class);
        
        // Flags de limpieza para evitar el bucle
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        startActivity(intent);
        finish(); 
    } else {
    }
}
}
