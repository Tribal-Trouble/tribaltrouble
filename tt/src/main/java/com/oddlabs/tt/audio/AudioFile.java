package com.oddlabs.tt.audio;

import com.oddlabs.tt.resource.File;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public final class AudioFile extends File<Audio> {
	public AudioFile(@NonNull String location) {
		super(location);
	}

    @Override
	public @NonNull Audio get() {
        try {
            return new Audio(this.getURL());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not load " + this.getURL(), ex);
        }
	}

    @Override
	public boolean equals(Object o) {
		return o instanceof AudioFile && super.equals(o);
	}
}
