package com.oddlabs.event;

import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class NotDeterministic extends Deterministic {
	@Override
	public boolean isPlayback() {
		return false;
	}

	@Override
	public void endLog() {
	}

	@Override
	protected byte log(byte b, byte def) {
		return b;
	}

	@Override
	protected char log(char c, char def) {
		return c;
	}

	@Override
	protected int log(int i, int def) {
		return i;
	}

	@Override
	protected long log(long l, long def) {
		return l;
	}

	@Override
	protected float log(float f, float def) {
		return f;
	}

	@Override
	protected Object logObject(Object o) {
		return o;
	}

	@Override
	protected void logBuffer(@NonNull ByteBuffer b) {
		b.position(b.limit());
	}

    @Override
    protected Path log(Path p, Path def) {
        return p;
    }
}
