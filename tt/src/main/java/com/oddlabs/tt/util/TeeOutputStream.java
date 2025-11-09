package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An OutputStream that writes output to multiple streams
 */
public final class TeeOutputStream extends OutputStream {
	private final OutputStream @NonNull [] streams;

	public TeeOutputStream(OutputStream @NonNull ... streams) {
		this.streams = Arrays.copyOf(streams, streams.length);
	}

	@Override
	public void write(byte @NonNull [] bytes, int offset, int length) throws IOException {
        for (OutputStream stream : streams) {
            stream.write(bytes, offset, length);
        }
	}

	@Override
	public void write(int b) throws IOException {
        for (OutputStream stream : streams) {
            stream.write(b);
        }
	}
}
