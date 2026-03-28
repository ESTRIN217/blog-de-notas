import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:package_info_plus/package_info_plus.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  void _launchURL() async {
    const url = 'https://github.com/ESTRIN217/Blog-de-notas';
    if (!await launchUrl(Uri.parse(url))) {
      throw 'Could not launch $url';
    }
  }

  void _launchGITHUB() async {
    const url = 'https://github.com/ESTRIN217';
    if (!await launchUrl(Uri.parse(url))) {
      throw 'Could not launch $url';
    }
  }

  void _launchLicense() async {
    const url =
        'https://github.com/ESTRIN217/Blog-de-notas/blob/master/LICENSE';
    if (!await launchUrl(Uri.parse(url))) {
      throw 'Could not launch $url';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sobre')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Card.outlined(
                child: SizedBox(
                  width:
                      double.infinity, // <--- Esto hace que ocupe todo el ancho
                  child: Padding(
                    padding: const EdgeInsets.all(
                      20.0,
                    ), // Padding interno para la Card
                    child: Column(
                      mainAxisSize:
                          MainAxisSize.min, // Ajusta la altura al contenido
                      children: [
                        Image.asset(
                          'assets/icon/notas.png',
                          width: 100,
                          height: 100,
                        ),
                        const SizedBox(height: 20),
                        Text(
                          'BLOG DE NOTAS',
                          style: Theme.of(context).textTheme.headlineSmall
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(
                          height: 4,
                        ), // Pequeño espacio entre textos
                        FutureBuilder<PackageInfo>(
                          future: PackageInfo.fromPlatform(),
                          builder: (context, snapshot) {
                            if (snapshot.hasData) {
                              final version = snapshot.data!.version;
                              final buildNumber = snapshot.data!.buildNumber;
                              return Text(
                                '$version ($buildNumber) • UNIVERSAL',
                                style: Theme.of(context).textTheme.bodySmall
                                    ?.copyWith(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 14,
                                    ),
                              );
                            } else {
                              // Mientras carga la info, ponemos un texto genérico o vacío
                              return const Text('Cargando versión...');
                            }
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              ),

              SizedBox(height: 20),

              const LinearProgressIndicator(year2023: false),

              const SizedBox(height: 20),
              const Text(
                'Una aplicación de notas simple y elegante para capturar tus ideas.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 16),
              ),
              const SizedBox(height: 40),
              const Text(
                'Desarrollado por',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
              ),
              SizedBox(height: 10),
              CircleAvatar(
                radius: 100,
                backgroundImage: AssetImage('assets/icon/perfil.png'),
              ),

              const SizedBox(height: 10),
              const Text(
                'ESTRIN217',
                style: TextStyle(fontSize: 30, fontWeight: FontWeight.bold),
              ),
              Card.outlined(
                clipBehavior: Clip
                    .hardEdge, // Asegura que el efecto splash siga el redondeo del Card
                child: ListTile(
                  // Aplica el padding interno directamente al contenido del tile
                  contentPadding: const EdgeInsets.all(10.0),
                  leading: const FaIcon(FontAwesomeIcons.github),
                  title: const Text('GitHub'),
                  onTap: () => _launchGITHUB(),
                ),
              ),
              const SizedBox(height: 20),

              const Padding(
                padding: EdgeInsets.only(
                  top: 16.0,
                  left: 16.0,
                  right: 16.0,
                  bottom: 8.0,
                ),
                child: Text(
                  'Enlaces utiles',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
              Card.outlined(
                clipBehavior: Clip.hardEdge,
                child: Column(
                  children: [
                    ListTile(
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16.0,
                        vertical: 4.0,
                      ),
                      leading: const FaIcon(FontAwesomeIcons.github),
                      title: const Text('Ver repositorio'),
                      onTap: _launchURL,
                    ),
                    const Divider(
                      height: 1,
                      indent: 56,
                    ), // Una línea sutil para separar
                    ListTile(
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16.0,
                        vertical: 4.0,
                      ),
                      leading: const Icon(
                        Icons.description_outlined,
                      ), // Ícono para la licencia
                      title: const Text('MIT LICENCIA'),
                      onTap: _launchLicense,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
