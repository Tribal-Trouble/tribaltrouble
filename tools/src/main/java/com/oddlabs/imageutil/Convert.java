package com.oddlabs.imageutil;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CLI Driver for TextureProcessor.
 * Automatically determines batch or single mode based on input path.
 */
public final class Convert {

    public static void main(@NonNull String @NonNull [] args) {
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
                System.out.println("Converting " + input + " -> " + output);
                TextureProcessor.processFile(input, operations, output);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
