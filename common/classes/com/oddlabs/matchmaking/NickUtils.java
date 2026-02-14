package com.oddlabs.matchmaking;

import java.util.regex.Pattern;

public final class NickUtils {
    private static final Pattern DISCRIMINATOR = Pattern.compile("#\\d+$");

    /** Strip the #XXXX discriminator for display. "Viking#7432" → "Viking" */
    public static String toDisplayName(String nick) {
        if (nick == null) return null;
        return DISCRIMINATOR.matcher(nick).replaceFirst("");
    }

    /** Generate a nick with discriminator: personaName + "#" + last 4 digits of steamAccountId */
    public static String generateSteamNick(String personaName, long steamAccountId) {
        // Strip any existing # from persona name to avoid ambiguity
        String sanitized = personaName.replace("#", "");
        String discriminator = String.format("%04d", Math.abs(steamAccountId % 10000));
        return sanitized + "#" + discriminator;
    }
}
