package com.oddlabs.tt.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An OutputStream that writes output to multiple streams
 */
public final class TeeOutputStream extends OutputStream {
	private final OutputStream[] streams;

	public TeeOutputStream(OutputStream... streams) {
		this.streams = Arrays.copyOf(streams, streams.length);
	}

    @Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
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
