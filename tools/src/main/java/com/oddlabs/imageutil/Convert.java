package com.oddlabs.imageutil;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * CLI Driver for TextureProcessor.
 * Automatically determines batch or single mode based on input path.
 */
public final class Convert {

    static void main(@NonNull String @NonNull... args) {
        if (args.length < 2) {
            System.err.println("Usage: Convert <infile/indir> [operations...] <outfile/outdir>");
            System.exit(1);
        }

        try {
            Path input = Path.of(args[0]);
            Path output = Path.of(args[args.length - 1]);
            List<String> operations = new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1));

            if (Files.isDirectory(input)) {
                if (Files.exists(output) && !Files.isDirectory(output)) {
                    System.err.println("Input is a directory, but output is an existing file: " + output);
                    System.exit(1);
                }
                TextureProcessor.processBatch(input, operations, output);
            } else {
                if (Files.isDirectory(output)) {
                    System.err.println("Input is a file, but output is a directory: " + output);
                    System.exit(1);
                }
                IO.println("Converting " + input + " -> " + output);
                TextureProcessor.processFiles(Stream.of(new TextureProcessor.SourceTarget(input, output)), operations);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
