package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Encapsulates a DXT compressed image in DDS format.
 * Supports DXT1, DXT3, DXT5, and modern DX10 headers (BC1-BC3).
 */
public final class DXTImage {
    public static final int MAGIC = 0x20534444; // "DDS "
    public static final int FOURCC_DXT1 = 0x31545844; // "DXT1"
    public static final int FOURCC_DXT3 = 0x33545844; // "DXT3"
    public static final int FOURCC_DXT5 = 0x35545844; // "DXT5"
    public static final int FOURCC_DX10 = 0x30315844; // "DX10"

    // DX10 DXGI Formats
    public static final int DXGI_FORMAT_BC1_UNORM = 71;
    public static final int DXGI_FORMAT_BC1_UNORM_SRGB = 72;
    public static final int DXGI_FORMAT_BC2_UNORM = 74;
    public static final int DXGI_FORMAT_BC2_UNORM_SRGB = 75;
    public static final int DXGI_FORMAT_BC3_UNORM = 77;
    public static final int DXGI_FORMAT_BC3_UNORM_SRGB = 78;

    private final short width;
    private final short height;
    private final int fourCC;
    private final byte @NonNull [] @NonNull [] mipmaps;

    public DXTImage(short width, short height, int fourCC, byte @NonNull [] @NonNull [] mipmaps) {
        this.width = width;
        this.height = height;
        this.fourCC = fourCC;
        this.mipmaps = mipmaps;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth(int mipmap_level) {
        return Math.max(1, width >> mipmap_level);
    }

    public int getHeight(int mipmap_level) {
        return Math.max(1, height >> mipmap_level);
    }

    public int getFourCC() {
        return fourCC;
    }

    public int getNumMipMaps() {
        return mipmaps.length;
    }

    public @NonNull ByteBuffer getMipMap() {
        return getMipMap(0);
    }

    public @NonNull ByteBuffer getMipMap(int mipmap_level) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(mipmaps[mipmap_level].length);
        buffer.put(mipmaps[mipmap_level]);
        buffer.flip();
        return buffer;
    }

    public void position(int mipmap_level) {
        // Nothing to do for this implementation
    }

    public void write(@NonNull Path file) throws IOException {
        try (WritableByteChannel out = Files.newByteChannel(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer header = ByteBuffer.allocateDirect(128);
            header.order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(MAGIC);
            header.putInt(124); // header size
            header.putInt(0x1 | 0x2 | 0x4 | 0x1000 | (mipmaps.length > 1 ? 0x20000 : 0)); // flags: CAPS, HEIGHT, WIDTH, PIXELFORMAT, (MIPMAPCOUNT)
            header.putInt(height);
            header.putInt(width);
            header.putInt(0); // pitch or linear size
            header.putInt(0); // depth
            header.putInt(mipmaps.length); // mipmap count
            for (int i = 0; i < 11; i++) header.putInt(0); // reserved

            // PIXELFORMAT
            header.putInt(32); // size
            header.putInt(0x4); // flags: FOURCC
            header.putInt(fourCC);
            header.putInt(0); // RGB bits
            header.putInt(0); // R mask
            header.putInt(0); // G mask
            header.putInt(0); // B mask
            header.putInt(0); // A mask

            // CAPS
            header.putInt(0x1000 | (mipmaps.length > 1 ? 0x400008 : 0)); // CAPS_TEXTURE, (CAPS_COMPLEX | CAPS_MIPMAP)
            header.putInt(0); // CAPS2
            header.putInt(0); // CAPS3
            header.putInt(0); // CAPS4
            header.putInt(0); // Reserved2

            header.flip();
            writeContents(out, header);

            for (byte[] mipmap : mipmaps) {
                writeContents(out, ByteBuffer.wrap(mipmap));
            }
        }
    }

    public static @NonNull DXTImage read(@NonNull URL url) throws IOException {
        try (InputStream in = new BufferedInputStream(url.openStream())) {
            return read(Channels.newChannel(in));
        }
    }

    public static @NonNull DXTImage read(@NonNull Path file) throws IOException {
        try (FileChannel in = FileChannel.open(file, StandardOpenOption.READ)) {
            return read(in);
        }
    }

    public static @NonNull DXTImage read(@NonNull ReadableByteChannel in) throws IOException {
        ByteBuffer header = ByteBuffer.allocateDirect(128);
        header.order(ByteOrder.LITTLE_ENDIAN);
        while (header.hasRemaining()) {
            if (in.read(header) == -1) throw new IOException("Unexpected end of stream while reading DDS header");
        }
        header.flip();

        if (header.getInt() != MAGIC) throw new IOException("Not a DDS file");
        header.getInt(); // size
        header.getInt(); // flags
        int height = header.getInt();
        int width = header.getInt();
        header.getInt(); // pitch
        header.getInt(); // depth
        int mipmapCount = Math.max(1, header.getInt());
        for (int i = 0; i < 11; i++) header.getInt(); // reserved

        // PIXELFORMAT
        header.getInt(); // size
        header.getInt(); // flags
        int fourCC = header.getInt();

        if (fourCC == FOURCC_DX10) {
            // Read DX10 header
            ByteBuffer dx10Header = ByteBuffer.allocateDirect(20);
            dx10Header.order(ByteOrder.LITTLE_ENDIAN);
            while (dx10Header.hasRemaining()) {
                if (in.read(dx10Header) == -1)
                    throw new IOException("Unexpected end of stream while reading DX10 header");
            }
            dx10Header.flip();
            int dxgiFormat = dx10Header.getInt();
            fourCC = switch (dxgiFormat) {
                case DXGI_FORMAT_BC1_UNORM, DXGI_FORMAT_BC1_UNORM_SRGB -> FOURCC_DXT1;
                case DXGI_FORMAT_BC2_UNORM, DXGI_FORMAT_BC2_UNORM_SRGB -> FOURCC_DXT3;
                case DXGI_FORMAT_BC3_UNORM, DXGI_FORMAT_BC3_UNORM_SRGB -> FOURCC_DXT5;
                default -> throw new IOException("Unsupported DXGI format in DX10 header: " + dxgiFormat);
            };
        } else if (fourCC != FOURCC_DXT1 && fourCC != FOURCC_DXT3 && fourCC != FOURCC_DXT5) {
            throw new IOException("Unsupported FourCC: " + Integer.toHexString(fourCC));
        }

        byte[][] mipmaps = new byte[mipmapCount][];
        for (int i = 0; i < mipmapCount; i++) {
            int mipWidth = Math.max(1, width >> i);
            int mipHeight = Math.max(1, height >> i);
            int size = ((mipWidth + 3) / 4) * ((mipHeight + 3) / 4) * (fourCC == FOURCC_DXT1 ? 8 : 16);
            mipmaps[i] = new byte[size];
            ByteBuffer mipBuffer = ByteBuffer.wrap(mipmaps[i]);
            while (mipBuffer.hasRemaining()) {
                if (in.read(mipBuffer) == -1)
                    throw new IOException("Unexpected end of stream while reading mipmap " + i);
            }
        }

        return new DXTImage((short) width, (short) height, fourCC, mipmaps);
    }

    private static void writeContents(@NonNull WritableByteChannel out, @NonNull ByteBuffer data) throws IOException {
        while (data.hasRemaining())
            out.write(data);
    }
}
