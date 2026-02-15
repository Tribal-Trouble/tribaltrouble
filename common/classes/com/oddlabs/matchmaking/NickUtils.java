package com.oddlabs.matchmaking;

import java.util.regex.Pattern;

public final class NickUtils {
    private static final Pattern DISCRIMINATOR = Pattern.compile("#\\d+$");

    /**
     * Strip the #<steamAccountId> discriminator for display. "Viking#76561198123456789" → "Viking"
     */
    public static String toDisplayName(String nick) {
        if (nick == null) return null;
        return DISCRIMINATOR.matcher(nick).replaceFirst("");
    }

    /** Generate a nick with discriminator: personaName + "#" + full steamAccountId */
    public static String generateSteamNick(String personaName, long steamAccountId) {
        // Strip any existing # from persona name to avoid ambiguity
        String sanitized = personaName.replace("#", "");
        return sanitized + "#" + steamAccountId;
    }
}
