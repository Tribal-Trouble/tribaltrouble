# Translation Tools

This document explains how to use the translation tools for Tribal Trouble to contribute translations in multiple languages.

## Overview

The translation system allows community contributors to work with game translations using familiar CSV/spreadsheet tools instead of directly editing Java properties files. The system converts between Java properties format and CSV format for easier editing.

Ask to be an editor on the translation document to help contribute:

Google sheet: <https://docs.google.com/spreadsheets/d/1rcTgC4fkxqZUE71Bq0asn_Ahz3px-NeKc-qFm0T2ObI>

## System Architecture

The translation tools consist of two Java classes integrated with the Ant build system:

- `TranslationExtractor` - Converts properties files to CSV format
- `TranslationImporter` - Converts CSV files back to properties format

These tools are executed through Ant build targets in the `tools/build.xml` file.

## Supported Languages

Currently supported languages:

- **en** - English (base language)
- **da** - Danish
- **de** - German
- **es** - Spanish
- **it** - Italian

## File Structure

Translation files are organized in the `tt/i18n/com/oddlabs/tt/` directory structure. Each translatable component has:

- Base file (English): `ComponentName.properties`
- Localized files: `ComponentName_[language].properties`

Examples:

- `Main.properties` (English)
- `Main_da.properties` (Danish)
- `form/GameMenu.properties` (English)
- `form/GameMenu_de.properties` (German)

The **TranslationBase** represents the file path without language suffix, such as:

- `Main`
- `form/GameMenu`
- `player/campaign/VikingIsland0`

## Usage Instructions

### Extract Translations to CSV

To convert all properties files to CSV format for editing:

```bash
cd tools
ant extract-translations
```

This creates CSV files in the `translations/` folder:

- `translations_en.csv`
- `translations_da.csv`
- `translations_de.csv`
- `translations_es.csv`
- `translations_it.csv`

### Edit Translations

Open the CSV files in your preferred spreadsheet application (Excel, LibreOffice Calc, Google Sheets, etc.) and edit the translations in the `value` column.

### Import Translations Back

After editing, convert the CSV files back to properties format:

```bash
cd tools
ant import-translations
```

This updates the properties files in `tt/i18n/com/oddlabs/tt/` with your changes.

## CSV Format

Each CSV file contains three columns:

| Column | Description | Example |
|--------|-------------|---------|
| **TranslationBase** | File path without language suffix | `form/GameMenu` |
| **key** | Translation key identifier | `start_game` |
| **value** | Translated text | `Start Game` |

Example CSV content:

```csv
TranslationBase,key,value
Main,error,Error
Main,error_message,"The game has crashed with the following message..."
form/GameMenu,start_game,Start Game
form/GameMenu,quit,Quit
```

Multi-line translations are properly quoted and escaped in the CSV format.

## Using Translations in Code

To use translations in the game code, load the appropriate ResourceBundle and retrieve strings using the translation keys:

```java
// Method 1: Load bundle using class name string
ResourceBundle bundle = ResourceBundle.getBundle("com.oddlabs.tt.form.GameMenu");

// Method 2: Load bundle using class.getName() (preferred for static fields)
private static final ResourceBundle bundle =
    ResourceBundle.getBundle(BugClientWindow.class.getName());

// Get translated string using Utils helper method
String startGameText = Utils.getBundleString(bundle, "start_game");

// The actual translation will be loaded based on the current locale:
// - English: "Start Game"
// - Danish: "Start Spil"
// - German: "Spiel Starten"
```

The ResourceBundle automatically selects the appropriate language file based on the current locale setting.
