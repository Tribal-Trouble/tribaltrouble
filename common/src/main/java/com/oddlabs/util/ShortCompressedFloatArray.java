package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ShortCompressedFloatArray implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	private final float @NonNull [] scale;
	private final float @NonNull [] offset;
	private final short@NonNull [] @NonNull [] data;

	public ShortCompressedFloatArray(float @NonNull [] array, int channels) {
		int channel_length = array.length/channels;
		scale = new float[channels];
		offset = new float[channels];
		data = new short[channels][channel_length];
		float[][] split_data = new float[channels][channel_length];
		for (int i = 0; i < channel_length; i++) {
			for (int j = 0; j < channels; j++) {
				split_data[j][i] = array[i*channels + j];
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

		float mid = (max + min)/2;
		offset[channel] = mid;
		scale[channel] = (max - mid)/Short.MAX_VALUE;

		for (int i = 0; i < array.length; i++) {
            data[channel][i] = (short)((array[i] - offset[channel])/scale[channel]);
        }
	}

	public float[] getFloatArray() {
		int channels = data.length;
		int channel_length = data[0].length;
		float[] result = new float[channels*channel_length];

		for (int i = 0; i < channel_length; i++) {
			for (int j = 0; j < channels; j++) {
				result[i*channels + j] = data[j][i]*scale[j] + offset[j];
			}
		}
		return result;
	}

	@Override
	public @NonNull String toString() {
        float[] array = getFloatArray();
        return IntStream.range(0, array.length)
                .mapToObj(i -> Float.toString(array[i]))
                .collect(Collectors.joining(", "));
	}
}
