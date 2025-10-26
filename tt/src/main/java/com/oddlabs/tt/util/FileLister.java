package com.oddlabs.tt.util;

import com.oddlabs.tt.event.LocalEventQueue;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public final class FileLister implements FileListerInterface {
	private final FileListerListener listener;

	public FileLister(@NonNull File dir, String pattern, FileListerListener listener) {
		this.listener = listener;
		newFiles(LocalEventQueue.getQueue().getDeterministic().log(dir.listFiles(new PatternFilenameFilter(pattern))));
	}

        @Override
	public void newFiles(File[] new_files) {
		listener.newFiles(new_files);
	}

	private static final class PatternFilenameFilter implements FilenameFilter {
		private final String pattern;

		public PatternFilenameFilter(String pattern) {
			this.pattern = pattern;
		}

                @Override
		public boolean accept(File dir, String name) {
			return Pattern.matches(pattern, name);
		}
	}
}
