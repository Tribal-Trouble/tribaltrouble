package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

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
            if (current < min)
                min = current;
            else if (current > max)
                max = current;
        }

        float mid = (max + min) / 2;
        offset[channel] = mid;
        scale[channel] = (max - mid) / Byte.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            data[channel][i] = (byte) ((array[i] - offset[channel]) / scale[channel]);
        }
    }

    public float[] getFloatArray() {
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
        String result = "";
        for (float v : array) {
            result += v + ", ";
        }
        return result;
    }
/*
	public static final void main(String[] args) {
		float[] array = {200f, 30f, 23.7765f};
		ByteCompressedFloatArray scfa = new ByteCompressedFloatArray(array, 1);
System.out.println("array = " + scfa);
	}
*/
}
