package com.oddlabs.imageutil;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.util.DXTImage;
import com.oddlabs.util.Image;
import org.jspecify.annotations.NonNull;
import org.lwjgl.stb.STBDXT;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class Convert {
	private static String current_ext;

    static void main(@NonNull String @NonNull ... args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: Convert <infile> <operations> <outfile>");
			System.exit(1);
		}
		Path infile = Path.of(args[0]);
		Path outfile = Path.of(args[args.length - 1]);
        List<String> args_list = new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1));
		IO.println("Converting " + infile);
		Layer[] images = new Layer[]{loadFile(infile)};
		images = processOperations(args_list.iterator(), images);
		if (Files.exists(outfile)) {
			if (Files.isDirectory(outfile)) {
				String infilename = infile.getFileName().toString();
				int dot_index = infilename.lastIndexOf(".");
				outfile = outfile.resolve(infilename.substring(0, dot_index));
			}
		} else {
			Path parent = outfile.toAbsolutePath().getParent();
			Files.createDirectories(parent);
		}
		if (current_ext != null && !outfile.getFileName().toString().endsWith(current_ext)) {
			outfile = outfile.getParent().resolve(outfile.getFileName() + "." + current_ext);
		}
		IO.println("outfile = " + outfile);
		save(outfile, images);
	}

	private static Layer[] processOperations(@NonNull Iterator<@NonNull String> args, Layer[] images) {
		while (args.hasNext()) {
			String op = args.next();
			images = processOperation(op, args, images);
		}
		return images;
	}

	private static Layer @NonNull [] processOperation(@NonNull String op, @NonNull Iterator<String> args, Layer @NonNull [] images) {
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
                current_ext = args.next();
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

	private static @NonNull Layer loadFile(@NonNull Path file) throws IOException {
		try (var in = new BufferedInputStream(Files.newInputStream(file))) {
			return loadFile(in);
		}
	}

	private static @NonNull Layer loadFile(@NonNull InputStream source) throws IOException {
		BufferedImage image = ImageIO.read(source);
		int width = image.getWidth();
		int height = image.getHeight();
		int channels = image.getColorModel().getNumComponents();
		int[] ints = new int[width*height];
        image.getRGB(0, 0, width, height, ints, 0, width);
		byte[] bytes = new byte[width*height* 4 * Byte.BYTES];
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
		Layer image_layer = new Layer(width, height);
		if (channels == 4)
			image_layer.a = new Channel(width, height);
		image_layer.loadFromBytes(bytes);
		return image_layer;
	}

	private static void saveImage(@NonNull Path file, Layer @NonNull [] images) throws IllegalArgumentException, IOException {
		if (images.length != 1)
			throw new IllegalArgumentException("Can't save more than 1 image in .image format");
		byte[] bytes = images[0].convertToBytes();
		Image image = new Image(images[0].getWidth(), images[0].getHeight(), ByteBuffer.wrap(bytes));
		try {
			image.write(file);
		} catch (UncheckedIOException uioe) {
			throw uioe.getCause();
		}
	}

	private static void saveDDS(@NonNull Path file, @NonNull Layer @NonNull [] images) throws IOException {
        int width = images[0].getWidth();
        int height = images[0].getHeight();
        boolean hasAlpha = images[0].a != null;
        int fourCC = hasAlpha ? DXTImage.FOURCC_DXT5 : DXTImage.FOURCC_DXT1;
        
        byte[][] mipmap_bytes = new byte[images.length][];
        
        // Allocate a single block buffer to be reused for 4x4 RGBA pixels (4 * 4 * 4 bytes = 64 bytes)
        ByteBuffer blockBuffer = MemoryUtil.memAlloc(64);

        try {
            for (int i = 0; i < images.length; i++) {
                Layer layer = images[i];
                int w = layer.getWidth();
                int h = layer.getHeight();
                byte[] rgba = layer.convertToBytes();
                
                // DXT1/5 blocks are 4x4 pixels. 
                // DXT1 is 8 bytes per block, DXT5 is 16 bytes per block.
                int blockSize = hasAlpha ? 16 : 8;
                int numBlocksX = (w + 3) / 4;
                int numBlocksY = (h + 3) / 4;
                int compressedSize = numBlocksX * numBlocksY * blockSize;
                
                ByteBuffer compressedBuffer = MemoryUtil.memAlloc(compressedSize);
                
                try {
                    for (int by = 0; by < numBlocksY; by++) {
                        for (int bx = 0; bx < numBlocksX; bx++) {
                            // Extract 4x4 block into blockBuffer
                            blockBuffer.clear();
                            for (int py = 0; py < 4; py++) {
                                int sy = by * 4 + py;
                                if (sy >= h) sy = h - 1; // Clamp height
                                
                                for (int px = 0; px < 4; px++) {
                                    int sx = bx * 4 + px;
                                    if (sx >= w) sx = w - 1; // Clamp width
                                    
                                    int srcIdx = (sy * w + sx) * 4;
                                    
                                    blockBuffer.put(rgba[srcIdx]);     // R
                                    blockBuffer.put(rgba[srcIdx + 1]); // G
                                    blockBuffer.put(rgba[srcIdx + 2]); // B
                                    blockBuffer.put(rgba[srcIdx + 3]); // A
                                }
                            }
                            blockBuffer.flip();

                            long compressedBlockAddr = MemoryUtil.memAddress(compressedBuffer) + (long) (by * numBlocksX + bx) * blockSize;

                            // Compress
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
        
		new DXTImage((short)width,(short)height, fourCC, mipmap_bytes).write(file);
	}

	private static void save(@NonNull Path file, Layer @NonNull [] images) throws IOException {
		if (file.getFileName().toString().endsWith(".dds")) {
			saveDDS(file, images);
		} else if (file.getFileName().toString().endsWith(".image")) {
			saveImage(file, images);
		} else
			throw new IllegalArgumentException("unknown extension: " + file);
	}
}
