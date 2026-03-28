import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'theme_provider.dart';
import 'about_screen.dart';
import 'l10n/app_localizations.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context)!.settings)),
      body: DynamicColorBuilder(
        builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
          final isDynamicColorSupported =
              lightDynamic != null && darkDynamic != null;

          return Consumer<ThemeProvider>(
            builder: (context, themeProvider, child) {
              return ListView(
                padding: const EdgeInsets.all(8),
                children: [
                  if (isDynamicColorSupported)
                    Padding(
                      padding: EdgeInsets.only(
                        top: 16.0,
                        left: 16.0,
                        right: 16.0,
                        bottom: 8.0,
                      ),
                      child: Text(
                        AppLocalizations.of(context)!.apariencia,
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),

                  Card(
                    clipBehavior: Clip.hardEdge,
                    child: SwitchListTile(
                      title: Text(
                        AppLocalizations.of(context)!.useDynamicColors,
                      ),
                      secondary: const Icon(Icons.palette),
                      value: themeProvider.useDynamicColors,
                      onChanged: (value) {
                        themeProvider.setUseDynamicColors(value);
                      },
                      thumbIcon: WidgetStateProperty.resolveWith<Icon?>((
                        Set<WidgetState> states,
                      ) {
                        // Si el switch ESTÁ seleccionado (ON)
                        if (states.contains(WidgetState.selected)) {
                          return const Icon(Icons.check);
                        }
                        // Si NO está seleccionado (OFF)
                        return const Icon(Icons.close);
                      }),
                    ),
                  ),
                  Card(
                    child: Column(
                      children: [
                        ListTile(
                          leading: Icon(Icons.dark_mode),
                          title: Text(AppLocalizations.of(context)!.themeMode),
                        ),
                        Padding(
                          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                          child: SegmentedButton<ThemeMode>(
                            segments: <ButtonSegment<ThemeMode>>[
                              ButtonSegment<ThemeMode>(
                                value: ThemeMode.system,
                                label: Text(
                                  AppLocalizations.of(context)!.system,
                                ),
                                icon: Icon(Icons.brightness_auto),
                              ),
                              ButtonSegment<ThemeMode>(
                                value: ThemeMode.light,
                                label: Text(
                                  AppLocalizations.of(context)!.light,
                                ),
                                icon: Icon(Icons.light_mode),
                              ),
                              ButtonSegment<ThemeMode>(
                                value: ThemeMode.dark,
                                label: Text(AppLocalizations.of(context)!.dark),
                                icon: Icon(Icons.dark_mode),
                              ),
                            ],
                            selected: {themeProvider.themeMode},
                            onSelectionChanged: (newSelection) {
                              themeProvider.setThemeMode(newSelection.first);
                            },
                          ),
                        ),
                      ],
                    ),
                  ),
                  Padding(
                    padding: EdgeInsets.only(
                      top: 16.0,
                      left: 16.0,
                      right: 16.0,
                      bottom: 8.0,
                    ),
                    child: Text(
                      AppLocalizations.of(context)!.idioma,
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ),
                  // Dentro del ListView en SettingsScreen [cite: 3]
                  Card(
                    clipBehavior: Clip.hardEdge,
                    child: ListTile(
                      leading: const Icon(Icons.language),
                      // Mostramos el idioma actual basado en el locale del provider
                      title: Text(
                        themeProvider.locale.languageCode == 'es'
                            ? 'Español'
                            : 'English',
                      ),
                      subtitle: Text(
                        AppLocalizations.of(context)!.idioma,
                      ), // [cite: 23]
                      onTap: () {
                        _showLanguageDialog(context, themeProvider);
                      },
                    ),
                  ),
                  Padding(
                    padding: EdgeInsets.only(
                      top: 16.0,
                      left: 16.0,
                      right: 16.0,
                      bottom: 8.0,
                    ),
                    child: Text(
                      AppLocalizations.of(context)!.informacion,
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ),
                  Card(
                    clipBehavior: Clip.hardEdge,
                    child: ListTile(
                      leading: const Icon(Icons.info_outline_rounded),
                      title: Text(AppLocalizations.of(context)!.sobre),
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => const AboutScreen(),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              );
            },
          );
        },
      ),
    );
  }

  void _showLanguageDialog(BuildContext context, ThemeProvider themeProvider) {
    showModalBottomSheet(
      context: context,
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: const Text('🇪🇸'),
                title: const Text('Español'),
                onTap: () {
                  themeProvider.setLocale(const Locale('es'));
                  Navigator.pop(context);
                },
              ),
              ListTile(
                leading: const Text('🇺🇸'),
                title: const Text('English'),
                onTap: () {
                  themeProvider.setLocale(const Locale('en'));
                  Navigator.pop(context);
                },
              ),
            ],
          ),
        );
      },
    );
  }
}
