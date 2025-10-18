package com.oddlabs.net;

import com.oddlabs.util.ByteBufferOutputStream;

import java.io.IOException;

public interface ARMIArgumentWriter {
	void writeArgument(Class type, Object arg, ByteBufferOutputStream out) throws IOException;
}
