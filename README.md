# Flutter Notes

Una aplicación de notas simple e intuitiva creada con Flutter. Esta aplicación permite a los usuarios crear, editar y organizar notas con una interfaz moderna y fácil de usar, con funciones avanzadas de personalización y localización.

## Características

### v3.0

- **Temas Avanzados y UI:**
  - **Modos de Tema:** Soporte completo para los modos **Claro y Oscuro**, además de una configuración del **Sistema** para sincronizarse con el sistema operativo.
    - **Color Dinámico:** El esquema de colores de la aplicación se puede generar dinámicamente a partir del fondo de pantalla del usuario en versiones de Android compatibles (Material You).
    - **Tipografía Personalizada:** Integración con **Google Fonts** para una apariencia de texto pulida y consistente.
    - **Persistencia de Configuraciones:** Todas las preferencias del usuario, incluido el tema, el idioma y el orden de clasificación, se guardan entre sesiones de la aplicación utilizando `shared_preferences`.
- **Internacionalización (i10n):**
  - **Soporte Multilingüe:** La interfaz de usuario está completamente localizada para **Inglés** y **Español**.
    - **Selección de Idioma:** Los usuarios pueden cambiar manualmente el idioma de la aplicación desde la nueva pantalla de configuraciones.
- **Editor de Texto Enriquecido Mejorado (Flutter Quill):**
  - El editor personalizado anterior ha sido reemplazado por la potente biblioteca **`flutter_quill`**, proporcionando una experiencia de edición más robusta y rica en funciones.
- **Texto a Voz:**
  - **Accesibilidad:** Una función integrada de texto a voz puede leer las notas en voz alta.

### v2.0

- **Mejoras en el Editor (Integradas en v3.0):**
  - **Funcionalidad de Deshacer/Rehacer:** Deshacer y rehacer cambios en el texto y el estilo.
    - **Personalización del Fondo:** Cambiar el color de fondo o establecer una imagen de la galería como fondo de la nota.
    - **Formato de Texto y Listas de Verificación:** Ajustar el tamaño de la fuente, aplicar estilos de negrita e cursiva y crear listas de verificación.

### v1.0

- **Pantalla Principal y Diseño:**
  - **Vistas Dobles:** Cambiar entre una vista de lista compacta y una vista de cuadrícula más visual.
    - **Clasificación Personalizable:** Ordenar las notas alfabéticamente, por fecha de modificación o en un orden personalizado.
    - **Búsqueda Funcional:** Filtrar las notas por título o contenido en tiempo real.

## Instalación

1. Asegúrate de tener Flutter instalado. Para obtener instrucciones, consulta la [documentación de Flutter](https://flutter.dev/docs/get-started/install).
2. Clona el repositorio:

   ```sh
   git clone https://github.com/ESTRIN217/Blog-de-notas.git
   ```

3. Navega al directorio del proyecto:

   ```sh
   cd flutter-notes
   ```

4. Instala las dependencias:

   ```sh
   flutter pub get
   ```

## Uso

1. Ejecuta la aplicación:

   ```sh
   flutter run
   ```

2. ¡Comienza a tomar notas!

## Contribuciones

¡Las contribuciones son bienvenidas! Si tienes alguna idea para una nueva característica o has encontrado un error, abre un [issue](https://github.com/ESTRIN217/flutter-notes/issues) o envía un [pull request](https://github.com/ESTRIN217/flutter-notes/pulls).

## Licencia

Este proyecto está licenciado bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para obtener más detalles.
