package com.oddlabs.fontutil;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.util.FontInfo;
import com.oddlabs.util.HashTable;
import com.oddlabs.util.Quad;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

public final class FontRenderer {
	private static final int GLYPH_X_BORDER = 4;
	private static final int GLYPH_Y_BORDER = 3;
	private static final int GLYPH_X_OVERLAP = 7;
	private static final int GLYPH_Y_OVERLAP = 5;
	private static final float SPACE_SCALE = 0.66666f;

	static void main(@NonNull String @NonNull ... args) {
        if (args.length < 9) {
			IO.println("FontRenderer <font_name> <font_size> <max_image_width> <max_chars> <scale_factor> <font_info_dir> <font_tex_dir> <font_tex_classpath> <additional_chars>");
        }
        try {
			int[] codepoints = IntStream.concat(IntStream.range(0, Integer.parseInt(args[3])), args[8].codePoints())
					.toArray();

			new FontRenderer(Path.of(args[0]),
					Integer.parseInt(args[1]), Float.parseFloat(args[4]),
					Integer.parseInt(args[2]), codepoints,
					Path.of(args[5]), Path.of(args[6]), args[7]);
            IO.println("Conversion complete\n");
        } catch(Throwable all) {
            IO.println("Conversion failed");
            all.printStackTrace(System.err);
            System.exit(1);
        }
	}

	public FontRenderer(@NonNull Path font_file,
						int logical_font_size, float scale_factor,
						int max_image_size, int @NonNull [] codepoints,
						@NonNull Path font_info_dir, @NonNull Path font_tex_dir,
						@NonNull String font_tex_classpath) throws Exception {
		String font_file_name = font_file.getFileName().toString();
		int extension = font_file_name.lastIndexOf('.');
		String src_font_name = extension != -1 ?  font_file_name.substring(0, extension) : font_file_name;
		
        int physical_font_size = Math.round(logical_font_size * scale_factor);
        
        IO.println("Rendering " + codepoints.length + " codepoints of " + src_font_name + " size " + logical_font_size + " (phys: " + physical_font_size + ")");
		String dest_font_name = src_font_name.toLowerCase();
        java.awt.Font src_font;
		try (InputStream font_is = new BufferedInputStream(Files.newInputStream(font_file))) {
            src_font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, font_is).deriveFont((float)physical_font_size);
        }

        int scaled_x_border = Math.round(GLYPH_X_BORDER * scale_factor);
        int scaled_y_border = Math.round(GLYPH_Y_BORDER * scale_factor);

