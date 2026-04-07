package com.oddlabs.matchserver;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class WebsiteLinkHelper {

    public static String getPlayerHighscoreUrl(String nick) {
        String domain = getDomain();
        String encoded = URLEncoder.encode(nick, StandardCharsets.UTF_8);
        return String.format("https://%s/#player#%s#0", domain, encoded);
    }

    public static String getReplayUrl(int gameId) {
        String dirPath = ServerConfiguration.getInstance().get(ServerConfiguration.SPECTATOR_DATA_DIR);
        if (dirPath == null || dirPath.isEmpty()) dirPath = "/var/games";
        File spectatorFile = new File(dirPath, String.valueOf(gameId));
        if (!spectatorFile.exists()) return null;
        return String.format("https://%s/watch.html#%d", getDomain(), gameId);
    }

    public static String getProfileLink(String displayText, String nick) {
        return String.format("[%s](%s)", displayText, getPlayerHighscoreUrl(nick));
    }

    private static String getDomain() {
        String domain = ServerConfiguration.getInstance().get(ServerConfiguration.WEBSITE_DOMAIN);
        return domain != null ? domain : "tribaltrouble.org";
    }
}
