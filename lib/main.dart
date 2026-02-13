import 'dart:math';

import 'package:flutter/material.dart';
import 'package:reorderable_grid_view/reorderable_grid_view.dart';
import 'list_item.dart';
import 'editor_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
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

  @override
  void initState() {
    super.initState();
    _items = List.generate(
      10,
      (index) => ListItem(
        id: '${index + 1}',
        title: 'Item ${index + 1}',
        summary: 'This is a summary of the item. It can span multiple lines, up to a maximum of ten lines. This text provides a brief overview of the content within the card, giving the user a glimpse of what to expect. This is line 3. This is line 4. This is line 5. This is line 6. This is line 7. This is line 8. This is line 9. This is the tenth and final line.',
        lastModified: DateTime.now().subtract(Duration(days: Random().nextInt(30))),
      ),
    );
  }

  void _toggleView() {
    setState(() {
      _isListView = !_isListView;
    });
  }

  Future<void> _navigateToEditor([ListItem? item]) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => EditorScreen(
          item: item ?? ListItem(id: DateTime.now().millisecondsSinceEpoch.toString(), title: '', summary: '', lastModified: DateTime.now()),
        ),
      ),
    );

    if (result is ListItem) {
      setState(() {
        final index = _items.indexWhere((i) => i.id == result.id);
        if (index != -1) {
          _items[index] = result; // Update existing item
        } else {
          _items.insert(0, result); // Add new item
        }

        // Re-sort if needed
        if (_sortMethod == SortMethod.alphabetical) {
          _sortAlphabetically();
        } else if (_sortMethod == SortMethod.byDate) {
          _sortByDate();
        }
      });
    }
  }

  void _showSortOptions() {
    showModalBottomSheet(
      context: context,
      builder: (context) {
        return Wrap(
          children: <Widget>[
            ListTile(
              leading: const Icon(Icons.sort_by_alpha),
              title: const Text('Sort Alphabetically'),
              onTap: () {
                _sortAlphabetically();
                Navigator.pop(context);
              },
            ),
            ListTile(
              leading: const Icon(Icons.date_range),
              title: const Text('Sort by Modification Date'),
              onTap: () {
                _sortByDate();
                Navigator.pop(context);
              },
            ),
            ListTile(
              leading: const Icon(Icons.drag_handle),
              title: const Text('Custom Sort'),
              onTap: () {
                _setCustomSort();
                Navigator.pop(context);
              },
            ),
          ],
        );
      },
    );
  }

  void _setCustomSort() {
    setState(() {
      _sortMethod = SortMethod.custom;
    });
  }

  void _sortAlphabetically() {
    setState(() {
      _sortMethod = SortMethod.alphabetical;
      _items.sort((a, b) => a.title.compareTo(b.title));
    });
  }

  void _sortByDate() {
    setState(() {
      _sortMethod = SortMethod.byDate;
      _items.sort((a, b) => b.lastModified.compareTo(a.lastModified));
    });
  }

  void _onReorder(int oldIndex, int newIndex) {
    setState(() {
      if (newIndex > oldIndex) {
        newIndex -= 1;
      }
      final ListItem item = _items.removeAt(oldIndex);
      _items.insert(newIndex, item);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: Builder(
          builder: (context) => IconButton(
            icon: const Icon(Icons.menu),
            onPressed: () => Scaffold.of(context).openDrawer(),
          ),
        ),
        title: TextField(
          decoration: InputDecoration(
            hintText: 'Search...',
            prefixIcon: const Icon(Icons.search),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(30.0),
              borderSide: BorderSide.none,
            ),
            filled: true,
            fillColor: Theme.of(context).colorScheme.surface,
            contentPadding: const EdgeInsets.symmetric(vertical: 0, horizontal: 20),
          ),
        ),
        actions: [
          IconButton(
            icon: Icon(_isListView ? Icons.grid_view : Icons.view_list),
            onPressed: _toggleView,
          ),
          IconButton(
            icon: const Icon(Icons.import_export),
            onPressed: _showSortOptions,
          ),
        ],
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
      ),
      drawer: Drawer(
        child: ListView(
          padding: EdgeInsets.zero,
          children: <Widget>[
            const DrawerHeader(
              decoration: BoxDecoration(
                color: Colors.deepPurple,
              ),
              child: Text(
                'Menu',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 24,
                ),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.home),
              title: const Text('Home'),
              onTap: () {
                Navigator.pop(context);
              },
            ),
            ListTile(
              leading: const Icon(Icons.settings),
              title: const Text('Settings'),
              onTap: () {
                Navigator.pop(context);
              },
            ),
          ],
        ),
      ),
      body: _isListView ? _buildReorderableListView() : _buildGridView(),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _navigateToEditor(),
        tooltip: 'Add Item',
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildItem(ListItem item) {
    return InkWell(
      onTap: () => _navigateToEditor(item),
      child: Card(
        elevation: 4,
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                item.title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                item.summary,
                maxLines: _isListView ? 10 : 6,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildReorderableListView() {
    if (_sortMethod != SortMethod.custom) {
      return _buildListView();
    }
    return ReorderableListView.builder(
      itemCount: _items.length,
      itemBuilder: (context, index) {
        final item = _items[index];
        return Container(
           key: ValueKey(item.id),
           margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: _buildItem(item)
        );
      },
      onReorder: _onReorder,
    );
  }

  Widget _buildListView() {
    return ListView.builder(
      itemCount: _items.length,
      itemBuilder: (context, index) {
        final item = _items[index];
         return Container(
           margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: _buildItem(item)
        );
      },
    );
  }

  Widget _buildGridView() {
    final gridDelegate = const SliverGridDelegateWithFixedCrossAxisCount(
      crossAxisCount: 2,
      crossAxisSpacing: 16,
      mainAxisSpacing: 16,
      childAspectRatio: 0.75,
    );

    if (_sortMethod != SortMethod.custom) {
      return GridView.builder(
        padding: const EdgeInsets.all(16.0),
        gridDelegate: gridDelegate,
        itemCount: _items.length,
        itemBuilder: (context, index) {
          final item = _items[index];
          return _buildItem(item);
        },
      );
    }

    return ReorderableGridView.builder(
      padding: const EdgeInsets.all(16.0),
      gridDelegate: gridDelegate,
      itemCount: _items.length,
      itemBuilder: (context, index) {
        final item = _items[index];
        return Container(
          key: ValueKey(item.id),
          child: _buildItem(item)
        );
      },
      onReorder: _onReorder,
    );
  }
}
