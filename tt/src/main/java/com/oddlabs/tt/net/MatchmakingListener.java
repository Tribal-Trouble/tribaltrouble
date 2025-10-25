package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Profile;
import com.oddlabs.tt.gui.ChatRoomInfo;

public interface MatchmakingListener extends ErrorListener {
	void clearList(int type);
	void receivedList(int type, Object[] names);
	void loggedIn();
	void loginError(int error_code);
	void receivedProfiles(Profile[] profiles, String last_nick);
	void joinedChat(ChatRoomInfo info);
	void updateChatRoom(ChatRoomInfo info);
}
