import 'package:flutter/material.dart';

enum SortMethod { custom, alphabetical, byDate }

class ListItem {
  final String id;
  final String title;
  final String summary;
  final DateTime lastModified;

  ListItem({
    required this.id,
    required this.title,
    required this.summary,
    required this.lastModified,
  });
}
