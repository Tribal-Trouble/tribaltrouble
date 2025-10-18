package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;

import java.text.DateFormat;
import java.util.Date;

public final class DateLabel extends Label {
	private final long val;

	public DateLabel(long val, Font font, int width) {
		super(format(val), font, width);
		this.val = val;
	}

	public DateLabel(long val, Font font) {
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
	public int compareTo(Object o) {
		if (o instanceof DateLabel) {
			DateLabel other = (DateLabel)o;
			return val < other.val ? -1 : 1;
		} else
			return -1;
	}
}

