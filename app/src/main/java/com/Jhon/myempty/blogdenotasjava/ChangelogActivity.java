package com.Jhon.myempty.blogdenotasjava;

import android.os.Bundle;
import android.os.Build;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.appbar.MaterialToolbar;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.Jhon.myempty.blogdenotasjava.Cambio;
import java.util.List;
import java.util.ArrayList;

public class ChangelogActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);

        RecyclerView rv = findViewById(R.id.recyclerChangelog);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        List<Cambio> listaCambios = new ArrayList<>();
        
        // AQUÍ AGREGAS TUS VERSIONES
        listaCambios.add(new Cambio("1.11.0 General", "08 de febrero de 2026", "1. REVISIÓN VISUALES GENERALES.\n" + 
        "2. SE REVISO EL ICONO MONOCROMÁTICO.\n" +
        "3. SE MEJORO LA CARGA DE ARCHIVOS Y EL GUARDADO DE ESTOS.\n" +
        "4. AHORA SE PUEDE SELECCIONAR UNA IMAGEN DE FONDO.\n" +
        "5. SE AGREGO SELECCIÓN VISUAL A LAS NOTAS.\n" +
        "6. CORRECCIONES DE ERRORES.\n" +
        "7. SE AÑADIÓ UN MENÚ DESLIZANTE A LA PANTALLA PRINCIPAL.\n" +
        "8. SE AÑADIÓ UN BOTÓN PARA CAMBIAR EL ORDEN DE LA LISTA.\n" +
        "9. SE AÑADIÓ UNA PANTALLA SOBRE EN CONFIGURACIÓN.\n" +
        "10. SE AÑADIÓ UNA PANTALLA DE INICIÓ"));
        listaCambios.add(new Cambio("1.10.0 General", "30 de enero de 2026", "1. Mejoras visuales generales.\n" + 
        "2. Se agrupó botones cercanos en un material button group.\n" + 
        "3. Mejoras visuales y generales a dibujo activity.\n" + 
        "4. Se agrego la función texto a voz.\n" + 
        "5. Ahora se puede exportar a PDF para compartir"));
        listaCambios.add(new Cambio("1.9.0 Editor", "24 de enero de 2026", "1.Se a mejorado la UI dándole un aspecto más a material 3.\n" +
        "2.Se mejoro visualmente la pantalla de Configuracion.\n" +
        "3.se añadió la opción de añadir casillas de verificación\n" +
        "4. Se mejoro el icono de la app y se agrego la compatibilidad con icono monochrome\n" +
        "5. Se añadió el botón de minimizar al modo Flotante"));
        listaCambios.add(new Cambio("1.8.0 Editor", "15 de enero de 2026", "1. Sincronización Editor ↔ FloatingService\n" +
        "Corrección de Llaves: Estandarizamos el uso de 'uri_archivo' para que ambos componentes compartan la misma referencia.\n" +
        
        "Ciclo de Vida: Implementamos onNewIntent en EditorActivity. Esto permite que, si el editor ya está abierto, la nota se refresque automáticamente al regresar de la burbuja sin crear ventanas duplicadas.\n" +
        
        "Carga Automática: Ajustamos manejarIntent para que ejecute cargarNotaSAF() inmediatamente al recibir datos nuevos.\n" +
        
        "2. Interfaz Dinámica y Material 3\n" +
        "Colores Oficiales: Sustituimos los colores hexadecimales fijos por Atributos de Material 3 (como colorSurfaceContainer y colorPrimaryContainer). Esto permite que tu app soporte Modo Oscuro y colores dinámicos del sistema automáticamente.\n" +
        
        "Lógica de Contraste: Aseguramos que el método aplicarColorFondoDinamico sea el encargado de cambiar el color del fondo. Así, el texto cambiará entre blanco y negro dependiendo de qué tan claro u oscuro sea el color elegido.\n" +
        
        "Compatibilidad: Corregimos errores de compilación reemplazando atributos modernos (colorInverseOnSurface) por otros más compatibles (colorSurfaceInverse).\n" +
        
        "3. Ajustes de Layout y Teclado\n" +
        "Barra de Iconos Elevada: Configuramos el AndroidManifest.xml con windowSoftInputMode='adjustResize'.\n" +
        
        "Estructura XML: Organizamos el editor.xml para que el NestedScrollView use layout_weight='1'. Esto garantiza que los iconos de la paleta y estilo se mantengan siempre visibles, pegados justo encima del teclado cuando este aparece."));
        listaCambios.add(new Cambio("1.7.0 Editor", "14 de enero de 2026", "1. Evolución del Motor de Dibujo\n" +
        "Hemos migrado de un sistema simple de 'pintar y olvidar' a un Sistema Basado en Objetos (DibujoObjeto).\n" +
        "Independencia: Cada trazo ahora es un objeto con su propia Matrix, Path original y Paint.\n" +
        "Transformaciones No Destructivas: Al usar matrices, podemos rotar y escalar los dibujos sin que pierdan calidad ni se deforme el trazo original.\n" +
        "2. Implementación del Modo Selección\n" +
        "Se ha creado una lógica de interacción avanzada que reconoce 10 puntos de contacto distintos:\n" +
        "4 Esquinas: Para escalado proporcional.\n" +
        "1 Punto Superior: Para rotación libre.\n" +
        "1 Centro del Objeto: Para desplazamiento (traslación) por el lienzo.\n" +
        "Detección por Colisión: El sistema ahora detecta cuál es el último objeto que tocaste mediante el método detectarSeleccion.\n" +
        "3. Resolución de Errores Críticos\n" +
        "Corregimos los 10 errores de compilación que surgieron en DibujoActivity. Estos errores se debían a la falta de métodos públicos en la nueva versión de LienzoView. Restauramos y adaptamos:\n" +
        "setColor() y setGrosor() para el control de pinceles.\n" +
        "Sistema de Deshacer/Rehacer compatible con la nueva lista de objetos.\n" +
        "Método cargarFondo() para la edición de imágenes externas."));
        listaCambios.add(new Cambio("1.6.0 Editor", "12 de enero de 2026", "Mejoras en la UI.\n" + 
        "Mejoras en el menú de Añadir.\n" + 
        "Añadido el menú para seleccionar color / imagen de fondo.\n" + 
        "Añadido el menu para cambiar el tamaño y el estilo de letra.\n" + 
        "Ahora todas las actividades son EdgeToEdge.\n" +
        "Mejoradas las tarjetas de resumen ahora muestran 10 lineas.\n" + 
        "1. Evolución al Formato Enriquecido (HTML)\n" +
        "Guardado Inteligente: Cambiamos la lógica de guardado para que ahora convierta el texto con formato (negritas, cursivas, subrayados) a HTML mediante Html.toHtml() antes de escribir el archivo .txt.\n" +
        "Lectura con Estilo: Actualizamos cargarNotaSAF para interpretar esas etiquetas HTML al abrir la nota, devolviendo el formato visual al usuario mediante Html.fromHtml().\n" +
        
        "Resumen de Lista: Optimizamos el método obtenerResumenSAF para leer hasta 10 líneas, limpiando las etiquetas HTML para que en la lista principal solo se vea texto limpio y profesional.\n" +

        "2. Persistencia de Estilos y Configuración\n" +
        "Memoria de Color: Implementamos un sistema usando SharedPreferences para que cada nota 'recuerde' su propio color de fondo individualmente, vinculándolo al nombre del archivo.\n" +
        
        "Modo de Escritura Activo: Creamos un TextWatcher con banderas booleanas (isBoldActive, etc.) que aplican estilos automáticamente mientras el usuario escribe, permitiendo que la negrita o cursiva se mantenga 'encendida'.\n" +
        
        "3. Corrección de Arquitectura y Errores\n" +
        "Edge-to-Edge: Corregimos la implementación de la interfaz inmersiva. Se eliminó el código erróneo de la clase Application y se centralizó en las Activities (SettingsActivity, etc.) para que la app se vea correctamente detrás de las barras de sistema.\n" +
        
        "Limpieza de Código: Solucionamos errores de compilación por falta de imports (como StyleSpan, Typeface, Html) y eliminamos duplicidad de variables en SettingsActivity tras la limpieza del código antiguo.\n" +
        
        "4. Interfaz de Ajustes (Settings)\n" +
        "Sincronización: Refinamos el archivo settings.xml para que sea compatible con Material You (Colores Dinámicos) y configuramos los listeners para que el cambio de tema (Claro/Oscuro/Sistema) sea instantáneo mediante recreate()."));
        listaCambios.add(new Cambio("1.5.0 Editor", "10 de enero de 2026","🛠️ Principales Problemas Solucionados.\n" + 
        "•1. El Error de 'Variable no encontrada' Problema: El código no compilaba porque faltaban declarar variables globales (archivoActualSAF, listas de rutas) y faltaban imports (android.util.Log).\n" +
        "Solución: Se declararon las variables a nivel de clase y se añadieron los imports necesarios.\n" +
        "•2. La Lógica de 'Etiquetas de Texto' vs. 'Carpetas'\n" +
        "Problema: Originalmente intentabas guardar la ruta de la imagen escrita dentro del archivo de texto ([[FOTO:ruta...]]). Esto era frágil y sucio.\n" +
        "Solución: Cambiamos la arquitectura. Ahora, cada nota tiene una carpeta de recursos hermana.\n" +
        "Archivo: MiNota.txt\n" +
        "Carpeta: MiNota_resources/ (donde van las imágenes).\n" +
        "•3. El Problema del Padre Nulo (getParentFile())\n" +
        "Problema Crítico: Al usar DocumentFile.fromSingleUri, Android no permitía obtener la carpeta padre (getParentFile() devolvía null), por lo que no se podían guardar las fotos.\n" +
        "Solución: Modificamos la lógica para usar carpetaUriPadre (la raíz que el usuario eligió al principio) para localizar y crear la carpeta de recursos.\n" +
        "•4. Sincronización de Creación (Tu idea clave)\n" +
        "Mejora: Implementamos la 'Creación Simultánea'.\n" +
        "Resultado: Ahora, en el momento exacto en que se crea el archivo .txt, se crea inmediatamente la carpeta _resources. Esto evita errores si intentas guardar una foto milisegundos después de crear la nota.\n" +
        "•5. Corrección de Carga (Lectura)\n" +
        "Problema: Las imágenes se guardaban pero no aparecían al abrir la nota de nuevo.\n" +
        "Solución: Reescribimos cargarNotaSAF. Ahora lee el texto y luego escanea automáticamente la carpeta _resources para mostrar las imágenes en la parte inferior, sin necesidad de leer códigos extraños dentro del texto."));
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
        toolbar = findViewById(R.id.topAppBar);
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        // 1. Referenciar el TextView
        MaterialTextView txtVersion = findViewById(R.id.txtVersionActualInfo);
        try {
            // 2. Obtener la información del paquete
            String nombreVersion = getPackageManager()
            .getPackageInfo(getPackageName(), 0).versionName;
            // 3. Mostrarla en el TextView
            txtVersion.setText("Versión actual: " + nombreVersion);
        } catch (Exception e) {
            e.printStackTrace();
            txtVersion.setText("Versión: 1.0.0"); // Valor por defecto si algo falla
            }
    }
}