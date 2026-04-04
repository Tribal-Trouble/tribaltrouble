package com.oddlabs.tt.util;

import java.io.File;
import java.io.IOException;

public interface FileLoaderInterface {
    void error(IOException e);

    void data(byte[] data, int num_bytes_read, boolean eof);

    void newFile(File file, long length);
}
