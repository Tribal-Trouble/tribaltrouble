package com.oddlabs.net;

import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ByteBufferInputStream extends InputStream {
	private final @NonNull ByteBuffer buffer;

	public ByteBufferInputStream(byte @NonNull [] array) {
		buffer = ByteBuffer.wrap(array);
	}
	
	public @NonNull ByteBuffer buffer() {
		return buffer;
	}

        @Override
	public int available() {
		return buffer.remaining();
	}

        @Override
	public int read(byte[] bytes, int offset, int length) {
		if (available() == 0)
			return -1;
		length = Math.min(length, available());
		buffer.get(bytes, offset, length);
		return length;
	}
	
        @Override
	public int read() {
		if (available() > 0) {
			int b = buffer.get();
			return b & 0xff;
		} else
			return -1;
	}
}
