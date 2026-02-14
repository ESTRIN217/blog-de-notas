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
  private MaterialButton btnSeleccionar, btnSiguiente;
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

  private final ActivityResultLauncher<Intent> launcherSelectorCarpeta =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
              Uri uri = result.getData().getData();
              if (uri != null) {
                try {
                  // 1. Otorgar permisos persistentes
                  final int takeFlags =
                      result.getData().getFlags()
                          & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                  getContentResolver().takePersistableUriPermission(uri, takeFlags);

                  // 2. Guardar en SharedPreferences
                  sharedPreferences.edit().putString("carpeta_uri", uri.toString()).apply();
                  carpetaUriString = uri.toString();

                  // 3. Feedback visual y activación de UI
                  btnSiguiente.setEnabled(true);

                  // Opcional: Mostrar el nombre de la carpeta seleccionada en un TextView
                  // DocumentFile folder = DocumentFile.fromTreeUri(this, uri);
                  // tvNombreCarpeta.setText("Carpeta: " + folder.getName());

                  Toast.makeText(this, "Acceso concedido correctamente", Toast.LENGTH_SHORT).show();

                } catch (SecurityException e) {
                  Log.e("SAF_ERROR", "Error al persistir el permiso: " + e.getMessage());
                  Toast.makeText(this, "Error: No se pudo mantener el acceso", Toast.LENGTH_LONG)
                      .show();
                }
              }
            } else {
              // El usuario canceló o hubo un error
              Toast.makeText(this, "Operación cancelada", Toast.LENGTH_SHORT).show();
            }
          });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    androidx.activity.EdgeToEdge.enable(this);
    super.onCreate(savedInstanceState);

    sharedPreferences = getSharedPreferences("MisNotasPrefs", MODE_PRIVATE);
    carpetaUriString = sharedPreferences.getString("carpeta_uri", null);

    // Si ya hay una carpeta configurada, saltamos directamente a MainActivity

    if (carpetaUriString != null) {
      // Verificar si el permiso sigue siendo válido antes de saltar
      Uri uri = Uri.parse(carpetaUriString);
      boolean tieneAcceso = false;
      for (UriPermission p : getContentResolver().getPersistedUriPermissions()) {
        if (p.getUri().equals(uri)) {
          tieneAcceso = true;
          break;
        }
      }

      if (tieneAcceso) {
        irAMain();
        return; // Salir para no cargar el layout de Inicio
      }
    }

    setContentView(R.layout.activity_inicio);

    btnSeleccionar =
        findViewById(R.id.btnSeleccionarCarpeta); // Asegúrate de poner este ID en tu XML
    btnSiguiente = findViewById(R.id.btnSiguiente); // El botón dentro del DockedToolbarLayout
    swNotificaciones = findViewById(R.id.swNotificaciones);
    swMicrofono = findViewById(R.id.swMicrofono);
    swCamara = findViewById(R.id.swCamara);

    btnSeleccionar.setOnClickListener(
        v -> {
          Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
          launcherSelectorCarpeta.launch(intent);
        });

    btnSiguiente.setEnabled(false); // Desactivado hasta que elijan carpeta
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
        
        // Pasamos la URI como un Extra
        intent.putExtra("extra_carpeta_uri", carpetaUriString);
        
        // Flags de limpieza para evitar el bucle
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        startActivity(intent);
        finish(); 
    } else {
        Toast.makeText(this, "Por favor, seleccione una carpeta primero", Toast.LENGTH_SHORT).show();
    }
}
}
