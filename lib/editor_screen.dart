import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:share_plus/share_plus.dart';
import 'list_item.dart';
import 'checklist_item.dart';

@immutable
class _EditorHistoryState {
  final TextEditingValue title;
  final TextEditingValue summary;
  final List<ChecklistItem> checklist;
  final int? backgroundColor;
  final String? backgroundImagePath;
  final double? fontSize;
  final bool isBold;
  final bool isItalic;

  const _EditorHistoryState({
    required this.title,
    required this.summary,
    required this.checklist,
    this.backgroundColor,
    this.backgroundImagePath,
    this.fontSize,
    required this.isBold,
    required this.isItalic,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! _EditorHistoryState) return false;
    if (title != other.title ||
        summary != other.summary ||
        backgroundColor != other.backgroundColor ||
        backgroundImagePath != other.backgroundImagePath ||
        fontSize != other.fontSize ||
        isBold != other.isBold ||
        isItalic != other.isItalic ||
        checklist.length != other.checklist.length) {
      return false;
    }
    for (int i = 0; i < checklist.length; i++) {
      if (checklist[i].id != other.checklist[i].id ||
          checklist[i].text != other.checklist[i].text ||
          checklist[i].isChecked != other.checklist[i].isChecked) {
        return false;
      }
    }
    return true;
  }

  @override
  int get hashCode => Object.hash(title, summary, backgroundColor, backgroundImagePath, fontSize, isBold, isItalic, Object.hashAll(checklist));
}

class EditorScreen extends StatefulWidget {
  final ListItem item;

  const EditorScreen({super.key, required this.item});

  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> {
  late TextEditingController _titleController;
  late TextEditingController _summaryController;
  late List<ChecklistItem> _checklistItems;
  final Map<String, TextEditingController> _checklistControllers = {};

  int? _backgroundColorValue;
  String? _backgroundImagePath;
  double? _fontSize;
  bool _isBold = false;
  bool _isItalic = false;

  final List<_EditorHistoryState> _undoStack = [];
  final List<_EditorHistoryState> _redoStack = [];

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.item.title);
    _summaryController = TextEditingController(text: widget.item.summary);
    _checklistItems = widget.item.checklist.map((item) => 
        ChecklistItem(id: item.id, text: item.text, isChecked: item.isChecked)
    ).toList();
    
    _backgroundColorValue = widget.item.backgroundColor;
    _backgroundImagePath = widget.item.backgroundImagePath;
    _fontSize = widget.item.fontSize ?? 14.0;
    _isBold = widget.item.isBold;
    _isItalic = widget.item.isItalic;

    for (var item in _checklistItems) {
      _checklistControllers[item.id] = TextEditingController(text: item.text)
        ..addListener(() => _onChecklistItemChanged(item.id));
    }

    _recordState(isInitial: true);

