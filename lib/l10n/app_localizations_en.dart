// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get helloWorld => 'Hello World!';

  @override
  String get flutterNotes => 'NOTEBOOK';

  @override
  String get search => 'Search...';

  @override
  String get toggleView => 'Toggle View';

  @override
  String get sort => 'Sort';

  @override
  String get menu => 'Menu';

  @override
  String get home => 'Home';

  @override
  String get settings => 'Settings';

  @override
  String get addItem => 'Add Note';

  @override
  String selected(Object count) {
    return '$count selected';
  }

  @override
  String get share => 'Share';

  @override
  String get delete => 'Delete';

  @override
  String get sortAlphabetically => 'Sort Alphabetically';

  @override
  String get sortByDate => 'Sort by Modification Date';

  @override
  String get customSort => 'Custom Sort';

  @override
  String get myNotes => 'My Notes';

  @override
  String get imageFromGallery => 'Image from gallery';

  @override
  String get title => 'Title';

  @override
  String get useDynamicColors => 'Use Dynamic Colors';

  @override
  String get themeMode => 'Mode dark';

  @override
  String get system => 'System';

  @override
  String get light => 'Off';

  @override
  String get dark => 'On';

  @override
  String get apariencia => 'Appearance';

  @override
  String get idioma => 'Language';

  @override
  String get informacion => 'Information';

  @override
  String get sobre => 'About';

  @override
  String get desarrolador => 'Developed by';

  @override
  String get enlaces => 'Useful links';

  @override
  String get repositorio => 'View repository';
}