		// calculate space width
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D)image.getGraphics();
		g2d.setFont(src_font);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		FontRenderContext frc = g2d.getFontRenderContext();
		char[] current_char = new char[] {'m', ' ', 'm'};
		GlyphVector gv = src_font.createGlyphVector(frc, current_char);

		Shape glyph_shape0 = gv.getGlyphOutline(0);
		Rectangle2D glyph_bounds0 = glyph_shape0.getBounds2D();

		Shape glyph_shape2 = gv.getGlyphOutline(2);
		Rectangle2D glyph_bounds2 = glyph_shape2.getBounds2D();
		float space_width_f= (int)Math.ceil(glyph_bounds2.getMinX()) - (int)Math.floor(glyph_bounds0.getMaxX());
		int space_width = (int)Math.ceil(space_width_f*SPACE_SCALE) + 2*scaled_x_border;
		IO.println("space_width: " + space_width);

		// calculate optimal image width and height
		int min_area = Integer.MAX_VALUE;
		int best_width = 0;
		int best_height = 0;
		int image_width = max_image_size;
		int image_height = 0;
		int[] heights = null;
		while (image_width > image_height) {
			heights = calculateImageHeight(src_font, image_width, space_width, codepoints, scaled_x_border, scaled_y_border);
			image_height = heights[0];
			int area = image_width*image_height;
			if (area <= min_area) {
				best_width = image_width;
				best_height = image_height;
				min_area = area;
			}
			image_width = image_width>>1;
		}

		// draw font images
		IO.println("optimal width*height: " + best_width + "*" + best_height);
		int max_glyph_height = heights[1];
		int max_baseline_height = heights[2];
        int max_under_baseline_height = heights[3];
		Channel white_alpha = drawFont(src_font, font_tex_classpath, font_info_dir, dest_font_name, logical_font_size, scale_factor, max_glyph_height, max_baseline_height, max_under_baseline_height, best_width, best_height, space_width, codepoints, true, scaled_x_border, scaled_y_border);
		Channel shadow = drawFont(src_font, font_tex_classpath, font_info_dir, dest_font_name, logical_font_size, scale_factor, max_glyph_height, max_baseline_height, max_under_baseline_height, best_width, best_height, space_width, codepoints, false, scaled_x_border, scaled_y_border);

		Channel black = new Channel(white_alpha.getWidth(), white_alpha.getHeight()).fill(0f);
		Channel white = new Channel(white_alpha.getWidth(), white_alpha.getHeight()).fill(1f);
		shadow.gamma(0.5f);
		Channel black_alpha = shadow.copy();
		black_alpha.channelBlend(white, shadow.copy().offset(0, -1));
		black_alpha.channelBlend(white, shadow.copy().offset(-1, 0));
		black_alpha.channelBlend(white, shadow.copy().offset(1, 0));
		black_alpha.channelBlend(white, shadow.copy().offset(0, 1));
		black_alpha.smooth(1).smooth(1).offset(1, 1);
		Layer font_image = new Layer(black, black, black, black_alpha);
		Layer highlight = new Layer(white, white, white, white_alpha);
		font_image.layerBlend(highlight);

		font_image.saveAsPNG(font_tex_dir + File.separator + dest_font_name + "_" + logical_font_size);
	}

	private int[] calculateImageHeight(@NonNull Font src_font,
									   int image_width, int space_width,
									   int @NonNull [] codepoints,
									   int x_border, int y_border) {
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D)image.getGraphics();
		g2d.setFont(src_font);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		FontRenderContext frc = g2d.getFontRenderContext();

		int max_baseline_height = 0;
		int max_under_baseline_height = 0;
        int tallest_char = ' ';
        int lowest_char = ' ';
        int num_lines = 1;
		int current_x = 0;

		// place chars
		IO.println("Calculating char placement for width = " + image_width);
		IO.print("Progress...");
		for (int i = 0; i < codepoints.length; i++) {
			if (i % 1000 == 0) {
				IO.print(".");
			}
			int codepoint = codepoints[i];
			if (src_font.canDisplay(codepoint)) {
				GlyphVector gv = src_font.createGlyphVector(frc, Character.toChars(codepoint));
				Shape glyph_shape = gv.getGlyphOutline(0);
				Rectangle2D glyph_bounds = glyph_shape.getBounds2D();
				int min_x = (int)Math.floor(glyph_bounds.getMinX()) - x_border;
				int min_y = (int)Math.floor(glyph_bounds.getMinY()) - y_border;
				int max_x = (int)Math.ceil(glyph_bounds.getMaxX()) + x_border;
				int max_y = (int)Math.ceil(glyph_bounds.getMaxY()) + y_border;
				int baseline_height = -min_y;
				if (baseline_height > max_baseline_height) {
                    max_baseline_height = baseline_height;
                    tallest_char = codepoint;
                }
				int under_baseline_height = max_y;
				if (under_baseline_height > max_under_baseline_height) {
                    max_under_baseline_height = under_baseline_height;
                    lowest_char = codepoint;
                }
				int glyph_width = codepoint == 32 ? space_width : max_x - min_x;
                assert glyph_width <= image_width : "character too wide to fit in image";
				if (current_x + glyph_width > image_width) {
					current_x = 0;
					num_lines++;
				}
				current_x += glyph_width;
			}
		}
		IO.println("done.");
        IO.print(" tallest char='" + tallest_char + "'(\\u" + Integer.toHexString(tallest_char) + "):" + max_baseline_height);
        IO.println(" lowest char='" + lowest_char + "'(\\u" + Integer.toHexString(lowest_char) + "):" + max_under_baseline_height);
		int max_glyph_height = max_under_baseline_height + max_baseline_height;
		int image_height = Utils.nextPowerOf2(max_glyph_height*num_lines);
		return new int[]{image_height, max_glyph_height, max_baseline_height, max_under_baseline_height};
	}

	private @NonNull Channel drawFont(@NonNull Font src_font, @NonNull String font_tex_classpath,
									  @NonNull Path font_info_dir, @NonNull String dest_font_name,
									  int logical_font_size, float scale_factor,
									  int max_glyph_height, int max_baseline_height, int max_under_baseline_height,
									  int image_width, int image_height, int space_width,
									  int @NonNull [] codepoints, boolean saveFontInfo,
									  int x_border, int y_border) {
		BufferedImage image = new BufferedImage(image_width, image_height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D)image.getGraphics();
		g2d.setFont(src_font);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		FontRenderContext frc = g2d.getFontRenderContext();

		g2d.translate(0, max_baseline_height);
		int current_x = 0;
		int current_y = 0;
		int valid_chars = 0;
		HashTable<@NonNull Quad> key_map = saveFontInfo ? new HashTable<>() : null;

		IO.println("Drawing chars for width*height = " + image_width + "*" + image_height);
		IO.print("Progress...");
		for (int i = 0; i < codepoints.length; i++) {
			if (i % 1000 == 0) {
				IO.print(".");
			}
			int codepoint = codepoints[i];
			if (src_font.canDisplay(codepoint)) {
				valid_chars++;
				GlyphVector gv = src_font.createGlyphVector(frc, Character.toChars(codepoint));
				Shape glyph_shape = gv.getGlyphOutline(0);
				Rectangle2D glyph_bounds = glyph_shape.getBounds2D();
				int min_x = (int)Math.floor(glyph_bounds.getMinX()) - x_border;
				//int min_y = (int)Math.floor(glyph_bounds.getMinY()) - GLYPH_Y_BORDER;
				int max_x = (int)Math.ceil(glyph_bounds.getMaxX()) + x_border;
				//int max_y = (int)Math.ceil(glyph_bounds.getMaxY()) + GLYPH_Y_BORDER;
				int glyph_width = codepoint == 32 ? space_width : max_x - min_x;
                assert glyph_width <= image_width : "character too wide to fit in image";
				if (current_x + glyph_width > image_width) {
					g2d.translate(-current_x, max_glyph_height);
					current_x = 0;
					current_y += max_glyph_height;
				}
				if (saveFontInfo) {
					float left = (float)current_x/image_width;
					float bottom = 1f - (float)(current_y + max_glyph_height)/image_height;
					float top = 1f - (float)current_y/image_height;
					float right = (float)(current_x + glyph_width)/image_width;
					var quad = new Quad(left, bottom, right, top, Math.round(glyph_width / scale_factor), Math.round(max_glyph_height / scale_factor));
					key_map.put(codepoint, quad);
				}
				g2d.translate(-min_x, 0);
				g2d.translate(0, -1);
				g2d.fill(glyph_shape);
				g2d.translate(0, 1);
				g2d.translate(min_x + glyph_width, 0);
				current_x += glyph_width;
			}
		}

		IO.println("done");
		if (saveFontInfo) {
			String tex_name = font_tex_classpath + "/" + dest_font_name + "_" + logical_font_size;
			FontInfo font_info = new FontInfo(tex_name, key_map, GLYPH_X_OVERLAP, GLYPH_Y_OVERLAP, Math.round(max_glyph_height / scale_factor), Math.round(max_baseline_height / scale_factor), Math.round(max_under_baseline_height / scale_factor));
			Path font_file_name = font_info_dir.resolve(dest_font_name + "_" + logical_font_size + ".font");
			font_info.saveToFile(font_file_name);
			IO.println("Number of valid chars found: " + valid_chars);
		}

		Channel channel = new Channel(image_width, image_height);
		byte[] image_pixels = (byte[])image.getRaster().getDataElements(0, 0, image.getWidth(), image.getHeight(), null);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pos = y * image.getWidth() * Integer.BYTES + x * Integer.BYTES;
				byte alpha = image_pixels[pos + 3];
				//byte img = image_pixels[pos + 1];
				int pixel = alpha & 0xff;
				float channel_pixel = pixel/255f;
				channel.putPixel(x, y, channel_pixel);
			}
		}
		return channel;
	}
}
