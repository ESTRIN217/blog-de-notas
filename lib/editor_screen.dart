import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_quill/flutter_quill.dart' as quill;
import 'package:image_picker/image_picker.dart';
import 'package:share_plus/share_plus.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'list_item.dart';

class EditorScreen extends StatefulWidget {
  final ListItem item;

  const EditorScreen({super.key, required this.item});

  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> {
  late TextEditingController _titleController;
  late quill.QuillController _contentController;
  late FlutterTts _flutterTts;

  int? _backgroundColorValue;
  String? _backgroundImagePath;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.item.title);
    _contentController = quill.QuillController(
      document: widget.item.document,
      selection: const TextSelection.collapsed(offset: 0),
    );
    _flutterTts = FlutterTts();

    _backgroundColorValue = widget.item.backgroundColor;
    _backgroundImagePath = widget.item.backgroundImagePath;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    _flutterTts.stop();
    super.dispose();
  }

  void _saveAndExit() {
    if (!mounted) return;
    final summaryJson = jsonEncode(_contentController.document.toDelta().toJson());

    final updatedItem = ListItem(
      id: widget.item.id,
      title: _titleController.text,
      summary: summaryJson,
      lastModified: DateTime.now(),
      backgroundColor: _backgroundColorValue,
      backgroundImagePath: _backgroundImagePath,
    );
    Navigator.pop(context, updatedItem);
  }

  void _shareItem() {
    final title = _titleController.text;
    final summary = _contentController.document.toPlainText();
    SharePlus.instance.share(
      ShareParams(
        text: '$title\n\n$summary',
        subject: title,
      ),
    );
  }

  void _deleteItem() {
    if (!mounted) return;
    Navigator.pop(context, "DELETE");
  }

  Future<void> _speak() async {
    final title = _titleController.text;
    final content = _contentController.document.toPlainText();
    if (title.isNotEmpty) {
      await _flutterTts.speak(title);
    }
    if (content.isNotEmpty) {
      await _flutterTts.speak(content);
    }
  }

  void _showEditorMenu() {
    showModalBottomSheet(
      context: context,
      builder: (ctx) {
        return Wrap(children: <Widget>[
          ListTile(
              leading: const Icon(Icons.share),
              title: const Text('Compartir'),
              onTap: () {
                Navigator.pop(ctx);
                _shareItem();
              }),
          ListTile(
              leading: const Icon(Icons.delete),
              title: const Text('Eliminar'),
              onTap: () {
                Navigator.pop(ctx);
                _deleteItem();
              }),
        ]);
      },
    );
  }

  void _showBackgroundSheet() {
    final colors = [
      null, // Default
      Colors.blueGrey[100]!.toARGB32(),
      Colors.amber[200]!.toARGB32(),
      Colors.deepOrange[200]!.toARGB32(),
      Colors.lightGreen[200]!.toARGB32(),
      Colors.teal[100]!.toARGB32(),
      Colors.purple[100]!.toARGB32(),
    ];

    showModalBottomSheet(
        context: context,
        builder: (ctx) {
          return Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                height: 80,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  itemCount: colors.length,
                  itemBuilder: (context, index) {
                    final colorValue = colors[index];
                    final isSelected = _backgroundColorValue == colorValue;

                    return GestureDetector(
                      onTap: () {
                        _changeBackgroundColor(colorValue);
                        Navigator.pop(ctx);
                      },
                      child: Container(
                        width: 60,
                        height: 60,
                        margin: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: colorValue != null
                              ? Color(colorValue)
                              : Theme.of(context).scaffoldBackgroundColor,
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: isSelected ? Colors.blue : Colors.grey,
                            width: isSelected ? 3 : 1,
                          ),
                        ),
                        child: colorValue == null
                            ? const Icon(Icons.format_color_reset)
                            : null,
                      ),
                    );
                  },
                ),
              ),
              const Divider(),
              ListTile(
                leading: const Icon(Icons.photo_library_outlined),
                title: const Text('Imagen de la galería'),
                onTap: () {
                  _pickImage();
                  Navigator.pop(ctx);
                },
              ),
            ],
          );
        });
  }

  void _showTextTools() {
    showModalBottomSheet(
      context: context,
      builder: (ctx) {
        return quill.QuillSimpleToolbar(
          controller: _contentController,
        );
      },
    );
  }

  void _changeBackgroundColor(int? colorValue) {
    setState(() {
      _backgroundColorValue = colorValue;
      _backgroundImagePath = null;
    });
  }

  Future<void> _pickImage() async {
    final picker = ImagePicker();
    final pickedFile = await picker.pickImage(source: ImageSource.gallery);

    if (pickedFile != null) {
      setState(() {
        _backgroundImagePath = pickedFile.path;
        _backgroundColorValue = null;
      });
    }
  }

  bool _isColorDark(int? colorValue) {
    if (colorValue == null) return Theme.of(context).brightness == Brightness.dark;
    return Color(colorValue).computeLuminance() < 0.5;
  }

  @override
  Widget build(BuildContext context) {
    final isDark = _isColorDark(_backgroundColorValue);
    final textColor = isDark ? Colors.white : Colors.black;
    final hintColor = isDark ? Colors.white70 : Colors.black54;
    final appBarColor =
        _backgroundColorValue != null ? Color(_backgroundColorValue!) : null;

    BoxDecoration? backgroundDecoration;
    if (_backgroundImagePath != null) {
      backgroundDecoration = BoxDecoration(
        image: DecorationImage(
          image: FileImage(File(_backgroundImagePath!)),
          fit: BoxFit.cover,
        ),
      );
    } else if (_backgroundColorValue != null) {
      backgroundDecoration = BoxDecoration(color: Color(_backgroundColorValue!));
    }

    return PopScope<Object?>(
      canPop: false,
      onPopInvokedWithResult: (bool didPop, Object? result) async {
        if (didPop) return;
        _saveAndExit();
      },
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
              icon: Icon(Icons.arrow_back, color: textColor),
              onPressed: _saveAndExit),
          backgroundColor: appBarColor,
          elevation: _backgroundColorValue != null ? 0 : null,
          title: null,
          actions: [
            IconButton(
                icon: Icon(Icons.more_vert, color: textColor),
                onPressed: _showEditorMenu),
          ],
        ),
        body: Container(
          decoration: backgroundDecoration,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                child: TextField(
                  controller: _titleController,
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: textColor,
                  ),
                  decoration: InputDecoration(
                    border: InputBorder.none,
                    hintText: 'Título',
                    hintStyle: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: hintColor,
                    ),
                  ),
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16.0),
                  child: quill.QuillEditor.basic(
                    controller: _contentController,
                    
                  ),
                ),
              ),
            ],
          ),
        ),
        bottomNavigationBar: BottomAppBar(
          elevation: 0,
          color: Colors.transparent,
          child: Row(
            children: [
              IconButton(
                  icon: Icon(Icons.palette_outlined, color: textColor),
                  onPressed: _showBackgroundSheet),
              IconButton(
                  icon: Icon(Icons.text_fields, color: textColor),
                  onPressed: _showTextTools),
              IconButton(
                  icon: Icon(Icons.volume_up, color: textColor),
                  onPressed: _speak),
            ],
          ),
        ),
      ),
    );
  }
}
