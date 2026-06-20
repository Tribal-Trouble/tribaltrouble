package com.oddlabs.imageutil;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.util.DXTImage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBDXT;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Utility class for processing textures (loading, transforming, and saving as DDS).
 */
public final class TextureProcessor {

    private enum UsageMode {
        COLOR_SRGB,
        DATA_LINEAR
    }

    public record SourceTarget(@NonNull Path source, @NonNull Path target) {
    }

    private TextureProcessor() {
    }

    /**
     * Processes a single file to a specific output file.
     */
    public static void processFile(@NonNull Path infile, @NonNull List<String> operations, @NonNull Path outfile)
            throws IOException {
        String basisuPath = System.getProperty("basisu.path");
        UsageMode usageMode = determineMode(infile, operations);

        boolean forceStb = operations.contains("-stb");

        if (basisuPath != null && outfile.toString().endsWith(".dds") && !forceStb) {
            // High quality path using basisu CLI
            processWithBasisu(infile, operations, outfile, basisuPath, usageMode);
        } else {
            // Standard fallback path using STB (High quality DXT5/1)
            Layer[] images = new Layer[]{loadFile(infile)};
            images = applyOperations(Arrays.asList(operations.toArray(new String[0])).iterator(), images, infile);

            Files.createDirectories(outfile.getParent());
            save(outfile, images);
        }
    }

