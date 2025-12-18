package com.oddlabs.event;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public abstract class Deterministic {
	protected static final int BUFFER_SIZE = 4096;
	protected static final int DEFAULTS_SIZE = 2;
	protected static final int MIN_DEFAULTS = Short.MIN_VALUE;

	private static final long CHECKPOINT_SIGNATURE = 0xdeadbabecafebeefL;

	private boolean enabled = true;

	public final boolean log(boolean b) {
		return log(b, false);
	}

	private boolean log(boolean b, boolean def) {
		assert enabled;
		return log(b ? (byte)1 : (byte)0, def ? (byte)1 : (byte)0) != 0;
	}

	public final byte log(byte b) {
		assert enabled;
		return log(b, (byte)0);
	}

	protected abstract byte log(byte b, byte def);

	public final char log(char c) {
		assert enabled;
		return log(c, (char)0);
	}

	protected abstract char log(char c, char def);

	public final int log(int i) {
		assert enabled;
		return log(i, 0);
	}

	protected abstract int log(int i, int def);

	public final long log(long l) {
		assert enabled;
		return log(l, 0);
	}

	protected abstract long log(long l, long def);

	public final float log(float f) {
		assert enabled;
		return log(f, 0f);
	}

	protected abstract float log(float f, float def);

    protected abstract @Nullable Path log(Path p, Path def);

    public final @Nullable Path log(@Nullable Path p) {
        assert enabled;
        return log(p, Path.of(""));
    }

	public final <T> @Nullable T log(@Nullable T o) {
		assert enabled;
		return logObject(o);
	}

	protected abstract <T> @Nullable T logObject(@Nullable T o);

	public final void log(@NonNull ByteBuffer o) {
		assert enabled;
		logBuffer(o);
	}

	protected abstract void logBuffer(@NonNull ByteBuffer o);

	public abstract void endLog();

	public abstract boolean isPlayback();

	public final void setEnabled(boolean enable) {
		this.enabled = enable;
	}

	public final void checkpoint() {
		checkpoint(CHECKPOINT_SIGNATURE);
	}
	public final void checkpoint(long value) {
		long logged_value = log(value);
		assert logged_value == value: logged_value + " != " + value;
	//	assert logged_value == value: "0x" + Long.toHexString(logged_value) + " != 0x" + Long.toHexString(value);
	}

	public final void checkpoint(boolean value) {
		boolean logged_value = log(value);
		assert logged_value == value: logged_value + " != " + value;
	//	assert logged_value == value: "0x" + Long.toHexString(logged_value) + " != 0x" + Long.toHexString(value);
	}

	public final void checkpoint(float value) {
		float logged_value = log(value);
		assert logged_value == value: logged_value + " != " + value;
	//	assert logged_value == value: "0x" + Long.toHexString(logged_value) + " != 0x" + Long.toHexString(value);
	}

	protected static int getTraceId() {
		Throwable t = new Throwable();
		StackTraceElement[] stack_trace_elements = t.getStackTrace();
		int hash = 0;
		for (StackTraceElement stack_trace_element : stack_trace_elements) {
			if (stack_trace_element.getClassName().startsWith(Deterministic.class.getPackage().getName())) {
				continue;
			}
			hash += stack_trace_element.getMethodName().hashCode();
		}
		return hash;
	}
}
