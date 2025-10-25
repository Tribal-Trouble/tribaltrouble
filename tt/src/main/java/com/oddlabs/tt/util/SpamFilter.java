package com.oddlabs.tt.util;

public final class SpamFilter {
	public static String scan(String string) {
		string = string.replaceAll("\\s+", " ");
		string = string.replaceAll("\\.{3,}", "…");
		string = string.replaceAll("\\?+", "?");
		string = string.replaceAll("\\!+", "!");
		return string;
	}

    private SpamFilter() {
    }
}
