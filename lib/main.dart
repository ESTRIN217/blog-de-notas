import 'dart:io';

import 'package:flutter/material.dart';
import 'package:reorderable_grid_view/reorderable_grid_view.dart';
import 'package:share_plus/share_plus.dart';
import 'list_item.dart';
import 'editor_screen.dart';
import 'checklist_item.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Notes',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Notes'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  bool _isListView = true;
  SortMethod _sortMethod = SortMethod.custom;
  late List<ListItem> _items;
  late List<ListItem> _filteredItems;
  final TextEditingController _searchController = TextEditingController();

  bool _isSelectionMode = false;
  final List<ListItem> _selectedItems = [];

  @override
  void initState() {
    super.initState();
    _items = [
      ListItem(
        id: 'welcome_note',
        title: '¡Bienvenido a Flutter Notes!',
        summary: 'Esta es una nota de ejemplo para que explores las funcionalidades de la app.',
        lastModified: DateTime.now(),
        backgroundColor: Colors.amber[200]!.toARGB32(),
        fontSize: 15.0,
        checklist: [
          ChecklistItem(id: 'c1', text: '← Abre el menú para ver más opciones.', isChecked: false),
          ChecklistItem(id: 'c2', text: '↓ Toca el botón de `+` para crear una nueva nota.', isChecked: false),
          ChecklistItem(id: 'c3', text: 'Mantén pulsada una nota para seleccionarla y ver más acciones.', isChecked: false),
          ChecklistItem(id: 'c4', text: 'Personaliza el fondo con el botón de la paleta en el editor.', isChecked: true),
          ChecklistItem(id: 'c5', text: 'Crea tus propias listas de tareas como esta.', isChecked: false),
          ChecklistItem(id: 'c6', text: '¡Explora y disfruta de la aplicación!', isChecked: false),
        ]
      )
    ];
    _filteredItems = _items;
    _searchController.addListener(_filterItems);
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }


  void _filterItems() {
    final query = _searchController.text.toLowerCase();
    setState(() {
      _filteredItems = _items.where((item) {
        final titleMatch = item.title.toLowerCase().contains(query);
        final summaryMatch = item.summary.toLowerCase().contains(query);
        return titleMatch || summaryMatch;
      }).toList();
      _sortFilteredItems(); 
    });
  }

  void _sortFilteredItems() {
     if (_sortMethod == SortMethod.alphabetical) {
      _filteredItems.sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));
    } else if (_sortMethod == SortMethod.byDate) {
      _filteredItems.sort((a, b) => b.lastModified.compareTo(a.lastModified));
    } else if (_sortMethod == SortMethod.custom) {
        _filteredItems.sort((a,b) {
            final aIndex = _items.indexOf(a);
            final bIndex = _items.indexOf(b);
            return aIndex.compareTo(bIndex);
        });
    }
  }

  void _toggleView() {
    setState(() {
      _isListView = !_isListView;
    });
  }

  Future<void> _navigateToEditor([ListItem? item]) async {
    if (_isSelectionMode) return; 

    final originalItem = item ?? ListItem(id: DateTime.now().millisecondsSinceEpoch.toString(), title: '', summary: '', lastModified: DateTime.now());

    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => EditorScreen(item: originalItem),
      ),
    );

    if (result == null) return;

    if (result == "DELETE") {
        setState(() {
            _items.removeWhere((i) => i.id == originalItem.id);
            _filterItems();
        });
    } else if (result is ListItem) {
      setState(() {
        final index = _items.indexWhere((i) => i.id == result.id);

        if (result.title.isEmpty && result.summary.isEmpty && result.checklist.isEmpty) {
            if (index != -1) {
                _items.removeAt(index);
            }
            _filterItems();
            return;
        }

        if (index != -1) {
          _items[index] = result; 
        } else {
          _items.insert(0, result); 
        }

        _filterItems();
      });
    }
  }

  void _startSelectionMode(ListItem item) {
    if (_isSelectionMode) return;
    setState(() {
      _isSelectionMode = true;
      _selectedItems.add(item);
    });
  }

  void _toggleSelection(ListItem item) {
    setState(() {
      if (_selectedItems.contains(item)) {
        _selectedItems.remove(item);
      } else {
        _selectedItems.add(item);
      }
      if (_selectedItems.isEmpty) {
        _isSelectionMode = false;
      }
    });
  }

  void _exitSelectionMode() {
    setState(() {
      _isSelectionMode = false;
      _selectedItems.clear();
    });
  }

  void _deleteSelectedItems() {
    setState(() {
      _items.removeWhere((item) => _selectedItems.contains(item));
       _filterItems();
      _exitSelectionMode();
    });
  }

  void _shareSelectedItems() {
    final content = _selectedItems.map((item) => "${item.title}\n${item.summary}").join('\n\n---\n\n');
    SharePlus.instance.share(
        ShareParams(
            text: content,
                subject: 'My Notes',
                  ),
                  );
    _exitSelectionMode();
  }

  void _showSortOptions() {
    showModalBottomSheet(
      context: context,
      builder: (context) => Wrap(
        children: <Widget>[
          ListTile(leading: const Icon(Icons.sort_by_alpha), title: const Text('Sort Alphabetically'), onTap: () => _sortAlphabetically()),
          ListTile(leading: const Icon(Icons.date_range), title: const Text('Sort by Modification Date'), onTap: () => _sortByDate()),
          ListTile(leading: const Icon(Icons.drag_handle), title: const Text('Custom Sort'), onTap: () => _setCustomSort()),
        ],
      ),
    );
  }

  void _setCustomSort() {
    setState(() {
      _sortMethod = SortMethod.custom;
      _filterItems();
    });
    Navigator.pop(context);
  }

  void _sortAlphabetically({bool preserveState = true}) {
    if (preserveState) Navigator.pop(context);
    setState(() {
      _sortMethod = SortMethod.alphabetical;
      _items.sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));
      _filterItems();
    });
  }

  void _sortByDate({bool preserveState = true}) {
     if (preserveState) Navigator.pop(context);
    setState(() {
      _sortMethod = SortMethod.byDate;
      _items.sort((a, b) => b.lastModified.compareTo(a.lastModified));
       _filterItems();
    });
  }

  void _onReorder(int oldIndex, int newIndex) {
    setState(() {
        final itemToMove = _filteredItems[oldIndex];
        final masterIndexOfItemToMove = _items.indexOf(itemToMove);

        _items.removeAt(masterIndexOfItemToMove);

        if (newIndex > oldIndex) {
            final referenceItem = _filteredItems[newIndex-1];
            final masterIndexOfReferenceItem = _items.indexOf(referenceItem);
            _items.insert(masterIndexOfReferenceItem + 1, itemToMove);
        } else {
            final referenceItem = _filteredItems[newIndex];
            final masterIndexOfReferenceItem = _items.indexOf(referenceItem);
            _items.insert(masterIndexOfReferenceItem, itemToMove);
        }

        _filterItems();
    });
  }

  PreferredSizeWidget _buildAppBar() {
    if (_isSelectionMode) {
      return AppBar(
        leading: IconButton(icon: const Icon(Icons.close), onPressed: _exitSelectionMode),
        title: Text('${_selectedItems.length} selected'),
        actions: [
          IconButton(icon: const Icon(Icons.share), onPressed: _shareSelectedItems, tooltip: 'Share'),
          IconButton(icon: const Icon(Icons.delete), onPressed: _deleteSelectedItems, tooltip: 'Delete'),
        ],
      );
    }

    return AppBar(
      leading: Builder(builder: (context) => IconButton(icon: const Icon(Icons.menu), onPressed: () => Scaffold.of(context).openDrawer())),
      title: TextField(
        controller: _searchController,
        decoration: InputDecoration(
          hintText: 'Search...',
          prefixIcon: const Icon(Icons.search),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(30.0), borderSide: BorderSide.none),
          filled: true,
          fillColor: Theme.of(context).colorScheme.surface,
          contentPadding: const EdgeInsets.symmetric(vertical: 0, horizontal: 20),
        ),
      ),
      actions: [
        IconButton(icon: Icon(_isListView ? Icons.grid_view : Icons.view_list), onPressed: _toggleView, tooltip: 'Toggle View'),
        IconButton(icon: const Icon(Icons.import_export), onPressed: _showSortOptions, tooltip: 'Sort'),
      ],
      backgroundColor: Theme.of(context).colorScheme.primaryContainer,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _buildAppBar(),
      drawer: Drawer(
        child: ListView(
          padding: EdgeInsets.zero,
          children: <Widget>[
            const DrawerHeader(decoration: BoxDecoration(color: Colors.deepPurple), child: Text('Menu', style: TextStyle(color: Colors.white, fontSize: 24))),
            ListTile(leading: const Icon(Icons.home), title: const Text('Home'), onTap: () => Navigator.pop(context)),
            ListTile(leading: const Icon(Icons.settings), title: const Text('Settings'), onTap: () => Navigator.pop(context)),
          ],
        ),
      ),
      body: _isListView ? _buildListView() : _buildGridView(),
      floatingActionButton: _isSelectionMode ? null : FloatingActionButton(
        onPressed: () => _navigateToEditor(),
        tooltip: 'Add Item',
        child: const Icon(Icons.add),
      ),
    );
  }

  bool _isColorDark(int? colorValue) {
    if (colorValue == null) return Theme.of(context).brightness == Brightness.dark;
    return Color(colorValue).computeLuminance() < 0.5;
  }

  Widget _buildItem(ListItem item, {bool isListView = true}) {
    final isSelected = _selectedItems.contains(item);
    final bool canReorder = _sortMethod == SortMethod.custom;

    final isDark = _isColorDark(item.backgroundColor);
    final textColor = isDark ? Colors.white : Colors.black;

    BoxDecoration? decoration;
    if (item.backgroundImagePath != null) {
      decoration = BoxDecoration(
        image: DecorationImage(
          image: FileImage(File(item.backgroundImagePath!)),
          fit: BoxFit.cover,
        ),
      );
    } else if (item.backgroundColor != null) {
      decoration = BoxDecoration(color: Color(item.backgroundColor!));
    }

    final summaryStyle = TextStyle(
      color: textColor.withAlpha((255 * 0.8).round()),
      fontSize: item.fontSize,
      fontWeight: item.isBold ? FontWeight.bold : FontWeight.normal,
      fontStyle: item.isItalic ? FontStyle.italic : FontStyle.normal,
    );

    final contentColumn = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        if (item.title.isNotEmpty)
          Text(
            item.title,
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: textColor),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        if (item.title.isNotEmpty && item.summary.isNotEmpty) const SizedBox(height: 8),
        if (item.summary.isNotEmpty)
          isListView
              ? Text(
                  item.summary,
                  style: summaryStyle,
                  maxLines: 10,
                  overflow: TextOverflow.ellipsis,
                )
              : Expanded(
                  child: Text(
                    item.summary,
                    style: summaryStyle,
                    maxLines: 6,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
      ],
    );

    return Container(
      decoration: decoration,
      child: Card(
        elevation: 2,
        clipBehavior: Clip.antiAlias,
        color: isSelected
            ? Theme.of(context).colorScheme.primaryContainer.withAlpha((255 * 0.6).round())
            : (decoration != null ? Colors.transparent : null),
        child: InkWell(
          onTap: () => _isSelectionMode ? _toggleSelection(item) : _navigateToEditor(item),
          onLongPress: () {
            if (!_isSelectionMode) {
              _startSelectionMode(item);
            }
          },
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: contentColumn,
                ),
              ),
              if (canReorder && !_isSelectionMode)
                ReorderableDragStartListener(
                  index: _filteredItems.indexOf(item),
                  child: Padding(
                    padding: const EdgeInsets.only(right: 12.0, top: 12.0, left: 4.0),
                    child: Icon(Icons.drag_handle, color: textColor.withAlpha((255 * 0.6).round())),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildListView() {
    final bool canReorder = _sortMethod == SortMethod.custom;
    if (canReorder) {
      return ReorderableListView.builder(
        buildDefaultDragHandles: false,
        itemCount: _filteredItems.length,
        itemBuilder: (context, index) {
          final item = _filteredItems[index];
          return Container(key: ValueKey(item.id), margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8), child: _buildItem(item));
        },
        onReorder: _onReorder,
      );
    }
    return ListView.builder(
      itemCount: _filteredItems.length,
      itemBuilder: (context, index) {
        final item = _filteredItems[index];
        return Container(margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8), child: _buildItem(item, isListView: true));
      },
    );
  }

  Widget _buildGridView() {
    final bool canReorder = _sortMethod == SortMethod.custom;
    final gridDelegate = const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, crossAxisSpacing: 16, mainAxisSpacing: 16, childAspectRatio: 0.75);

    if (canReorder) {
      return ReorderableGridView.builder(
        dragEnabled: false, 
        padding: const EdgeInsets.all(16.0),
        gridDelegate: gridDelegate,
        itemCount: _filteredItems.length,
        itemBuilder: (context, index) => Container(key: ValueKey(_filteredItems[index].id), child: _buildItem(_filteredItems[index], isListView: false)),
        onReorder: _onReorder,
      );
    }
    return GridView.builder(
      padding: const EdgeInsets.all(16.0),
      gridDelegate: gridDelegate,
      itemCount: _filteredItems.length,
      itemBuilder: (context, index) => _buildItem(_filteredItems[index], isListView: false),
    );
  }
}
