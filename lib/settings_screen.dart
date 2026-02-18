import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'theme_provider.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Ajustes'),
      ),
      body: DynamicColorBuilder(
        builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
          final isDynamicColorSupported = lightDynamic != null && darkDynamic != null;

          return Consumer<ThemeProvider>(
            builder: (context, themeProvider, child) {
              return ListView(
                children: [
                  if (isDynamicColorSupported)
                    SwitchListTile(
                      title: const Text('Usar colores dinámicos'),
                      value: themeProvider.useDynamicColors,
                      onChanged: (value) {
                        themeProvider.setUseDynamicColors(value);
                      },
                    ),
                  if (isDynamicColorSupported) const Divider(),
                  const ListTile(
                    title: Text('Modo de tema'),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16.0),
                    child: SegmentedButton<ThemeMode>(
                      segments: const <ButtonSegment<ThemeMode>>[
                        ButtonSegment<ThemeMode>(
                          value: ThemeMode.system,
                          label: Text('Sistema'),
                          icon: Icon(Icons.brightness_auto),
                        ),
                        ButtonSegment<ThemeMode>(
                          value: ThemeMode.light,
                          label: Text('Claro'),
                          icon: Icon(Icons.light_mode),
                        ),
                        ButtonSegment<ThemeMode>(
                          value: ThemeMode.dark,
                          label: Text('Oscuro'),
                          icon: Icon(Icons.dark_mode),
                        ),
                      ],
                      selected: <ThemeMode>{themeProvider.themeMode},
                      onSelectionChanged: (Set<ThemeMode> newSelection) {
                        themeProvider.setThemeMode(newSelection.first);
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
}
