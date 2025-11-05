package com.oddlabs.matchmaking;

import java.io.Serial;
import java.io.Serializable;

public final class ChatRoomUser implements Serializable {
	@Serial
	private final static long serialVersionUID = 1;

	private final String nick;
	private final boolean playing;

	public ChatRoomUser(String nick, boolean playing) {
		this.nick = nick;
		this.playing = playing;
	}

	public String getNick() {
		return nick;
	}

	public boolean isPlaying() {
		return playing;
	}

        @Override
	public int hashCode() {
		return nick.hashCode();
	}

        @Override
	public boolean equals(Object other) {
		if (!(other instanceof ChatRoomUser))
			return false;
		return ((ChatRoomUser)other).nick.equals(nick);
	}
}
