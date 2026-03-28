# Blueprint: Blog de notas

## 1. Descripción general

Este documento describe el diseño y las características de una aplicación moderna e intuitiva para tomar notas con Flutter. La aplicación proporciona una interfaz flexible y fácil de usar para crear, Gestiona y organiza notas, con funciones avanzadas de personalización y localización.

## 2. Características por versión

### 2.1. Versión 3.0.0

- **Temas e interfaz de usuario avanzados:**
  - **Modos de tema:** Compatibilidad total con los modos **claro y oscuro**, además de una configuración del **sistema** para sincronizar con el sistema operativo.
  - **Color dinámico:** En las versiones de Android compatibles (Material You), la combinación de colores de la aplicación se puede generar dinámicamente a partir del fondo de pantalla del usuario.
  - **Tipografía personalizada:** Integración con **Google Fonts** para una apariencia de texto pulida y uniforme.
  - **Persistencia de la configuración:** Todas las preferencias del usuario, incluidos el tema, el idioma y el orden de clasificación, se guardan entre sesiones de la aplicación mediante `shared_preferences`.
- **Internacionalización (i10n):**
  - **Soporte multilingüe:** La interfaz de usuario está totalmente localizada para **inglés** y **español**.
  - **Selección de idioma:** Los usuarios pueden cambiar manualmente el idioma de la aplicación desde la pantalla de configuración.
- **Editor de texto enriquecido mejorado (Flutter Quill):**
  - El editor personalizado anterior ha sido reemplazado por la potente biblioteca `flutter_quill`, que proporciona una experiencia de edición más robusta y con más funciones.
- **Conversión de texto a voz:**
  - **Accesibilidad:** Una función integrada de conversión de texto a voz puede leer las notas en voz alta.

### 2.2. Versión 2.0.0 (Características integradas y mejoradas en v3.0)

- **Mejoras en el editor:**
  - **Funcionalidad de deshacer/rehacer:** Deshacer y rehacer los cambios de texto y estilo.
  - **Personalización del fondo:** Cambia el color de fondo de una nota o establece una imagen de fondo desde la galería.
  - **Formato de texto:** Formato básico de texto, como tamaño de fuente, negrita y cursiva.
  - **Listas de verificación:** Crea listas de verificación interactivas dentro de las notas.
    
*Nota: Estas funciones formaban parte originalmente de un editor personalizado y han sido reemplazadas por la implementación de `flutter_quill` en la versión 3.0.*

### 2.3. Versión 1.0.0 (Características principales)

- **Pantalla principal y diseño:**
  - **Modos de doble visualización:** Cambia entre la vista de lista y la vista de cuadrícula.
  - **Clasificación personalizable:** Ordena las notas alfabéticamente, por fecha de modificación o utiliza un orden personalizado arrastrando y soltando.
  - **Búsqueda funcional:** Filtrado de notas en tiempo real por título o contenido.
- **Editor de notas:**
  - **Edición de texto enriquecido:** Una pantalla dedicada para editar notas.
  - **Ahorro automático:** Los cambios se guardan automáticamente cuando el usuario sale del editor.
  - **Manejo de notas vacías:** Las notas en blanco se descartan automáticamente..
- **Selección y acción masivas:**
  - **Modo de selección:** Mantén pulsada una nota para entrar en el modo de selección y elegir varios elementos.
  - **Barra de aplicaciones contextual:** Aparece en el modo de selección para permitir compartir o eliminar en bloque las notas seleccionadas.

### 2.4 Versión 4.0.0

- **Mejoras en el editor:**
  - **Soporte de imagen en nota**
  - **Soporte de audio en nota**
  - **Soporte de dibujo en nota**

- **Mas formatos para compartir:**
  - **PDF, Markdown, HTML, CSV, and JSON**

## 3. Calidad del código y control de versiones

### 3.1. Dependencias

- El proyecto utiliza `flutter_quill`, `provider`, `dynamic_color`, `google_fonts`, `shared_preferences`, `flutter_tts`, y más para respaldar su conjunto de funciones avanzadas.

### 3.2. Control de versiones

- **Versión actual:** `3.0.0+1`
