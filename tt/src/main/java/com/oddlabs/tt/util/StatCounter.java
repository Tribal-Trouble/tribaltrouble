package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;

public final class StatCounter {
    private final long @NonNull [] values;
    private long old_val = 0;
    private long sum = 0;
    private int position = 0;

    public StatCounter(int average_count) {
        values = new long[average_count];
    }

    public void updateDelta(long val) {
        long diff = val - old_val;
        old_val = val;
        updateAbsolute(diff);
    }

    public void updateAbsolute(long val) {
        sum += val - values[position];
        values[position] = val;
        position = (position + 1) % values.length;
    }

    public long getMax() {
        long max = Long.MIN_VALUE;
        for (long value : values) {
            if (value > max)
                max = value;
        }
        return max;
    }

    public long getAveragePerUpdateFloored() {
        return sum / values.length;
    }

    public float getAveragePerUpdate() {
        return (float) sum / values.length;
    }
}