    _titleController.addListener(_recordState);
    _summaryController.addListener(_recordState);
  }

  @override
  void dispose() {
    _titleController.removeListener(_recordState);
    _summaryController.removeListener(_recordState);
    _titleController.dispose();
    _summaryController.dispose();
    for (var controller in _checklistControllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  void _recordState({bool isInitial = false}) {
    final newState = _EditorHistoryState(
      title: _titleController.value,
      summary: _summaryController.value,
      checklist: _checklistItems.map((item) => 
          ChecklistItem(id: item.id, text: item.text, isChecked: item.isChecked)
      ).toList(),
      backgroundColor: _backgroundColorValue,
      backgroundImagePath: _backgroundImagePath,
      fontSize: _fontSize,
      isBold: _isBold,
      isItalic: _isItalic,
    );

    if (_undoStack.isNotEmpty && newState == _undoStack.last) {
      return;
    }

    setState(() {
      if (isInitial) {
        _undoStack.add(newState);
      } else {
        _undoStack.add(newState);
        _redoStack.clear();
      }
    });
  }

  void _onChecklistItemChanged(String itemId) {
    final controller = _checklistControllers[itemId];
    final item = _checklistItems.firstWhere((i) => i.id == itemId);
    if (controller != null && item.text != controller.text) {
        item.text = controller.text;
        _recordState();
    }
  }

  void _undo() {
    if (_undoStack.length < 2) return;

    setState(() {
      final currentState = _undoStack.removeLast();
      _redoStack.add(currentState);
      _applyState(_undoStack.last);
    });
  }

  void _redo() {
    if (_redoStack.isEmpty) return;

    setState(() {
      final nextState = _redoStack.removeLast();
      _undoStack.add(nextState);
      _applyState(nextState);
    });
  }

  void _applyState(_EditorHistoryState state) {
    _titleController.removeListener(_recordState);
    _summaryController.removeListener(_recordState);

    _titleController.value = state.title;
    _summaryController.value = state.summary;
    _backgroundColorValue = state.backgroundColor;
    _backgroundImagePath = state.backgroundImagePath;
    _fontSize = state.fontSize;
    _isBold = state.isBold;
    _isItalic = state.isItalic;
    _checklistItems = state.checklist.map((item) => 
        ChecklistItem(id: item.id, text: item.text, isChecked: item.isChecked)
    ).toList();

    for (var controller in _checklistControllers.values) {
      controller.dispose();
    }
    _checklistControllers.clear();
    for (var item in _checklistItems) {
      _checklistControllers[item.id] = TextEditingController(text: item.text)
        ..addListener(() => _onChecklistItemChanged(item.id));
    }

    _titleController.addListener(_recordState);
    _summaryController.addListener(_recordState);
  }

  void _saveAndExit() {
    if (!mounted) return;
    final updatedItem = ListItem(
      id: widget.item.id,
      title: _titleController.text,
      summary: _summaryController.text,
      lastModified: DateTime.now(),
      checklist: _checklistItems,
      backgroundColor: _backgroundColorValue,
      backgroundImagePath: _backgroundImagePath,
      fontSize: _fontSize,
      isBold: _isBold,
      isItalic: _isItalic,
    );
    Navigator.pop(context, updatedItem);
  }

  void _shareItem() {
    final title = _titleController.text;
    final summary = _summaryController.text;
    final checklistText = _checklistItems.map((item) => '[${item.isChecked ? 'x' : ' '}] ${item.text}').join('\n');
    SharePlus.instance.share(
  ShareParams(
    text: '$title\n\n$summary\n\n$checklistText',
    subject: title,
  ),
);
  }

  void _deleteItem() {
    if (!mounted) return;
    Navigator.pop(context, "DELETE");
  }

  void _addChecklistItem() {
    setState(() {
      final newItem = ChecklistItem(id: DateTime.now().millisecondsSinceEpoch.toString());
      _checklistItems.add(newItem);
      _checklistControllers[newItem.id] = TextEditingController(text: newItem.text)
        ..addListener(() => _onChecklistItemChanged(newItem.id));
      _recordState();
    });
  }

 void _deleteChecklistItem(String id) {
    setState(() {
        final controller = _checklistControllers.remove(id);
        controller?.dispose();
        _checklistItems.removeWhere((item) => item.id == id);
        _recordState();
    });
}

  void _onReorder(int oldIndex, int newIndex) {
    setState(() {
      if (newIndex > oldIndex) {
        newIndex -= 1;
      }
      final item = _checklistItems.removeAt(oldIndex);
      _checklistItems.insert(newIndex, item);
      _recordState();
    });
  }

  void _showEditorMenu() {
    showModalBottomSheet(
      context: context,
      builder: (ctx) {
        return Wrap(children: <Widget>[
          ListTile(leading: const Icon(Icons.share), title: const Text('Share'), onTap: () { Navigator.pop(ctx); _shareItem(); }),
          ListTile(leading: const Icon(Icons.delete), title: const Text('Delete'), onTap: () { Navigator.pop(ctx); _deleteItem(); }),
        ]);
      },
    );
  }

  void _showAddContentSheet() {
    showModalBottomSheet(
      context: context,
      builder: (ctx) {
        return Wrap(children: <Widget>[
          ListTile(
            leading: const Icon(Icons.check_box_outlined),
            title: const Text('Checklist Item'),
            onTap: () {
              Navigator.pop(ctx);
              _addChecklistItem();
            },
          ),
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
                          color: colorValue != null ? Color(colorValue) : Theme.of(context).scaffoldBackgroundColor,
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
                title: const Text('Image from gallery'),
                onTap: () {
                  _pickImage();
                  Navigator.pop(ctx);
                },
              ),
            ],
          );
        });
  }
  
  void _changeBackgroundColor(int? colorValue) {
    setState(() {
      _backgroundColorValue = colorValue;
      _backgroundImagePath = null;
      _recordState();
    });
  }

  Future<void> _pickImage() async {
    final picker = ImagePicker();
    final pickedFile = await picker.pickImage(source: ImageSource.gallery);

    if (pickedFile != null) {
      setState(() {
        _backgroundImagePath = pickedFile.path;
        _backgroundColorValue = null;
        _recordState();
      });
    }
  }

  void _showTextFormatSheet() {
    showModalBottomSheet(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setSheetState) {
            return Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Font Size', style: TextStyle(fontWeight: FontWeight.bold)),
                  Slider(
                    value: _fontSize ?? 14.0,
                    min: 10.0,
                    max: 28.0,
                    divisions: 9,
                    label: (_fontSize ?? 14.0).round().toString(),
                    onChanged: (double value) {
                      setSheetState(() {
                        _fontSize = value;
                      });
                      setState(() {}); // Update the main UI for live preview
                    },
                    onChangeEnd: (double value) {
                       _recordState();
                    },
                  ),
                  const SizedBox(height: 16),
                   const Text('Font Style', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  ToggleButtons(
                    isSelected: [_isBold, _isItalic],
                    onPressed: (int index) {
                      setSheetState(() {
                        if (index == 0) _isBold = !_isBold;
                        if (index == 1) _isItalic = !_isItalic;
                      });
                       setState(() {}); // Update the main UI for live preview
                       _recordState();
                    },
                    borderRadius: const BorderRadius.all(Radius.circular(8)),
                    children: const [
                      Padding(padding: EdgeInsets.symmetric(horizontal: 16), child: Icon(Icons.format_bold)),
                      Padding(padding: EdgeInsets.symmetric(horizontal: 16), child: Icon(Icons.format_italic)),
                    ],
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  bool _isColorDark(int? colorValue) {
    if (colorValue == null) return Theme.of(context).brightness == Brightness.dark;
    return Color(colorValue).computeLuminance() < 0.5;
  }

  Widget _buildChecklistItem(ChecklistItem item, int index, Color textColor) {
    final controller = _checklistControllers[item.id];
    if (controller == null) return Container();

    return ReorderableDelayedDragStartListener(
      index: index,
      child: Row(
        key: ValueKey(item.id),
        children: [
          Checkbox(
            value: item.isChecked,
            onChanged: (bool? value) {
              if (value != null) {
                setState(() {
                  item.isChecked = value;
                  _recordState();
                });
              }
            },
            activeColor: textColor,
            checkColor: _backgroundColorValue != null ? Color(_backgroundColorValue!) : null,
          ),
          Expanded(
            child: TextField(
              controller: controller,
              decoration: const InputDecoration(border: InputBorder.none, hintText: 'List item'),
              style: TextStyle(
                color: textColor,
                decoration: item.isChecked ? TextDecoration.lineThrough : TextDecoration.none,
              ),
            ),
          ),
          IconButton(icon: Icon(Icons.clear, color: textColor), onPressed: () => _deleteChecklistItem(item.id)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = _isColorDark(_backgroundColorValue);
    final textColor = isDark ? Colors.white : Colors.black;
    final hintColor = isDark ? Colors.white70 : Colors.black54;
    final appBarColor = _backgroundColorValue != null ? Color(_backgroundColorValue!) : null;

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

    final contentStyle = TextStyle(
      color: textColor,
      fontSize: _fontSize,
      fontWeight: _isBold ? FontWeight.bold : FontWeight.normal,
      fontStyle: _isItalic ? FontStyle.italic : FontStyle.normal,
    );

    return PopScope(
        canPop: false,
        onPopInvokedWithResult: (didPop, _) { if (didPop) return; _saveAndExit(); },
        child: Scaffold(
            appBar: AppBar(
              leading: IconButton(icon: Icon(Icons.arrow_back, color: textColor), onPressed: _saveAndExit),
              backgroundColor: appBarColor,
              elevation: _backgroundColorValue != null ? 0 : null,
              title: null,
              actions: [
                IconButton(icon: Icon(Icons.undo, color: textColor), onPressed: _undoStack.length > 1 ? _undo : null),
                IconButton(icon: Icon(Icons.redo, color: textColor), onPressed: _redoStack.isNotEmpty ? _redo : null),
                IconButton(icon: Icon(Icons.more_vert, color: textColor), onPressed: _showEditorMenu),
              ],
            ),
            body: Container(
              decoration: backgroundDecoration,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                child: CustomScrollView(
                  slivers: [
                    SliverList(
                      delegate: SliverChildListDelegate([
                        TextField(
                          controller: _titleController,
                          style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: textColor),
                          decoration: InputDecoration(border: InputBorder.none, hintText: 'Title', hintStyle: TextStyle(color: hintColor)),
                        ),
                        TextField(
                          controller: _summaryController,
                          maxLines: null,
                          keyboardType: TextInputType.multiline,
                          style: contentStyle,
                          decoration: InputDecoration(border: InputBorder.none, hintText: 'Content...', hintStyle: TextStyle(color: hintColor)),
                        ),
                      ]),
                    ),
                    SliverReorderableList(
                      itemBuilder: (context, index) => _buildChecklistItem(_checklistItems[index], index, textColor),
                      itemCount: _checklistItems.length,
                      onReorder: _onReorder,
                    ),
                  ],
                ),
              ),
            ),
            bottomNavigationBar: BottomAppBar(
               elevation: 0,
              color: Colors.transparent,
              child: Row(
                children: [
                  IconButton(icon: Icon(Icons.add, color: textColor), onPressed: _showAddContentSheet),
                  IconButton(icon: Icon(Icons.palette_outlined, color: textColor), onPressed: _showBackgroundSheet),
                  IconButton(icon: Icon(Icons.text_fields, color: textColor), onPressed: _showTextFormatSheet),
                ],
              ),
            )));
  }
}
