package com.Jhon.myempty.blogdenotasjava;

import android.os.Bundle;
import android.os.Build;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import java.util.List;
import java.util.ArrayList;

public class ChangelogActivity extends AppCompatActivity {

    private ImageView btnAtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);

        RecyclerView rv = findViewById(R.id.recyclerChangelog);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        List<Cambio> listaCambios = new ArrayList<>();
        
        // AQUÍ AGREGAS TUS VERSIONES
        listaCambios.add(new Cambio("1.4.0 Editor", "09 de enero de 2026", "•1. El Editor Inteligente (Visualización Real) Problema: Las imágenes se guardaban como texto [[FOTO: ...]] y no se veían en la nota. Solución: Implementamos un sistema de Spannables (ImageSpan). Ahora, el editor escanea el texto y reemplaza esas etiquetas por el dibujo real. Mejora: Las fotos y dibujos ahora se ven dentro del cuerpo del texto, justo donde los insertaste, no solo en un contenedor aparte.\n" +
        "•2. Interfaz Estilo 'Google Keep' Barra de Herramientas: Reemplazamos los botones clásicos por una barra inferior moderna con 5 iconos: Selección, Bolígrafo, Marcador, Borrador y Regla. Selector 'Bottom Sheet': Creamos esa ventana elegante que sube desde abajo para elegir el color y el grosor del pincel mediante un deslizador (Slider) y círculos de colores. Guardado Moderno: Movimos la función de guardar a un icono de 'Check' (Hecho) en la barra superior para limpiar el diseño de la pantalla.\n" +
        "•3. Funcionalidad del Lienzo (LienzoView) Deshacer y Rehacer: Implementamos un sistema de 'pilas' que recuerda cada trazo de forma independiente. Ya puedes corregir errores paso a paso. Modos Dinámicos: El lienzo ahora distingue entre el Bolígrafo (trazo sólido) y el Borrador (trazo grueso que limpia el lienzo). Corrección de Compresión: Cambiamos el formato de guardado de .jpg a .png para que los dibujos no pierdan calidad ni se vean borrosos.\n" +
        "•4. Correcciones Técnicas (Bug Fixes) Error de Compilación: Solucionamos el fallo de setColorFilter asegurando que el código reconozca las vistas como ImageView. Error de Recursos (XML): Corregimos el crash de ComplexColor cambiando las referencias de atributos de color de @attr a ?attr. Estabilidad: Añadimos validaciones para que las imágenes se escalen correctamente al ancho de la pantalla, evitando que la aplicación se cierre por falta de memoria."));
        listaCambios.add(new Cambio("1.3.0 Editor", "08 de enero de 2026", "•🎭 Nuevo Menú de Inserción: Se sustituyó el menú clásico por un BottomSheetDialog moderno y ergonómico, facilitando el acceso a todas las herramientas multimedia desde la parte inferior.\n" +
        "• 🎙️ Grabadora de Voz Profesional: Interfaz dedicada con cronómetro en tiempo real, Sistema de grabación mediante MediaRecorder, Reproductor integrado en la nota con barra de progreso, botón Play/Pause y opción de eliminar.\n" +
        "• 📸 Integración de Cámara: Implementación de FileProvider para captura segura de imágenes. Visualización de fotos mediante tarjetas (Cards) con bordes redondeados dentro del editor.\n" +
        "• 🎨 Lienzo de Dibujo: Creación de una vista personalizada (LienzoView) para bocetos y notas a mano alzada. Función para exportar y guardar los dibujos como imágenes JPG adjuntas.\n" +
        "• 💾 Sistema de Persistencia Multimedia: Desarrollo de un sistema de etiquetas ([[AUDIO: ...]] y [[FOTO: ...]]) que permite que los archivos adjuntos se guarden dentro del archivo .txt y se recarguen automáticamente al abrir la nota.\n" +
        "• 🛠️ Estabilidad y Código: Migración a StringBuilder para un manejo de memoria más eficiente al guardar archivos grandes. Corrección de errores de compilación relacionados con importaciones de IOException y gestión de rutas."));
        listaCambios.add(new Cambio("v1.2.0", "07/01/2026", 
        "• Autoguardado inteligente al escribir y al salir.\n" +
        "• Historial de Deshacer/Rehacer optimizado (50 pasos).\n" +
        "• Función para insertar fecha y hora en el cursor.\n" +
        "• Cambio dinámico de vista (Lista/Cuadrícula).\n" +
        "• Fondo con colores dinámicos (estilo Google Keep).\n" +
        "• Corrección de errores críticos en IDs y diseño."));
        listaCambios.add(new Cambio("v1.1.1", "06/01/2026", "• Corrección de errores.\n" +
            "• mejoras en la UI."));
        listaCambios.add(new Cambio("v1.1.0", "06/01/2026", 
            "• Se agregó el botón de cambio de vista (Lista/Cuadrícula).\n" +
            "• Se añadió la función de inserción rápida de fecha.\n" +
            "• Mejoras en el diseño del editor."));
            
        listaCambios.add(new Cambio("v1.0.5", "02/01/2026",
            "• Nuevo sistema de colores dinámicos.\n" +
            "• Corrección de cierre inesperado en el modo flotante."));

        listaCambios.add(new Cambio("v1.0.0", "01/01/2026", 
            "• Lanzamiento inicial de My Notes.\n" +
            "• Soporte para notas de texto y modo flotante."));

        // Usas un adaptador sencillo (puedes crear uno rápido)
        ChangelogAdapter adaptador = new ChangelogAdapter(listaCambios);
        rv.setAdapter(adaptador);
        btnAtras = findViewById(R.id.btnAtrasSettings);
        
        btnAtras.setOnClickListener(v -> {
            finish();
        });
    }
}