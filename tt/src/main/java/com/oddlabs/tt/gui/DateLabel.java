package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public final class DateLabel extends Label {
    private final long when;

    public DateLabel(long when, @NonNull Font font, int width) {
        super(format(when), font, width);
        this.when = when;
    }

    public DateLabel(long when, @NonNull Font font) {
        super(format(when), font);
        this.when = when;
    }

    private static @NonNull String format(long date) {
        return date > 0 ? DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(
                ZoneId.systemDefault()).format(Instant.ofEpochMilli(date)) : "-";
    }

    @Override
    public int compareTo(@NonNull Label o) {
        return o instanceof DateLabel other ? Long.compare(when, other.when) : -1;
    }
}
