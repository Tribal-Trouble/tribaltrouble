package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;

public final class Languages {
	private static final String[][] languages = new String[][]{{"en", "English"}, {"da", "Dansk"}, {"de", "Deutsch"}, {"es","Español"}, {"it", "Italiano"}};

	public static boolean hasLanguage(String language) {
        for (String[] language1 : languages) {
            if (language1[0].equals(language)) {
                return true;
            }
        }
		return false;
	}

	public static String[] @NonNull [] getLanguages() {
		return languages;
	}

	public static Quad @NonNull [] getFlags() {
		Quad[] flags = new Quad[]{Skin.getSkin().getFlagDa(), Skin.getSkin().getFlagEn(), Skin.getSkin().getFlagDe(), Skin.getSkin().getFlagEs(), Skin.getSkin().getFlagIt()};
		return flags;
	}
}