    private static void processWithBasisu(@NonNull Path infile, @NonNull List<String> operations, @NonNull Path outfile,
            @NonNull String basisuPath, @NonNull UsageMode usageMode) throws IOException {
        Path workDir = Files.createTempDirectory("basisu_work");
        try {
            // copy input to workDir to have a clean, known name
            Path inputCopy = workDir.resolve("input.png");
            Files.copy(infile, inputCopy);

            // Step 0: Check for alpha to know whether to look for BC1 or BC3
            Layer source = loadFile(infile);
            boolean hasAlpha = source.a != null;

            Path tempKtx2 = workDir.resolve("input.ktx2");

            // Step 1: Compress to .ktx2
            List<String> compressCmd = new ArrayList<>();
            compressCmd.add(basisuPath);
            compressCmd.add("-file");
            compressCmd.add(inputCopy.toAbsolutePath().toString());
            compressCmd.add("-output_file");
            compressCmd.add(tempKtx2.toAbsolutePath().toString());
            compressCmd.add("-ktx2");

            boolean mipmaps = false;
            boolean flip = false;
            boolean uastc = false;
            for (int i = 0; i < operations.size(); i++) {
                String op = operations.get(i);
                if ("-mipmaps".equals(op)) mipmaps = true;
                if ("-flip".equals(op)) flip = true;
                if ("-uastc".equals(op)) uastc = true;
                if ("-gamma".equals(op)) i++; // skip value
            }

            if (mipmaps) {
                compressCmd.add("-mipmap");
                compressCmd.add(usageMode == UsageMode.COLOR_SRGB ? "-mip_srgb" : "-mip_linear");
            }
            // basisu defaults to no mipmaps, so we don't need -no_mipmap (which is invalid)

            if (flip) compressCmd.add("-y_flip");

            if (usageMode == UsageMode.DATA_LINEAR) {
                compressCmd.add("-linear");
            }

            if (uastc) {
                compressCmd.add("-uastc");
                // UASTC level 2 is a good balance for high quality UI
                compressCmd.add("-uastc_level");
                compressCmd.add("2");
            } else {
                // Use highest ETC1S quality level that still transcodes reliably to BC1/BC3.
                compressCmd.add("-comp_level");
                compressCmd.add("5");
            }

            execute(compressCmd, workDir);

            // Step 2: Unpack to standard DDS.
            // We force BC1/BC3 output to ensure maximum compatibility with older GL drivers.
            List<String> unpackCmd = new ArrayList<>();
            unpackCmd.add(basisuPath);
            unpackCmd.add("-unpack");
            unpackCmd.add("-file");
            unpackCmd.add(tempKtx2.toAbsolutePath().toString());

            execute(unpackCmd, workDir);

            // Find the produced .dds (BC1 or BC3)
            String targetSuffix = hasAlpha ? "BC3_RGBA" : "BC1_RGB";
            Path unpackedDds = null;
            try (Stream<Path> s = Files.list(workDir)) {
                unpackedDds = s.filter(p -> {
                    String name = p.getFileName().toString();
                    return name.endsWith(".dds") && name.contains(targetSuffix);
                }).findFirst().orElse(null);
            }

            if (unpackedDds == null) {
                // Fallback: search for ANY .dds in the work directory
                try (Stream<Path> s = Files.list(workDir)) {
                    unpackedDds = s.filter(p -> p.toString().endsWith(".dds")).findFirst().orElse(null);
                }
            }

            if (unpackedDds == null || !Files.exists(unpackedDds)) {
                throw new IOException("Basisu failed to create any DDS output in " + workDir);
            }

            Files.createDirectories(outfile.getParent());
            Files.move(unpackedDds, outfile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        } finally {
            deleteDirectory(workDir);
        }
    }

    private static void deleteDirectory(@NonNull Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> s = Files.walk(path)) {
                s.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // ignore
                    }
                });
            }
        }
    }

    private static void execute(@NonNull List<String> command, @NonNull Path workingDir) throws IOException {
        try (Process process = new ProcessBuilder(command)
                .directory(workingDir.toFile())
                .start()) {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Basisu command failed in " + workingDir + ": " + String.join(" ", command));
                process.getErrorStream().transferTo(System.err);
                throw new IOException("basisu failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("basisu execution interrupted", e);
        }
    }

    /**
     * Processes all PNG files in a directory into an output directory.
     */
    public static void processBatch(@NonNull Path inputDir, @NonNull List<@NonNull String> operations,
            @NonNull Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        try (Stream<Path> stream = Files.list(inputDir)) {
            processFiles(stream, operations, outputDir);
        }
    }

    public static void processFiles(@NonNull Stream<Path> stream, @NonNull List<@NonNull String> operations,
            @NonNull Path outputDir) throws IOException {
        String format = "dds";
        // Check for -format in operations
        for (int i = 0; i < operations.size(); i++) {
            if ("-format".equals(operations.get(i)) && i + 1 < operations.size()) {
                format = operations.get(i + 1);
                break;
            }
        }

        var finalFormat = format;
        try {
            processFiles(stream.filter(Files::isRegularFile)
                    .filter(sourceFile -> sourceFile.toString().endsWith(".png"))
                    .map(sourceFile -> {
                        var filename = sourceFile.getFileName().toString();
                        String baseName = filename.substring(0, filename.lastIndexOf('.'));
                        return new SourceTarget(sourceFile, outputDir.resolve(baseName + "." + finalFormat));
                    }), operations);
        } catch (UncheckedIOException uioe) {
            throw uioe.getCause();
        }
    }

    public static void processFiles(@NonNull Stream<@NonNull SourceTarget> stream, @NonNull List<String> operations)
            throws IOException {
        try {
            stream.filter(st -> {
                try {
                    // check if target is absent or if the source file is newer than the target
                    return !Files.exists(st.target) || Files.getLastModifiedTime(st.source).compareTo(Files
                            .getLastModifiedTime(st.target)) >= 0;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
                    .parallel()
                    .forEach(st -> {
                        try {
                            IO.println("Batch processing: " + st.source.getFileName() + " -> " + st.target
                                    .getFileName());
                            processFile(st.source, operations, st.target);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to process " + st.source, e);
                        }
                    });
        } catch (UncheckedIOException uioe) {
            throw uioe.getCause();
        }
    }

    private static Layer @NonNull [] applyOperations(@NonNull Iterator<String> args, Layer @NonNull [] images,
            @NonNull Path infile) {
        UsageMode usageMode = inferUsageFromName(infile);
        while (args.hasNext()) {
            String op = args.next();
            if ("-usage".equals(op) && args.hasNext()) {
                String value = args.next().toLowerCase(Locale.ROOT);
                if ("data".equals(value) || "linear".equals(value)) {
                    usageMode = UsageMode.DATA_LINEAR;
                } else if ("color".equals(value) || "srgb".equals(value)) {
                    usageMode = UsageMode.COLOR_SRGB;
                }
                continue;
            }
            if ("-data".equals(op)) {
                usageMode = UsageMode.DATA_LINEAR;
                continue;
            }
            if ("-color".equals(op)) {
                usageMode = UsageMode.COLOR_SRGB;
                continue;
            }
            images = applyOperation(op, args, images, usageMode);
        }
        return images;
    }

    private static Layer @NonNull [] applyOperation(@NonNull String op, @NonNull Iterator<String> args,
            Layer @NonNull [] images, @NonNull UsageMode usageMode) {
        boolean isData = usageMode == UsageMode.DATA_LINEAR;

        switch (op) {
            case "-mipmaps" -> {
                if (images.length != 1)
                    throw new IllegalArgumentException("Can only create mipmaps from one image, not " + images.length);
                List<Layer> mipmaps = new ArrayList<>();
                Layer current = images[0];
                mipmaps.add(current);
                while (current.getWidth() > 1 || current.getHeight() > 1) {
                    current = current.copy();
                    if (isData) {
                        current.scaleHalf();
                    } else {
                        current.scaleHalfLinear();
                    }
                    mipmaps.add(current);
                }
                images = mipmaps.toArray(new Layer[0]);
            }
            case "-half" -> {
                for (Layer image : images) {
                    image.scaleHalf();
                }
            }
            case "-format" -> args.next(); // Skip, used for extension determination only
            case "-flip" -> {
                for (Layer image : images) {
                    image.flipV();
                }
            }
            case "-uastc" -> {
                // Basis-specific, ignored in fallback path
            }
            case "-stb" -> {
                // Forcing STB path, handled in processFile
            }
            case "-gamma" -> {
                String gamma_str = args.next();
                float gamma = Float.parseFloat(gamma_str);
                for (Layer image : images) {
                    image.gamma(gamma);
                }
            }
            case "-bgra" -> {
                for (Layer image : images) {
                    Channel temp = image.r;
                    image.r = image.b;
                    image.b = temp;
                }
            }
            case "-argb" -> {
                for (Layer image : images) {
                    Channel old_r = image.r;
                    Channel old_g = image.g;
                    Channel old_b = image.b;
                    Channel old_a = image.a;
                    if (old_a == null) {
                        old_a = new Channel(image.getWidth(), image.getHeight());
                        old_a.fill(1.0f);
                    }
                    image.r = old_a;
                    image.g = old_r;
                    image.b = old_g;
                    image.a = old_b;
                }
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + op);
        }
        return images;
    }

    private static @NonNull UsageMode determineMode(@NonNull Path infile, @NonNull List<String> operations) {
        UsageMode usageMode = parseExplictMode(operations);
        return usageMode != null ? usageMode : inferUsageFromName(infile);
    }

    private static @Nullable UsageMode parseExplictMode(@NonNull List<String> operations) {
        for (int i = 0; i < operations.size(); i++) {
            String op = operations.get(i);
            if ("-data".equals(op)) {
                return UsageMode.DATA_LINEAR;
            }
            if ("-color".equals(op)) {
                return UsageMode.COLOR_SRGB;
            }
            if ("-usage".equals(op) && i + 1 < operations.size()) {
                String value = operations.get(i + 1).toLowerCase(Locale.ROOT);
                if ("data".equals(value) || "linear".equals(value)) {
                    return UsageMode.DATA_LINEAR;
                }
                if ("color".equals(value) || "srgb".equals(value)) {
                    return UsageMode.COLOR_SRGB;
                }
            }
        }
        return null;
    }

    private static @NonNull UsageMode inferUsageFromName(@NonNull Path infile) {
        String lowerName = infile.getFileName().toString().toLowerCase(Locale.ROOT);
        return lowerName.contains("normal") || lowerName.contains("bump") || lowerName.contains("mica")
                ? UsageMode.DATA_LINEAR
                : UsageMode.COLOR_SRGB;
    }

    public static @NonNull Layer loadFile(@NonNull Path file) throws IOException {
        try (var in = new BufferedInputStream(Files.newInputStream(file))) {
            BufferedImage image = ImageIO.read(in);
            int width = image.getWidth();
            int height = image.getHeight();
            int channels = image.getColorModel().getNumComponents();
            int[] ints = new int[width * height];
            image.getRGB(0, 0, width, height, ints, 0, width);
            byte[] bytes = new byte[width * height * 4];
            int index = 0;
            for (int argb : ints) {
                byte a = (byte) ((argb >> 24) & 0xff);
                byte r = (byte) ((argb >> 16) & 0xff);
                byte g = (byte) ((argb >> 8) & 0xff);
                byte b = (byte) ((argb) & 0xff);
                bytes[index++] = r;
                bytes[index++] = g;
                bytes[index++] = b;
                bytes[index++] = a;
            }
            Layer layer = new Layer(width, height);
            if (channels == 4) {
                layer.a = new Channel(width, height);
            }
            layer.loadFromBytes(bytes);
            return layer;
        }
    }

    public static void saveDDS(@NonNull Path file, @NonNull Layer @NonNull [] images) throws IOException {
        int width = images[0].getWidth();
        int height = images[0].getHeight();
        boolean hasAlpha = images[0].a != null;
        int fourCC = hasAlpha ? DXTImage.FOURCC_DXT5 : DXTImage.FOURCC_DXT1;

        byte[][] mipmap_bytes = new byte[images.length][];
        ByteBuffer blockBuffer = MemoryUtil.memAlloc(64);

        try {
            for (int i = 0; i < images.length; i++) {
                Layer layer = images[i];
                int w = layer.getWidth();
                int h = layer.getHeight();
                byte[] rgba = layer.convertToBytes();

                int blockSize = hasAlpha ? 16 : 8;
                int numBlocksX = (w + 3) / 4;
                int numBlocksY = (h + 3) / 4;
                int compressedSize = numBlocksX * numBlocksY * blockSize;

                ByteBuffer compressedBuffer = MemoryUtil.memAlloc(compressedSize);

                try {
                    for (int by = 0; by < numBlocksY; by++) {
                        for (int bx = 0; bx < numBlocksX; bx++) {
                            blockBuffer.clear();
                            for (int py = 0; py < 4; py++) {
                                int sy = Math.min(by * 4 + py, h - 1);
                                for (int px = 0; px < 4; px++) {
                                    int sx = Math.min(bx * 4 + px, w - 1);
                                    int srcIdx = (sy * w + sx) * 4;
                                    blockBuffer.put(rgba[srcIdx]);
                                    blockBuffer.put(rgba[srcIdx + 1]);
                                    blockBuffer.put(rgba[srcIdx + 2]);
                                    blockBuffer.put(rgba[srcIdx + 3]);
                                }
                            }
                            blockBuffer.flip();

                            long compressedBlockAddr = MemoryUtil.memAddress(compressedBuffer) + (long) (by * numBlocksX
                                    + bx) * blockSize;
                            STBDXT.nstb_compress_dxt_block(
                                    compressedBlockAddr,
                                    MemoryUtil.memAddress(blockBuffer),
                                    hasAlpha ? 1 : 0,
                                    STBDXT.STB_DXT_HIGHQUAL
                            );
                        }
                    }
                    byte[] compressedData = new byte[compressedSize];
                    compressedBuffer.get(compressedData);
                    mipmap_bytes[i] = compressedData;
                } finally {
                    MemoryUtil.memFree(compressedBuffer);
                }
            }
        } finally {
            MemoryUtil.memFree(blockBuffer);
        }

        new DXTImage((short) width, (short) height, fourCC, mipmap_bytes).write(file);
    }

    private static void save(@NonNull Path file, @NonNull Layer @NonNull [] images) throws IOException {
        String filename = file.getFileName().toString();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        switch (extension) {
            case "dds" -> saveDDS(file, images);
            case "png" -> {
                if (images.length != 1)
                    throw new IllegalArgumentException("Can't save more than 1 image in .png format");
                images[0].saveAsPNG(file);
            }
            default -> throw new IllegalArgumentException("Unknown image extension: " + extension);
        }
    }
}
