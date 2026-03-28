# Blueprint: Note-Taking App

## 1. Overview

This document outlines the design and features of a modern, intuitive note-taking application built with Flutter. The app provides a flexible and user-friendly interface for creating, managing, and organizing notes, with advanced customization and localization features.

## 2. Features by Version

### 2.1. Version 3.0.0

- **Advanced Theming & UI:**
- **Theme Modes:** Full support for **Light and Dark modes**, plus a **System** setting to sync with the OS.
- **Dynamic Color:** The app's color scheme can be dynamically generated from the user's wallpaper on supported Android versions (Material You).
- **Custom Typography:** Integration with **Google Fonts** for a polished and consistent text appearance.
- **Settings Persistence:** All user preferences, including theme, language, and sort order, are saved across app sessions using `shared_preferences`.
- **Internationalization (i10n):**
- **Multi-Language Support:** The UI is fully localized for **English** and **Spanish**.
- **Language Selection:** Users can manually switch the app's language from the settings screen.
- **Enhanced Rich Text Editor (Flutter Quill):**
- The previous custom editor has been replaced with the powerful `flutter_quill` library, providing a more robust and feature-rich editing experience.
- **Text-to-Speech:**
- **Accessibility:** An integrated text-to-speech feature can read notes aloud.

### 2.2. Version 2.0.0 (Features integrated and enhanced in v3.0)

- **Editor Enhancements:**
- **Undo/Redo Functionality:** Undo and redo text and style changes.
- **Background Customization:** Change a note's background color or set a background image from the gallery.
- **Text Formatting:** Basic text styling like font size, bold, and italics.
- **Checklists:** Create interactive checklists within notes.
    *Note: These features were originally part of a custom editor and have now been superseded by the `flutter_quill` implementation in v3.0.*

### 2.3. Version 1.0.0 (Core Features)

- **Main Screen & Layout:**
- **Dual View Modes:** Switch between list and grid views.
- **Customizable Sorting:** Sort notes alphabetically, by modification date, or use a custom drag-and-drop order.
- **Functional Search:** Real-time note filtering by title or content.
- **Note Editor:**
- **Rich Text Editing:** A dedicated screen for editing notes.
- **Automatic Saving:** Changes are saved automatically when the user exits the editor.
- **Empty Note Handling:** Blank notes are automatically discarded.
- **Selection & Bulk Actions:**
- **Selection Mode:** Long-press a note to enter selection mode and choose multiple items.
- **Contextual App Bar:** Appears in selection mode to allow bulk sharing or deleting of selected notes.

### 2.4 Version 4.0.0

- **Imagen support in note**
- **Shared in PDF, Markdown,and JSON**

## 3. Code Quality & Versioning

### 3.1. Dependencies

- The project utilizes `flutter_quill`, `provider`, `dynamic_color`, `google_fonts`, `shared_preferences`, `flutter_tts`, `floating_draggable_widget`, and more to support its advanced feature set.

### 3.2. Versioning

- **Current Version:** `3.0.0+1`
