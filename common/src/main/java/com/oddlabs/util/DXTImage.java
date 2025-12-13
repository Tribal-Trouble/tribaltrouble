package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.BufferedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class DXTImage {
	private static final int INITIAL_BUFFER_SIZE = 100000;
	private static byte @NonNull [] scratch_buffer = new byte[INITIAL_BUFFER_SIZE];
	private final short width;
	private final short height;
	private final int internal_format;
	private final ByteBuffer mipmaps;

	private static @NonNull ByteBuffer convertToByteBuffer(int internal_format, int width, int height, byte@NonNull [] @NonNull [] mipmaps) {
		int size = 0;
		int mipmap_width = width;
		int mipmap_height = height;
		for (byte[] mipmap : mipmaps) {
			size += mipmap.length;
			assert mipmap.length == getMipMapSize(internal_format, mipmap_width, mipmap_height);
			mipmap_width /= 2;
			mipmap_height /= 2;
		}
		ByteBuffer buffer = ByteBuffer.allocateDirect(size);
		for (byte[] mipmap : mipmaps) {
			buffer.put(mipmap);
		}
		buffer.flip();
		return buffer;
	}

	public DXTImage(short width, short height, int internal_format, byte@NonNull [] @NonNull [] mipmaps) {
		this(width, height, internal_format, convertToByteBuffer(internal_format, width, height, mipmaps));
	}
	
	public DXTImage(short width, short height, int internal_format, ByteBuffer mipmaps) {
		this.width = width;
		this.height = height;
		this.internal_format = internal_format;
		this.mipmaps = mipmaps;
		position(0);
	}

	public short getWidth() {
		return width;
	}

	public short getHeight() {
		return height;
	}

	public int getInternalFormat() {
		return internal_format;
	}

	public ByteBuffer getMipMap() {
		return mipmaps;
	}

	private static int getMipMapSize(int internal_format, int width, int height) {
		int blocksize = internal_format == 0x83F0 // GL_COMPRESSED_RGB_S3TC_DXT1_EXT
			? 8 : 16;
		return ((width + 3)/4) * ((height + 3)/4) * blocksize;
	}

	public int getNumMipMaps() {
		int size = mipmaps.capacity();
		int mipmap_width = width;
		int mipmap_height = height;
		int num_mipmaps = 0;
		while (size > 0) {
			int mipmap_size = getMipMapSize(internal_format, mipmap_width, mipmap_height);
			size -= mipmap_size;
			mipmap_width /= 2;
			mipmap_height /= 2;
			num_mipmaps++;
		}
		assert size == 0;
		return num_mipmaps;
	}

	public int getWidth(int mipmap_index) {
		return width >>> mipmap_index;
	}

	public int getHeight(int mipmap_index) {
		return height >>> mipmap_index;
	}

	public void position(int mipmap_index) {
		mipmaps.clear();
		int mipmap_width = width;
		int mipmap_height = height;
		int position = 0;
		while (mipmap_index > 0) {
			position += getMipMapSize(internal_format, mipmap_width, mipmap_height);
			mipmap_width /= 2;
			mipmap_height /= 2;
			mipmap_index--;
		}
		mipmaps.position(position);
		position += getMipMapSize(internal_format, mipmap_width, mipmap_height);
		mipmaps.limit(position);
	}

	public static @NonNull DXTImage read(@NonNull URL url) throws IOException {
		try (InputStream in = new BufferedInputStream(url.openStream())) {
            int index = 0;
            int bytes_read;
            while ((bytes_read = in.read(scratch_buffer, index, scratch_buffer.length - index)) != -1) {
                index += bytes_read;
                if (index == scratch_buffer.length) {
                    byte[] new_scratch_buffer = new byte[scratch_buffer.length * 2];
                    System.arraycopy(scratch_buffer, 0, new_scratch_buffer, 0, scratch_buffer.length);
                    scratch_buffer = new_scratch_buffer;
                }
            }
            ByteBuffer header = ByteBuffer.wrap(scratch_buffer);
            short width = header.getShort();
            short height = header.getShort();
            int internal_format = header.getInt();
            int data_length = index - header.position();
            ByteBuffer buffer = ByteBuffer.allocateDirect(data_length);
            buffer.put(scratch_buffer, header.position(), data_length);
            buffer.flip();
            return new DXTImage(width, height, internal_format, buffer);
        }
	}

	public void write(@NonNull Path file) throws IOException {
        try (var out = Files.newByteChannel(file, EnumSet.of(CREATE,TRUNCATE_EXISTING,READ,WRITE))) {
            ByteBuffer header = ByteBuffer.allocate(Short.BYTES + Short.BYTES + Integer.BYTES);
            header.putShort(width).putShort(height).putInt(internal_format);
            header.flip();
            writeContents(out, header);
            int old_position = mipmaps.position();
            int old_limit = mipmaps.limit();
            mipmaps.clear();
            writeContents(out, mipmaps);
            mipmaps.position(old_position);
            mipmaps.limit(old_limit);
        }
	}

	private static void writeContents(@NonNull WritableByteChannel out, @NonNull ByteBuffer data) throws IOException {
		while (data.hasRemaining())
			out.write(data);
	}
}
