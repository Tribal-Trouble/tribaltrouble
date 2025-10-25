package com.oddlabs.tt.net;

import com.oddlabs.tt.util.SpamFilter;

public final class ChatMessage {
    public enum Type {
        NORMAL, TEAM, PRIVATE,CHATROOM,GAME_MENU
    }

	public final String nick;
	public final String message;
	public final Type type;

	public ChatMessage(String nick, String msg, Type type) {
		this.nick = nick;
		this.message = SpamFilter.scan(msg);
		this.type = type;
	}

	public String formatShort() {
		return "<" + nick + "> " + message;
	}

	public String formatLong() {
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
