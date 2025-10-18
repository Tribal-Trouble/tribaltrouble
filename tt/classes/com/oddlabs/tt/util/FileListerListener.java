package com.oddlabs.tt.util;

import java.io.File;

public interface FileListerListener {
	void newFiles(File[] files);
}
