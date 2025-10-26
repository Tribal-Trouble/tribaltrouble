package com.oddlabs.osutil;

import org.jspecify.annotations.NonNull;
import org.lwjgl.LWJGLUtil;

public abstract class OSUtil {
	public static @NonNull OSUtil create() {
		int platform = LWJGLUtil.getPlatform();
		switch (platform) {
			case LWJGLUtil.PLATFORM_MACOSX:
				return new MacOSXUtil();
			case LWJGLUtil.PLATFORM_LINUX:
			case LWJGLUtil.PLATFORM_WINDOWS:
			default:
				throw new RuntimeException("Unsupported platform: " + platform);
		}
	}

	public abstract void registerAssociation(String game_name, Association association);
	public abstract void registerURLScheme(String game_name, URLAssociation association);
}
