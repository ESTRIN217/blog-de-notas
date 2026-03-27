import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'theme_provider.dart';
import 'about_screen.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ajustes')),
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
                    const Padding(
                      padding: EdgeInsets.only(
                        top: 16.0,
                        left: 16.0,
                        right: 16.0,
                        bottom: 8.0,
                      ),
                      child: Text(
                        'Apariencia',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),

                  Card(
                    clipBehavior: Clip.hardEdge,
                    child: SwitchListTile(
                      title: const Text('Usar colores dinámicos'),
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
                        const ListTile(
                          leading: Icon(Icons.dark_mode),
                          title: Text('Modo oscuro'),
                        ),
                        Padding(
                          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                          child: SegmentedButton<ThemeMode>(
                            segments: const <ButtonSegment<ThemeMode>>[
                              ButtonSegment<ThemeMode>(
                                value: ThemeMode.system,
                                label: Text('Sistema'),
                                icon: Icon(Icons.brightness_auto),
                              ),
                              ButtonSegment<ThemeMode>(
                                value: ThemeMode.light,
                                label: Text('Apagado'),
                                icon: Icon(Icons.light_mode),
                              ),
                              ButtonSegment<ThemeMode>(
                                value: ThemeMode.dark,
                                label: Text('Encendido'),
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
                  const Padding(
                    padding: EdgeInsets.only(
                      top: 16.0,
                      left: 16.0,
                      right: 16.0,
                      bottom: 8.0,
                    ),
                    child: Text(
                      'Información',
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ),
                  Card(
                    clipBehavior: Clip.hardEdge,
                    child: ListTile(
                      leading: const Icon(Icons.info_outline_rounded),
                      title: const Text('Sobre'),
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
}
