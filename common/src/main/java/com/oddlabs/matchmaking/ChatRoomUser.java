package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class ChatRoomUser implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	private final @NonNull String nick;
	private final boolean playing;

	public ChatRoomUser(@NonNull String nick, boolean playing) {
		this.nick = nick;
		this.playing = playing;
	}

	public @NonNull String getNick() {
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
	public boolean equals(@Nullable Object other) {
        return other instanceof ChatRoomUser && ((ChatRoomUser) other).nick.equals(nick);
	}
}
