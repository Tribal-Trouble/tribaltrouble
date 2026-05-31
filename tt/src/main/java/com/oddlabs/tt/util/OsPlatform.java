package com.oddlabs.tt.util;

public final class OsPlatform {

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase();

    public static final boolean IS_MAC = OS_NAME.contains("mac");
    public static final boolean IS_WINDOWS = OS_NAME.contains("windows");
    public static final boolean IS_LINUX = OS_NAME.contains("linux") || OS_NAME.contains("unix");

    private OsPlatform() {
    }
}
