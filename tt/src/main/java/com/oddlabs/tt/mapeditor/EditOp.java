package com.oddlabs.tt.mapeditor;

import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * One edit as it goes between the players of a shared session: the heights and resources of the cells it changed,
 * as they are after it. An edit says what cells became, not what was done to them, so laying the same edits in the
 * same order gives every player the same island whatever each had before.
 *
 * <p>Heights go over in steps of {@link #HEIGHT_STEP} meters, and the player who made the edit keeps them at those
 * steps too, so nobody's island differs by a rounding. On the wire an edit is deflated: the rectangle the heights
 * lie in, a bit for each cell in it that the edit changed, each such height as the difference from the one before,
 * and then each resource cell and what is on it.
 *
 * @param x0             the first cell of the rectangle of heights
 * @param width          the rectangle's width in cells, 0 when the edit changed no heights
 * @param heights        a height for each cell of the rectangle, a row at a time, NaN where the edit left the cell
 *                       alone
 * @param resource_cells the cells, as y * size + x, where the edit changed what resource stands
 * @param resource_kinds what stands on each of those cells, as a {@link Resource} ordinal, or -1 for nothing
 */
record EditOp(int x0, int y0, int width, int height, float @NonNull [] heights, int @NonNull [] resource_cells,
              byte @NonNull [] resource_kinds) {

    /** Meters between the heights an edit can carry: fine enough not to show, coarse enough to deflate well. */
    static final float HEIGHT_STEP = 1f / 256f;
    private static final int VERSION = 1;
    private static final int MAX_HEIGHT_STEPS = 0xFFFF;
    static final byte NO_RESOURCE = -1;

    /** A height as an edit carries it. */
    static float quantize(float height) {
        return Math.clamp(Math.round(height / HEIGHT_STEP), 0, MAX_HEIGHT_STEPS) * HEIGHT_STEP;
    }

    boolean hasHeights() {
        return width > 0 && height > 0;
    }

    /** The cells changed, heights and resources together, as x0, y0, x1, y1 inclusive; x1 is below x0 when none. */
    int @NonNull [] bounds(int size) {
        int bx0 = hasHeights() ? x0 : Integer.MAX_VALUE;
        int by0 = hasHeights() ? y0 : Integer.MAX_VALUE;
        int bx1 = hasHeights() ? x0 + width - 1 : Integer.MIN_VALUE;
        int by1 = hasHeights() ? y0 + height - 1 : Integer.MIN_VALUE;
        for (int cell : resource_cells) {
            bx0 = Math.min(bx0, cell % size);
            by0 = Math.min(by0, cell / size);
            bx1 = Math.max(bx1, cell % size);
            by1 = Math.max(by1, cell / size);
        }
        return new int[]{bx0, by0, bx1, by1};
    }

    byte @NonNull [] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(new DeflaterOutputStream(bytes, new Deflater(Deflater.BEST_SPEED)))) {
            out.writeByte(VERSION);
            out.writeShort(x0);
            out.writeShort(y0);
            out.writeShort(width);
            out.writeShort(height);
            int cells = width * height;
            byte[] mask = new byte[(cells + 7) / 8];
            for (int i = 0; i < cells; i++)
                if (!Float.isNaN(heights[i]))
                    mask[i >> 3] |= (byte) (1 << (i & 7));
            out.write(mask);
            int previous = 0;
            for (int i = 0; i < cells; i++) {
                if (Float.isNaN(heights[i]))
                    continue;
                int steps = Math.round(heights[i] / HEIGHT_STEP);
                writeVarInt(out, zigzag(steps - previous));
                previous = steps;
            }
            out.writeInt(resource_cells.length);
            for (int i = 0; i < resource_cells.length; i++) {
                out.writeInt(resource_cells[i]);
                out.writeByte(resource_kinds[i]);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    /** Reads an edit, checking that it fits an island of the given size in cells. */
    static @NonNull EditOp decode(byte @NonNull [] data, int size) throws IOException {
        try (var in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
            if (in.readByte() != VERSION)
                throw new IOException("Unknown edit version");
            int x0 = in.readUnsignedShort();
            int y0 = in.readUnsignedShort();
            int width = in.readUnsignedShort();
            int height = in.readUnsignedShort();
            if (x0 + width > size || y0 + height > size)
                throw new IOException("Edit outside the island");
            int cells = width * height;
            byte[] mask = new byte[(cells + 7) / 8];
            in.readFully(mask);
            float[] heights = new float[cells];
            int previous = 0;
            for (int i = 0; i < cells; i++) {
                if ((mask[i >> 3] & (1 << (i & 7))) == 0) {
                    heights[i] = Float.NaN;
                    continue;
                }
                previous += unzigzag(readVarInt(in));
                if (previous < 0 || previous > MAX_HEIGHT_STEPS)
                    throw new IOException("Height out of range");
                heights[i] = previous * HEIGHT_STEP;
            }
            int count = in.readInt();
            if (count < 0 || count > size * size)
                throw new IOException("Bad resource count " + count);
            int[] resource_cells = new int[count];
            byte[] resource_kinds = new byte[count];
            for (int i = 0; i < count; i++) {
                resource_cells[i] = in.readInt();
                resource_kinds[i] = in.readByte();
                if (resource_cells[i] < 0 || resource_cells[i] >= size * size || resource_kinds[i] < NO_RESOURCE
                        || resource_kinds[i] >= Resource.values().length)
                    throw new IOException("Bad resource cell");
            }
            return new EditOp(x0, y0, width, height, heights, resource_cells, resource_kinds);
        }
    }

    private static int zigzag(int value) {
        return (value << 1) ^ (value >> 31);
    }

    private static int unzigzag(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    private static void writeVarInt(@NonNull DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(@NonNull DataInputStream in) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                return value;
        }
        throw new IOException("Bad number");
    }
}
