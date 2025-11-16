package com.oddlabs.matchmaking;

import java.io.Serial;
import java.io.Serializable;

public final class ChatRoomEntry implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	private final String name;
	private final int num_joined;

	public ChatRoomEntry(String name, int num_joined) {
		this.name = name;
		this.num_joined = num_joined;
	}

	public String getName() {
		return name;
	}

	public int getNumJoined() {
		return num_joined;
	}
}
