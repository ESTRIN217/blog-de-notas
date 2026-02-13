import 'package:flutter/material.dart';
import 'list_item.dart';

class EditorScreen extends StatefulWidget {
  final ListItem item;

  const EditorScreen({super.key, required this.item});

  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> {
  late TextEditingController _titleController;
  late TextEditingController _summaryController;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.item.title);
    _summaryController = TextEditingController(text: widget.item.summary);
  }

  @override
  void dispose() {
    _titleController.dispose();
    _summaryController.dispose();
    super.dispose();
  }

  void _saveAndExit() {
    final updatedItem = ListItem(
      id: widget.item.id,
      title: _titleController.text,
      summary: _summaryController.text,
      lastModified: DateTime.now(),
    );
    Navigator.pop(context, updatedItem);
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false, // Prevent default back button behavior
      onPopInvoked: (didPop) {
        if (didPop) return;
        _saveAndExit();
      },
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: _saveAndExit,
          ),
          title: null, // No title
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            children: [
              TextField(
                controller: _titleController,
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                decoration: const InputDecoration.collapsed(
                  hintText: 'Title',
                ),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: TextField(
                  controller: _summaryController,
                  maxLines: null,
                  expands: true,
                  keyboardType: TextInputType.multiline,
                  decoration: const InputDecoration.collapsed(
                    hintText: 'Content...',
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
