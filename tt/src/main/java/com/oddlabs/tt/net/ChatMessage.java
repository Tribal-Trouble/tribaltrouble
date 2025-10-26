package com.oddlabs.tt.net;

import com.oddlabs.tt.util.SpamFilter;
import org.jspecify.annotations.NonNull;

public final class ChatMessage {
    public enum Type {
        NORMAL, TEAM, PRIVATE,CHATROOM,GAME_MENU
    }

	public final String nick;
	public final @NonNull String message;
	public final Type type;

	public ChatMessage(String nick, String msg, Type type) {
		this.nick = nick;
		this.message = SpamFilter.scan(msg);
		this.type = type;
	}

	public @NonNull String formatShort() {
		return "<" + nick + "> " + message;
	}

	public @NonNull String formatLong() {
		switch (type) {
			case TEAM:
				return "(Team) " + formatShort();
			case PRIVATE:
				return "(Private) " + formatShort();
			case NORMAL: /* Fall through */
			case CHATROOM:
			case GAME_MENU:
				return formatShort();
			default:
				throw new RuntimeException();
		}
	}
}
