package com.oddlabs.tt.net;

import com.oddlabs.tt.util.SpamFilter;
import org.jspecify.annotations.NonNull;

public record ChatMessage(@NonNull String nick, @NonNull String message, @NonNull Type type) {
    public enum Type {
        NORMAL, TEAM, PRIVATE, CHATROOM, GAME_MENU
    }

    public ChatMessage(@NonNull String nick, @NonNull String message, @NonNull Type type) {
        this.nick = nick;
        this.message = SpamFilter.scan(message);
        this.type = type;
    }

    public @NonNull String formatShort() {
        return "<" + nick + "> " + message;
    }

    public @NonNull String formatLong() {
        return switch (type) {
            case TEAM -> "(Team) " + formatShort();
            case PRIVATE -> "(Private) " + formatShort(); /* Fall through */
            case NORMAL, CHATROOM, GAME_MENU -> formatShort();
        };
    }
}
