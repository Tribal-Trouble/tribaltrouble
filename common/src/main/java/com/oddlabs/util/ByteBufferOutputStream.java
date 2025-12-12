package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class ByteBufferOutputStream extends OutputStream {
	private static final int BUFFER_SIZE = 16382;
	
	private ByteBuffer buffer;

	public ByteBufferOutputStream(boolean direct) {
		super();
		if (direct)
			buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		else
			buffer = ByteBuffer.allocate(BUFFER_SIZE);
	}

	public void reset() {
		buffer.clear();
	}

	public byte[] toByteArray() {
		byte[] result = new byte[buffer.position()];
		buffer.flip();
		buffer.get(result);
		return result;
	}
	
	public ByteBuffer buffer() {
		return buffer;
	}

	private void ensureCapacity(int size) {
		if (buffer.remaining() < size) {
			ByteBuffer new_buffer;
			if (buffer.isDirect())
				new_buffer = ByteBuffer.allocateDirect(buffer.capacity()*2 + size);
			else
				new_buffer = ByteBuffer.allocate(buffer.capacity()*2 + size);
			buffer.flip();
			new_buffer.put(buffer);
			buffer = new_buffer;
		}
	}
	
	@Override
	public void write(byte @NonNull [] bytes, int offset, int length) {
		ensureCapacity(length);
		buffer.put(bytes, offset, length);
	}
	
	@Override
	public void write(int b_int) {
		ensureCapacity(1);
		byte b = (byte)(b_int & 0xff);
		buffer.put(b);
	}
}
