package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class DXTImage {
	// DDS file format constants
	private static final int DDS_MAGIC = 0x20534444; // "DDS "

	// DDS_HEADER flags
	private static final int DDSD_CAPS = 0x1;
	private static final int DDSD_HEIGHT = 0x2;
	private static final int DDSD_WIDTH = 0x4;
	private static final int DDSD_PIXELFORMAT = 0x1000;
	private static final int DDSD_MIPMAPCOUNT = 0x20000;
	private static final int DDSD_LINEARSIZE = 0x80000;

	// DDS_PIXELFORMAT flags
	private static final int DDPF_FOURCC = 0x4;

	// DDS capabilities
	private static final int DDSCAPS_COMPLEX = 0x8;
	private static final int DDSCAPS_MIPMAP = 0x400000;
	private static final int DDSCAPS_TEXTURE = 0x1000;

	// FourCC codes
	public static final int FOURCC_DXT1 = 0x31545844; // "DXT1"
	public static final int FOURCC_DXT5 = 0x35545844; // "DXT5"

	private static final int INITIAL_BUFFER_SIZE = 100000;
	private static byte @NonNull [] scratch_buffer = new byte[INITIAL_BUFFER_SIZE];
	private final short width;
	private final short height;
	private final int fourCC;
	private final ByteBuffer mipmaps;

	private static @NonNull ByteBuffer convertToByteBuffer(int fourCC, int width, int height, byte@NonNull [] @NonNull [] mipmaps) {
		int size = 0;
		int mipmap_width = width;
		int mipmap_height = height;
		for (byte[] mipmap : mipmaps) {
			size += mipmap.length;
			assert mipmap.length == getMipMapSize(fourCC, mipmap_width, mipmap_height);
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

	public DXTImage(short width, short height, int fourCC, byte@NonNull [] @NonNull [] mipmaps) {
		this(width, height, fourCC, convertToByteBuffer(fourCC, width, height, mipmaps));
	}
	
	public DXTImage(short width, short height, int fourCC, ByteBuffer mipmaps) {
		this.width = width;
		this.height = height;
		this.fourCC = fourCC;
		this.mipmaps = mipmaps;
		position(0);
	}

	public short getWidth() {
		return width;
	}

	public short getHeight() {
		return height;
	}

	public int getFourCC() {
		return fourCC;
	}

	public ByteBuffer getMipMap() {
		return mipmaps;
	}

	private static int getMipMapSize(int fourCC, int width, int height) {
		int blocksize = fourCC == FOURCC_DXT1 ? 8 : 16;
		return ((width + 3)/4) * ((height + 3)/4) * blocksize;
	}

	public int getNumMipMaps() {
		int size = mipmaps.capacity();
		int mipmap_width = width;
		int mipmap_height = height;
		int num_mipmaps = 0;
		while (size > 0 && mipmap_width > 0 && mipmap_height > 0) {
			int mipmap_size = getMipMapSize(fourCC, mipmap_width, mipmap_height);
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
			position += getMipMapSize(fourCC, mipmap_width, mipmap_height);
			mipmap_width /= 2;
			mipmap_height /= 2;
			mipmap_index--;
		}
		mipmaps.position(position);
		position += getMipMapSize(fourCC, mipmap_width, mipmap_height);
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

            ByteBuffer ddsBuffer = ByteBuffer.wrap(scratch_buffer, 0, index).order(ByteOrder.LITTLE_ENDIAN);

            if (ddsBuffer.getInt() == DDS_MAGIC) {
                // DDS_HEADER
                if (ddsBuffer.getInt() != 124) { // dwSize
                    throw new IOException("Invalid DDS header size");
                }
                ddsBuffer.getInt(); // dwFlags
                short height = (short) ddsBuffer.getInt();
                short width = (short) ddsBuffer.getInt();
                
                ddsBuffer.position(76); // Seek to DDS_PIXELFORMAT

                // DDS_PIXELFORMAT
                if (ddsBuffer.getInt() != 32) { // ddspf.dwSize
                    throw new IOException("Invalid DDS pixel format size");
                }
                ddsBuffer.getInt(); // ddspf.dwFlags
                int fourCC = ddsBuffer.getInt();

                if (fourCC != FOURCC_DXT1 && fourCC != FOURCC_DXT5) {
                    throw new IOException("Unsupported FourCC: " + Integer.toHexString(fourCC));
                }

                ddsBuffer.position(128); // Seek to data

                int data_length = ddsBuffer.remaining();
                ByteBuffer buffer = ByteBuffer.allocateDirect(data_length);
                buffer.put(ddsBuffer);
                buffer.flip();

                return new DXTImage(width, height, fourCC, buffer);
            } else {
                // Old .dxtn format
                ddsBuffer.rewind();
                short width = ddsBuffer.getShort();
                short height = ddsBuffer.getShort();
                int internal_format = ddsBuffer.getInt();
                int fourCC = internal_format == 0x83F0 ? FOURCC_DXT1 : FOURCC_DXT5;

                int data_length = ddsBuffer.remaining();
                ByteBuffer buffer = ByteBuffer.allocateDirect(data_length);
                buffer.put(ddsBuffer);
                buffer.flip();
                return new DXTImage(width, height, fourCC, buffer);
            }
        }
	}

	public void write(@NonNull Path file) throws IOException {
        try (var out = Files.newByteChannel(file, EnumSet.of(CREATE,TRUNCATE_EXISTING,WRITE))) {
            int numMipMaps = getNumMipMaps();
            int pitchOrLinearSize = getMipMapSize(fourCC, width, height);

            ByteBuffer header = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);

            header.putInt(DDS_MAGIC);
            header.putInt(124); // dwSize
            header.putInt(DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT | DDSD_MIPMAPCOUNT | DDSD_LINEARSIZE);
            header.putInt(height);
            header.putInt(width);
            header.putInt(pitchOrLinearSize);
            header.putInt(0); // dwDepth
            header.putInt(numMipMaps);
            for (int i = 0; i < 11; i++) {
                header.putInt(0); // dwReserved1
            }

            header.putInt(32); // ddspf.dwSize
            header.putInt(DDPF_FOURCC);
            header.putInt(fourCC);
            header.putInt(0); // ddspf.dwRGBBitCount
            header.putInt(0); // ddspf.dwRBitMask
            header.putInt(0); // ddspf.dwGBitMask
            header.putInt(0); // ddspf.dwBBitMask
            header.putInt(0); // ddspf.dwABitMask

            header.putInt(DDSCAPS_TEXTURE | DDSCAPS_COMPLEX | DDSCAPS_MIPMAP);
            header.putInt(0); // dwCaps2
            header.putInt(0); // dwCaps3
            header.putInt(0); // dwCaps4
            header.putInt(0); // dwReserved2

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
