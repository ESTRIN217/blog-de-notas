# Blueprint: Note-Taking App

## 1. Overview

This document outlines the design and features of a modern, intuitive note-taking application built with Flutter. The app provides a flexible and user-friendly interface for creating, managing, and organizing notes.

## 2. Core Features (v1.0.0)

### 2.1. Main Screen & Layout
- **Dual View Modes:** Users can switch between a compact **list view** and a more visual **grid view**.
- **Customizable Sorting:** Notes can be sorted by:
    - **Alphabetical** order (A-Z).
    - **Modification Date** (newest first).
    - **Custom** user-defined order.
- **Persistent Reordering:** In "Custom" sort mode, users can drag and drop notes to manually rearrange them. This order is maintained across sessions.
- **Functional Search:** A search bar in the app header allows users to filter notes in real-time by title or content.

### 2.2. Note Editor
- **Rich Text Editing:** A dedicated screen for creating and editing notes with distinct fields for title and content.
- **Automatic Saving:** Changes are automatically saved when the user navigates back to the main screen.
- **Empty Note Handling:** Notes left blank (no title and no content) are automatically discarded to prevent clutter.
- **In-Editor Actions:** A context menu within the editor provides quick access to:
    - **Share:** Share the note's content with other apps.
    - **Delete:** Remove the current note.

### 2.3. Selection & Bulk Actions
- **Selection Mode:** Users can long-press a note to enter a multi-selection mode.
- **Contextual App Bar:** In selection mode, the app bar transforms to show the number of selected items and provide bulk actions:
    - **Share:** Share the content of all selected notes.
    - **Delete:** Permanently delete all selected notes.

### 2.4. Onboarding Experience
- **Welcome Note:** On the first run, the app generates a single, informative welcome note to guide the user.

## 3. Code Quality & Versioning

### 3.1. Code Quality
- **Dependency Management:** The project uses `share_plus`, `reorderable_grid_view`, and `image_picker`.
- **Modern Practices:** The codebase has been updated to replace all deprecated widgets and practices, including `PopScope`, `InputDecoration.collapsed`, and ensuring safe `BuildContext` handling.
- **Platform Configuration:** The app is configured with the necessary permissions for `image_picker` on both iOS (`Info.plist`) and Android (`AndroidManifest.xml`).

### 3.2. Versioning
- **v1.0.0:** This version marks the first stable release of the application. It includes all core features listed above and has been tested for functionality and stability. The version is set in `pubspec.yaml` as `1.0.0+1`.

## 4. Version 2.0 Goals

### 4.1. Editor Enhancements
- **Undo/Redo Functionality:**
    - **Implementation:** The editor screen now features `undo` and `redo` buttons directly in the `AppBar` for easy access.
    - **Logic:** A history of text changes is maintained, allowing users to navigate this history. The buttons are dynamically enabled/disabled based on whether actions are available.

- **Background Customization:**
    - **Implementation:** A palette icon in the `BottomAppBar` opens a `BottomSheet` for background customization.
    - **Color Palette:** Users can select from a predefined list of colors or revert to the default note color.
    - **Image Background:** Users can pick an image from their device's gallery to use as a background for the note.
    - **Readability:** The color of the text (title, content) automatically adjusts to be light on dark backgrounds and dark on light backgrounds, ensuring legibility.
    - **Main Screen Display:** The selected background color or image is visible on the note's preview card on the main screen.

- **Text Formatting & Checklists:**
    - **Implementation:** A text-fields icon in the `BottomAppBar` opens a `BottomSheet` containing the rich text formatting toolbar.
    - **Font Size & Style:** Users can adjust the font size and apply **bold** and *italic* styles to the note's content.
    - **Checklists:** Users can create interactive checklists directly within the note content.
    - **Live Preview:** All text style and checklist changes are reflected in real-time in the editor.
