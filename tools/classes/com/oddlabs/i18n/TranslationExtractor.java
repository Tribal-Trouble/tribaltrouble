package com.oddlabs.i18n;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public final strictfp class TranslationExtractor {
    private static final String[] SUPPORTED_LANGUAGES = {"en", "da", "de", "es", "it"};
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("_(da|de|es|it)$");

    public static final void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: TranslationExtractor <i18n_path> <output_dir>");
            System.exit(1);
        }

        String i18nPath = args[0];
        String outputDir = args[1];

        try {
            extractTranslations(i18nPath, outputDir);
        } catch (IOException e) {
            System.err.println("Error extracting translations: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void extractTranslations(String i18nPath, String outputDir) throws IOException {
        Path i18nDir = Paths.get(i18nPath);
        Path outputPath = Paths.get(outputDir);

        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        // Find all properties files
        Map<String, List<Path>> languageFiles = new HashMap<>();
        for (String lang : SUPPORTED_LANGUAGES) {
            languageFiles.put(lang, new ArrayList<>());
        }

        Files.walk(i18nDir)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".properties"))
            .forEach(path -> {
                String fileName = path.getFileName().toString();
                String lang = extractLanguage(fileName);
                languageFiles.get(lang).add(path);
            });

        // Generate CSV for each language
        for (String lang : SUPPORTED_LANGUAGES) {
            List<Path> files = languageFiles.get(lang);
            if (!files.isEmpty()) {
                generateCSV(lang, files, i18nDir, outputPath);
                System.out.println("Generated translations_" + lang + ".csv with " + files.size() + " translation files");
            }
        }
    }

    private static String extractLanguage(String fileName) {
        if (fileName.contains("_da.properties")) return "da";
        if (fileName.contains("_de.properties")) return "de";
        if (fileName.contains("_es.properties")) return "es";
        if (fileName.contains("_it.properties")) return "it";
        return "en";
    }

    private static void generateCSV(String language, List<Path> files, Path i18nRoot, Path outputDir) throws IOException {
        Path csvFile = outputDir.resolve("translations_" + language + ".csv");

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8))) {
            writer.println("TranslationBase,key,value");

            // Sort files for consistent output
            files.sort(Comparator.comparing(Path::toString));

            for (Path file : files) {
                String translationBase = getTranslationBasePath(file, i18nRoot);
                Properties props = loadProperties(file);

                // Sort keys for consistent output
                List<String> sortedKeys = new ArrayList<>(props.stringPropertyNames());
                Collections.sort(sortedKeys);

                for (String key : sortedKeys) {
                    String value = props.getProperty(key);
                    writer.println(escapeCSV(translationBase) + "," +
                                 escapeCSV(key) + "," +
                                 escapeCSV(value));
                }
            }
        }
    }

    private static String getTranslationBasePath(Path file, Path i18nRoot) {
        Path relativePath = i18nRoot.relativize(file);
        String pathStr = relativePath.toString().replace('\\', '/');

        // Remove .properties extension
        if (pathStr.endsWith(".properties")) {
            pathStr = pathStr.substring(0, pathStr.length() - 11);
        }

        // Remove language suffix
        pathStr = LANGUAGE_PATTERN.matcher(pathStr).replaceAll("");

        return pathStr;
    }

    private static Properties loadProperties(Path file) throws IOException {
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            props.load(input);
        }
        return props;
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";

        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}