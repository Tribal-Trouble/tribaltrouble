package com.oddlabs.matchserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Screens names and censors chat messages against the banned_words table. Words with match type
 * 'substring' match anywhere (reserved for unambiguous slurs), while 'exact' words only match a
 * whole name or chat token (avoids the classic false positives on innocent words containing rude
 * fragments). The word list is cached so per-message checks stay off the database; the
 * /bannedwords Discord command invalidates the cache on changes, so the TTL only matters for
 * words edited directly in the database.
 */
public final class BannedWordFilter {

    public static final String MATCH_SUBSTRING = "substring";
    public static final String MATCH_EXACT = "exact";

    private static final long CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000;
    private static final String CENSOR_REPLACEMENT = "****";

    private record BannedWord(String lite, String collapsed, boolean substring_match) {
    }

    private static volatile List<BannedWord> cached_words;
    private static volatile long cache_loaded_time;

    private BannedWordFilter() {
    }

    /**
     * Forces the next check to reload the word list, so moderation commands apply immediately
     * instead of after the cache TTL.
     */
    public static void invalidateCache() {
        cached_words = null;
    }

    public static boolean isAllowed(String name) {
        String lite = normalize(name, false);
        String collapsed = normalize(name, true);
        for (BannedWord word : getBannedWords()) {
            if (matches(word, lite, collapsed))
                return false;
        }
        return true;
    }

    /**
     * Replaces every whitespace separated token that hits the banned word list with asterisks.
     */
    public static String censorChatMessage(String msg) {
        if (msg == null || msg.isEmpty())
            return msg;
        String[] tokens = msg.split(" ", -1);
        boolean censored = false;
        for (int i = 0; i < tokens.length; i++) {
            String lite = normalize(tokens[i], false);
            if (lite.isEmpty())
                continue;
            String collapsed = normalize(tokens[i], true);
            for (BannedWord word : getBannedWords()) {
                if (matches(word, lite, collapsed)) {
                    tokens[i] = CENSOR_REPLACEMENT;
                    censored = true;
                    break;
                }
            }
        }
        return censored ? String.join(" ", tokens) : msg;
    }

    private static boolean matches(BannedWord word, String lite, String collapsed) {
        if (word.substring_match())
            return collapsed.contains(word.collapsed()) || lite.contains(word.lite());
        return lite.equals(word.lite());
    }

    private static List<BannedWord> getBannedWords() {
        long now = System.currentTimeMillis();
        List<BannedWord> words = cached_words;
        if (words == null || now - cache_loaded_time > CACHE_TTL_MILLIS) {
            words = new ArrayList<>();
            for (String[] entry : DBInterface.getBannedWords()) {
                words.add(new BannedWord(normalize(entry[0], false), normalize(entry[0], true),
                        MATCH_SUBSTRING.equals(entry[1])));
            }
            cached_words = words;
            cache_loaded_time = now;
        }
        return words;
    }

    /**
     * Reduces text to bare letters so leetspeak and separator tricks cannot dodge the word list:
     * lowercase, common digit-for-letter substitutions mapped back, all other characters dropped.
     * With collapse_runs, repeated letters are also collapsed ("fuuuck" -> "fuck"). Collapsing is
     * only safe for substring matching; exact matching uses the uncollapsed form so short words do
     * not swallow unrelated names.
     */
    private static String normalize(String name, boolean collapse_runs) {
        StringBuilder sb = new StringBuilder(name.length());
        String lower = name.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            char c = switch (lower.charAt(i)) {
                case '0' -> 'o';
                case '1' -> 'i';
                case '2' -> 'z';
                case '3' -> 'e';
                case '4' -> 'a';
                case '5' -> 's';
                case '6', '9' -> 'g';
                case '7' -> 't';
                case '8' -> 'b';
                default -> lower.charAt(i);
            };
            if (c < 'a' || c > 'z')
                continue;
            if (collapse_runs && sb.length() > 0 && sb.charAt(sb.length() - 1) == c)
                continue;
            sb.append(c);
        }
        return sb.toString();
    }
}
