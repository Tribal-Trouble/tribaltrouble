package com.oddlabs.imageutil;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.util.DXTImage;
import org.jspecify.annotations.NonNull;
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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class for processing textures (loading, transforming, and saving as DDS).
 */
public final class TextureProcessor {

    private TextureProcessor() {}

    /**
     * Processes a single file to a specific output file.
     */
    public static void processFile(@NonNull Path infile, @NonNull List<String> operations, @NonNull Path outfile) throws IOException {
        if (Files.exists(outfile) && Files.getLastModifiedTime(outfile).compareTo(Files.getLastModifiedTime(infile)) >= 0) {
            return;
        }

        Layer[] images = new Layer[]{loadFile(infile)};
        images = applyOperations(Arrays.asList(operations.toArray(new String[0])).iterator(), images);

        Files.createDirectories(outfile.getParent());
        save(outfile, images);
    }

    /**
     * Processes all PNG files in a directory into an output directory.
     */
    public static void processBatch(@NonNull Path inputDir, @NonNull List<String> operations, @NonNull Path outputDir) throws IOException {
        String format = "dds";
        // Check for -format in operations
        for (int i = 0; i < operations.size(); i++) {
            if ("-format".equals(operations.get(i)) && i + 1 < operations.size()) {
                format = operations.get(i + 1);
                break;
            }
        }

        Files.createDirectories(outputDir);
        final String finalFormat = format;
        try (Stream<Path> stream = Files.list(inputDir)) {
            stream.filter(p -> p.toString().endsWith(".png")).forEach(p -> {
                try {
                    String baseName = p.getFileName().toString().substring(0, p.getFileName().toString().lastIndexOf('.'));
                    Path target = outputDir.resolve(baseName + "." + finalFormat);
                    System.out.println("Batch processing: " + p.getFileName() + " -> " + target.getFileName());
                    processFile(p, operations, target);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to process " + p, e);
                }
            });
        } catch (UncheckedIOException uioe) {
            throw uioe.getCause();
        }
    }

    private static Layer[] applyOperations(@NonNull Iterator<String> args, Layer[] images) {
        while (args.hasNext()) {
            String op = args.next();
            images = applyOperation(op, args, images);
        }
        return images;
    }

    private static Layer @NonNull [] applyOperation(@NonNull String op, @NonNull Iterator<String> args, Layer @NonNull [] images) {
        switch (op) {
            case "-mipmaps":
                if (images.length != 1)
                    throw new IllegalArgumentException("Can only create mipmaps from one image, not " + images.length);
                List<Layer> mipmaps = new ArrayList<>();
                Layer original_image = images[0];
                int mip_width = original_image.getWidth();
                int mip_height = original_image.getHeight();
                mipmaps.add(original_image);
                while (mip_width > 1 && mip_height > 1) {
                    mip_width /= 2;
                    mip_height /= 2;
                    Layer mipmap = original_image.copy();
                    mipmap.scaleCubicWrapping(mip_width, mip_height);
                    mipmaps.add(mipmap);
                }
                images = mipmaps.toArray(new Layer[0]);
                break;
            case "-half":
                for (Layer image : images) {
                    image.scaleHalf();
                }
                break;
            case "-format":
                args.next(); // Skip, used for extension determination only
                break;
            case "-flip":
                for (Layer image : images) {
                    image.flipV();
                }
                break;
            case "-gamma":
                String gamma_str = args.next();
                float gamma = Float.parseFloat(gamma_str);
                for (Layer image : images) {
                    image.gamma(gamma);
                }
                break;
            case "-bgra":
                for (Layer image : images) {
                    Channel temp = image.r;
                    image.r = image.b;
                    image.b = temp;
                }
                break;
            case "-argb":
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
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + op);
        }
        return images;
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

                            long compressedBlockAddr = MemoryUtil.memAddress(compressedBuffer) + (long) (by * numBlocksX + bx) * blockSize;
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

    private static void save(@NonNull Path file, Layer @NonNull [] images) throws IOException {
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
