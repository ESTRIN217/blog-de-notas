// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get helloWorld => '¡Hola Mundo!';

  @override
  String get flutterNotes => 'BLOG DE NOTAS';

  @override
  String get search => 'Buscar...';

  @override
  String get toggleView => 'Cambiar Vista';

  @override
  String get sort => 'Ordenar';

  @override
  String get menu => 'Menú';

  @override
  String get home => 'Inicio';

  @override
  String get settings => 'Configuración';

  @override
  String get addItem => 'Añadir nota';

  @override
  String selected(Object count) {
    return '$count seleccionados';
  }

  @override
  String get share => 'Compartir';

  @override
  String get delete => 'Eliminar';

  @override
  String get sortAlphabetically => 'Ordenar Alfabéticamente';

  @override
  String get sortByDate => 'Ordenar por Fecha de Modificación';

  @override
  String get customSort => 'Orden Personalizado';

  @override
  String get myNotes => 'Mis Notas';

  @override
  String get imageFromGallery => 'Imagen de la galería';

  @override
  String get title => 'Título';

  @override
  String get useDynamicColors => 'Usar Colores Dinámicos';

  @override
  String get themeMode => 'Modo Oscuro';

  @override
  String get system => 'Sistema';

  @override
  String get light => 'Apagado';

  @override
  String get dark => 'Encendido';

  @override
  String get apariencia => 'Apariencia';

  @override
  String get idioma => 'Idioma';

  @override
  String get informacion => 'Información';

  @override
  String get sobre => 'Sobre la aplicación';

  @override
  String get desarrolador => 'Desarrollado por';

  @override
  String get enlaces => 'Enlaces utiles';

  @override
  String get repositorio => 'Ver repositorio';
}
