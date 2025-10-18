package com.oddlabs.event;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public final class StackTraceDeterministic extends Deterministic {
	private final Deterministic deterministic;
	private final Deterministic stack_deterministic;

	public StackTraceDeterministic(Deterministic deterministic, Deterministic stack_deterministic) {
		this.deterministic = deterministic;
		this.stack_deterministic = stack_deterministic;
	}

        @Override
	public boolean isPlayback() {
		return deterministic.isPlayback();
	}

        @Override
	public void endLog() {
		deterministic.endLog();
		stack_deterministic.endLog();
	}

	private void logTrace() {
		int stack_trace_hash = getTraceId();
		int old_stack_trace_hash =  stack_deterministic.log(stack_trace_hash);
		if (old_stack_trace_hash != stack_trace_hash)
			throw new Error("old_stack_trace_hash = " + old_stack_trace_hash + " | stack_trace_hash = " + stack_trace_hash);
	}

        @Override
	protected byte log(byte b, byte def) {
		logTrace();
		return deterministic.log(b, def);
	}

        @Override
	protected char log(char c, char def) {
		logTrace();
		return deterministic.log(c, def);
	}

        @Override
	protected int log(int i, int def) {
		logTrace();
		return deterministic.log(i, def);
	}

        @Override
	protected long log(long l, long def) {
		logTrace();
		return deterministic.log(l, def);
	}

        @Override
	protected float log(float f, float def) {
		logTrace();
		return deterministic.log(f, def);
	}

        @Override
	protected Object logObject(Object o) {
		logTrace();
		return deterministic.log(o);
	}

        @Override
	protected void logBuffer(ByteBuffer b) {
		logTrace();
		deterministic.log(b);
	}

    @Override
    protected Path log(Path p, Path def) {
        logTrace();
        return deterministic.log(p);
    }
}
