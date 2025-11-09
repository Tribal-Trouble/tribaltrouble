package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.tt.gui.ChatPanel;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

public final class ChatRoomHistory extends ChatHistory {

	private ChatRoomUser[] old_users;

	void update(ChatRoomUser[] new_users) {
		if (old_users == null)
			return;
		Set<ChatRoomUser> new_users_set = new HashSet<>(Arrays.asList(new_users));
		Set<ChatRoomUser> old_users_set = new HashSet<>(Arrays.asList(old_users));
		Set<ChatRoomUser> joined_users = new HashSet<>(new_users_set);
		joined_users.removeAll(old_users_set);
		Iterator<ChatRoomUser> it = joined_users.iterator();
		ResourceBundle bundle = ResourceBundle.getBundle(ChatPanel.class.getName());
		while (it.hasNext()) {
			ChatRoomUser user = it.next();
			addMessage(Utils.getBundleString(bundle, "user_joined", user.getNick()));
		}
		Set<ChatRoomUser> left_users = new HashSet<>(old_users_set);
		left_users.removeAll(new_users_set);
		it = left_users.iterator();
		while (it.hasNext()) {
			ChatRoomUser user = it.next();
			addMessage(Utils.getBundleString(bundle, "user_left", user.getNick()));
		}
	}

	@Override
	public void chat(@NonNull ChatMessage message) {
		if (message.type != ChatMessage.Type.PRIVATE && message.type != ChatMessage.Type.CHATROOM)
			return;
		addMessage(message.formatLong());
	}
}
