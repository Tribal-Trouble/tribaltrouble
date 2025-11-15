package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

import java.text.DateFormat;
import java.util.Date;

public final class DateLabel extends Label {
	private final long val;

	public DateLabel(long val, @NonNull Font font, int width) {
		super(format(val), font, width);
		this.val = val;
	}

	public DateLabel(long val, @NonNull Font font) {
		super(format(val), font);
		this.val = val;
	}

	private static String format(long date) {
		if (date < 0)
			return "-";
		else
			return DateFormat.getDateTimeInstance().format(new Date(date));
	}

	@Override
	public int compareTo(@NonNull Label o) {
        return o instanceof DateLabel other ? val < other.val ? -1 : 1 : -1;
	}
}

