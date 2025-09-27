package com.oddlabs.i18n;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final strictfp class TranslationImporter {
    private static final String[] SUPPORTED_LANGUAGES = {"en", "da", "de", "es", "it"};

    public static final void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: TranslationImporter <csv_dir> <output_i18n_path>");
            System.exit(1);
        }

        String csvDir = args[0];
        String outputPath = args[1];

        try {
            importTranslations(csvDir, outputPath);
        } catch (IOException e) {
            System.err.println("Error importing translations: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void importTranslations(String csvDir, String outputPath) throws IOException {
        Path csvDirectory = Paths.get(csvDir);
        Path outputDirectory = Paths.get(outputPath);

        for (String language : SUPPORTED_LANGUAGES) {
            Path csvFile = csvDirectory.resolve("translations_" + language + ".csv");

            if (Files.exists(csvFile)) {
                System.out.println("Processing " + csvFile.getFileName() + "...");
                processCSV(csvFile, outputDirectory, language);
            } else {
                System.out.println("Warning: " + csvFile.getFileName() + " not found, skipping...");
            }
        }
    }

    private static void processCSV(Path csvFile, Path outputDir, String language)
            throws IOException {
        Map<String, Properties> translationsByBase = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Skip header
            if (line == null || !line.equals("TranslationBase,key,value")) {
                throw new IOException("Invalid CSV header in " + csvFile);
            }

            StringBuilder currentRecord = new StringBuilder();
            boolean inQuotedField = false;

            while ((line = reader.readLine()) != null) {
                if (currentRecord.length() > 0) {
                    currentRecord.append('\n');
                }
                currentRecord.append(line);

                // Check if we're in a complete record
                inQuotedField = isInQuotedField(currentRecord.toString());

                if (!inQuotedField) {
                    // We have a complete record, parse it
                    String[] parts = parseCSVRecord(currentRecord.toString());
                    if (parts.length >= 3) {
                        String translationBase = parts[0];
                        String key = parts[1];
                        String value = parts[2];

                        // Don't convert escape sequences - keep them as-is for properties files

                        translationsByBase
                                .computeIfAbsent(translationBase, k -> new Properties())
                                .setProperty(key, value);
                    }
                    currentRecord.setLength(0);
                }
            }
        }

        // Write properties files
        for (Map.Entry<String, Properties> entry : translationsByBase.entrySet()) {
            writePropertiesFile(entry.getKey(), entry.getValue(), outputDir, language);
        }
    }

    private static boolean isInQuotedField(String record) {
        boolean inQuotes = false;
        boolean escapeNext = false;

        for (int i = 0; i < record.length(); i++) {
            char c = record.charAt(i);

            if (escapeNext) {
                escapeNext = false;
                continue;
            }

            if (c == '"') {
                if (inQuotes && i + 1 < record.length() && record.charAt(i + 1) == '"') {
                    escapeNext = true;
                } else {
                    inQuotes = !inQuotes;
                }
            }
        }

        return inQuotes;
    }

    private static String[] parseCSVRecord(String record) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;

        for (int i = 0; i < record.length(); i++) {
            char c = record.charAt(i);

            if (escapeNext) {
                current.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '"') {
                if (inQuotes && i + 1 < record.length() && record.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // Skip the next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private static void writePropertiesFile(
            String translationBase, Properties props, Path outputDir, String language)
            throws IOException {
        String fileName = getFileName(translationBase, language);
        Path filePath = getFilePath(outputDir, translationBase, fileName);

        // Ensure directory exists
        Files.createDirectories(filePath.getParent());

        try (PrintWriter writer =
                new PrintWriter(Files.newBufferedWriter(filePath, StandardCharsets.UTF_8))) {

            // Sort keys for consistent output
            List<String> sortedKeys = new ArrayList<>(props.stringPropertyNames());
            Collections.sort(sortedKeys);

            for (String key : sortedKeys) {
                String value = props.getProperty(key);
                writer.println(key + "=" + escapePropertiesValue(value));
            }

            writer.println(); // Add trailing newline
        }

        System.out.println("Created: " + filePath);
    }

    private static String getFileName(String translationBase, String language) {
        String baseFileName =
                translationBase.contains("/")
                        ? translationBase.substring(translationBase.lastIndexOf('/') + 1)
                        : translationBase;

        if ("en".equals(language)) {
            return baseFileName + ".properties";
        } else {
            return baseFileName + "_" + language + ".properties";
        }
    }

    private static Path getFilePath(Path outputDir, String translationBase, String fileName) {
        if (translationBase.contains("/")) {
            String directory = translationBase.substring(0, translationBase.lastIndexOf('/'));
            return outputDir.resolve(directory).resolve(fileName);
        } else {
            return outputDir.resolve(fileName);
        }
    }

    private static String escapePropertiesValue(String value) {
        if (value == null) return "";

        // Convert \n\ sequences back to proper properties line continuation format
        if (value.contains("\\n\\")) {
            // Split by \n\ and rejoin with proper line continuation
            String[] parts = value.split("\\\\n\\\\");
            StringBuilder result = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                result.append("\\n\\\n\t\t\t  ").append(parts[i]);
            }
            return result.toString();
        }

        return value;
    }
}
