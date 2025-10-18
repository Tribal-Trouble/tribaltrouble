package com.oddlabs.tt.gui;

import com.oddlabs.matchmaking.ChatRoomUser;

public final class ChatRoomInfo {
	private final String name;

	private ChatRoomUser[] users;

	public ChatRoomInfo(String name) {
		this.name = name;
	}

	public void setUsers(ChatRoomUser[] users) {
		this.users = users;
	}

	public String getName() {
		return name;
	}

	public ChatRoomUser[] getUsers() {
		return users;
	}
}
