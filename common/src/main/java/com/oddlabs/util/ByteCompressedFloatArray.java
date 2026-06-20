package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Compresses an array of float values into 8-bit bytes (per channel)
 */
public final class ByteCompressedFloatArray implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private final float @NonNull [] scale;
    private final float @NonNull [] offset;
    private final byte @NonNull [] @NonNull [] data;

    public ByteCompressedFloatArray(float @NonNull [] array, int channels) {
        int channel_length = array.length / channels;
        scale = new float[channels];
        offset = new float[channels];
        data = new byte[channels][channel_length];
        float[][] split_data = new float[channels][channel_length];
        for (int i = 0; i < channel_length; i++) {
            for (int j = 0; j < channels; j++) {
                split_data[j][i] = array[i * channels + j];
            }
        }
        for (int i = 0; i < channels; i++) {
            compress(split_data[i], i);
        }
    }

    private void compress(float @NonNull [] array, int channel) {
        float min = array[0];
        float max = array[0];

        for (float current : array) {
            if (current < min) {
                min = current;
            } else if (current > max) {
                max = current;
            }
        }

        float mid = (max + min) / 2;
        offset[channel] = mid;
        float diff = max - mid;
        scale[channel] = diff == 0f ? 1f : diff / Byte.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            data[channel][i] = (byte) Math.clamp(Math.round((array[i] - offset[channel]) / scale[channel]),
                    -Byte.MAX_VALUE, Byte.MAX_VALUE);
        }
    }

    public float @NonNull [] getFloatArray() {
        int channels = data.length;
        int channel_length = data[0].length;
        float[] result = new float[channels * channel_length];

        for (int i = 0; i < channel_length; i++) {
            for (int j = 0; j < channels; j++) {
                result[i * channels + j] = data[j][i] * scale[j] + offset[j];
            }
        }
        return result;
    }

    @Override
    public @NonNull String toString() {
        float[] array = getFloatArray();
        return IntStream.range(0, array.length)
                .mapToObj(idx -> Float.toString(array[idx]))
                .collect(Collectors.joining(", "));
    }
}
