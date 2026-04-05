package com.oddlabs.matchserver;

import java.io.File;
import java.net.URLEncoder;

public class WebsiteLinkHelper {
    public static String getPlayerHighscoreUrl(String nick) {
        String domain = ServerConfiguration.getInstance().get(ServerConfiguration.WEBSITE_DOMAIN);
        if (domain == null) domain = "tribaltrouble.org";

        try {
            nick = URLEncoder.encode(nick, "UTF-8");
        } catch (Exception e) {
            // Fallback in case of error
            nick = "";
        }
        return String.format("https://%s/#player#%s#0", domain, nick);
    }

    public static String getReplayUrl(int game_id) {
        File spectatorFile = new File("/var/games/" + game_id);
        boolean exists = spectatorFile.exists();
        String domain = ServerConfiguration.getInstance().get(ServerConfiguration.WEBSITE_DOMAIN);
        if (domain == null) domain = "tribaltrouble.org";
        return exists ? String.format("https://%s/watch.html#%d", domain, game_id) : null;
    }

    public static String getProfileLink(String display_text, String nick) {
        String url = getPlayerHighscoreUrl(nick);
        return String.format("[%s](%s)", display_text, url);
    }
}
