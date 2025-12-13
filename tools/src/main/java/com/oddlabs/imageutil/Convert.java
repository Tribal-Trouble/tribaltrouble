package com.oddlabs.imageutil;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.util.DXTImage;
import com.oddlabs.util.Image;
import io.github.memo33.jsquish.Squish;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
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

    static void main(String @NonNull ... args) throws IOException {
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
//		int channels = image.getRaster().getNumBands() <= 3 ? 3 : 4;
//		assert image.getColorModel() == ColorModel.getRGBdefault();
		int channels = image.getColorModel().getNumComponents();
//		final byte[] bytes = getImageData(image);
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

	private static void saveDxtn(@NonNull Path file, Layer @NonNull [] images) throws IOException {
		Squish.CompressionType type = switch (images[0].a) {
            case null -> Squish.CompressionType.DXT1;
            default -> Squish.CompressionType.DXT5;
        };
		byte[][] mipmap_bytes = new byte[images.length][];
		for (int i = 0; i < mipmap_bytes.length; i++) {
//images[i].saveAsPNG(new File(file.getParentFile(), images[i].getWidth() + "x" + images[i].getHeight() + "-" + file.getName() + ".png"));
			byte[] mipmap = images[i].convertToBytes();
			mipmap_bytes[i] = Squish.compressImage(mipmap, images[i].getWidth(), images[i].getHeight(), null, type, Squish.CompressionMethod.CLUSTER_FIT);
/*System.out.println("Decompressing............");
			byte[] decompressed = Squish.decompressImage(null, images[i].getWidth(), images[i].getHeight(), mipmap_bytes[i], type);
System.out.println("Done");*/
		}
		int internal_format = switch (type) {
			case DXT1 -> 0x83F0; // GL_COMPRESSED_RGB_S3TC_DXT1_EXT
			case DXT5 -> 0x83F3; // GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
			default -> throw new IllegalArgumentException("Unsupported compression type: " + type);
		};
		new DXTImage((short)images[0].getWidth(),(short)images[0].getHeight(), internal_format, mipmap_bytes).write(file);
	}

	private static void save(@NonNull Path file, Layer @NonNull [] images) throws IOException {
		if (file.getFileName().toString().endsWith(".dxtn")) {
			saveDxtn(file, images);
		} else if (file.getFileName().toString().endsWith(".image")) {
			saveImage(file, images);
		} else
			throw new IllegalArgumentException("unknown extension: " + file);
	}

/*
		String filename = new File(infile).getName();
		filename = filename.substring(0, filename.lastIndexOf("."));
		System.out.println("Converting " + infile);
		BufferedImage image = ImageIO.read(new File(infile));
		int width = image.getWidth();
		int height = image.getHeight();
		int channels = image.getRaster().getNumBands() <= 3 ? 3 : 4;
		final byte[] bytes = getImageData(image);
		Utils.flip(bytes, width*4, height);
		Layer image_layer = new Layer(width, height);
		if (channels == 4)
			image_layer.a = new Channel(width, height);
		image_layer.loadFromBytes(bytes);
image_layer.saveAsPNG(filename + "." + width + "x" + height + "p");
		image_layer.gamma(Layer.INV_GAMMA_EXPONENT);
		List mipmaps = new ArrayList();
		final byte[] blocks = compressImage(channels, width, height, bytes, Squish.CompressionMethod.CLUSTER_FIT);
		mipmaps.add(blocks);
		int mip_width = width;
		int mip_height = height;
		Layer mipmap_layer = image_layer;
		while (mip_width > 1 && mip_height > 1) {
			mip_width /=2;
			mip_height /= 2;
			mipmap_layer.scaleHalf();
			Layer mipmap_layer_export = mipmap_layer.copy();
			mipmap_layer_export.gamma(Layer.GAMMA_EXPONENT);
			byte[] mipmap = mipmap_layer_export.convertToBytes();
mipmap_layer.saveAsPNG(filename + "." + mip_width + "x" + mip_height + "p");
			byte[] mipmap_blocks = compressImage(channels, mip_width, mip_height, mipmap, Squish.CompressionMethod.CLUSTER_FIT);
			mipmaps.add(mipmap_blocks);
		}
		String outfile = dstdir + File.separatorChar + filename + ".dxtn";
		int internal_format;
		if (channels == 3) {
			internal_format = EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
		} else {
			internal_format = EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
		}
		new DXTImage((short)width,(short)height, internal_format, (byte[][])mipmaps.toArray(new byte[][]{})).write(new File(outfile));
	}
*/
	private static byte[] getImageData(@NonNull BufferedImage image) {

		final int type = image.getType();

		if ( type != BufferedImage.TYPE_3BYTE_BGR && type != BufferedImage.TYPE_4BYTE_ABGR ) {
			// Bored to do it right, let Java2D do the conversion for us
			final BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getRaster().getNumBands() <= 3 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_4BYTE_ABGR);
			final Graphics2D g2 = newImage.createGraphics();

			g2.drawImage(image, null, 0, 0);

			image = newImage;
		}

		final Raster raster = image.getRaster();
		byte[] data = ((DataBufferByte)raster.getDataBuffer()).getData();

		if ( raster.getNumBands() == 3 ) {
			final byte[] bytes = new byte[image.getWidth() * image.getHeight() * 4];

			for ( int i = 0, j = 0; i < data.length; ) {
				final byte b = data[i++];
				final byte g = data[i++];
				final byte r = data[i++];

				bytes[j++] = r;
				bytes[j++] = g;
				bytes[j++] = b;
				bytes[j++] = (byte)0xFF;
			}

			data = bytes;
		} else {
			for ( int i = 0; i < data.length; ) {
				final byte a = data[i + 0];
				final byte b = data[i + 1];
				final byte g = data[i + 2];
				final byte r = data[i + 3];

				data[i + 0] = r;
				data[i + 1] = g;
				data[i + 2] = b;
				data[i + 3] = a;

				i += 4;
			}
		}

		return data;
	}
}
