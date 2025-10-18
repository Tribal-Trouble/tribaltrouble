package com.oddlabs.net;

import java.io.*;

public interface ARMIArgumentReader {
	Object readArgument(Class type, ByteBufferInputStream in) throws IOException, ClassNotFoundException;
}
