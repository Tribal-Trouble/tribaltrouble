package com.oddlabs.net;

import com.oddlabs.util.ByteBufferOutputStream;
import java.io.*;

public interface ARMIArgumentWriter {
	void writeArgument(Class type, Object arg, ByteBufferOutputStream out) throws IOException;
}
