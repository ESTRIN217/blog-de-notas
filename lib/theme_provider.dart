import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ThemeProvider with ChangeNotifier {
  bool _useDynamicColors = true;
  ThemeMode _themeMode = ThemeMode.system;

  bool get useDynamicColors => _useDynamicColors;
  ThemeMode get themeMode => _themeMode;

  void setUseDynamicColors(bool value) {
    _useDynamicColors = value;
    notifyListeners();
  }

  void setThemeMode(ThemeMode mode) {
    _themeMode = mode;
    notifyListeners();
  }

  Locale _locale = const Locale('es'); // Idioma por defecto
  Locale get locale => _locale;

  // Constructor: Al crear el Provider, cargamos las preferencias guardadas
  ThemeProvider() {
    _loadPreferences();
  }

  // Función privada para cargar el idioma al iniciar
  Future<void> _loadPreferences() async {
    final prefs = await SharedPreferences.getInstance();
    final String? languageCode = prefs.getString('language_code');
    
    if (languageCode != null) {
      _locale = Locale(languageCode);
      notifyListeners();
    }
  }

  // Actualiza el idioma y lo guarda en memoria
  Future<void> setLocale(Locale newLocale) async {
    if (_locale != newLocale) {
      _locale = newLocale;
      notifyListeners(); // Actualiza la UI de inmediato
      
      // Guardamos la preferencia en segundo plano
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('language_code', newLocale.languageCode);
    }
  }
}
