package com.oddlabs.matchmaking;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.HostSequenceID;

public interface MatchmakingServerInterface {
	int TYPE_NONE = 0;
	int TYPE_GAME = 1;
	int TYPE_CHAT_ROOM_LIST = 2;
	int TYPE_RANKING_LIST = 3;
	
	int MATCHMAKING_SERVER_PORT = 33214;

	int MAX_PLAYERS = 6;
	int MIN_PLAYERS = 1;
	int MIN_ROOM_NAME_LENGTH = 1;
	int MAX_ROOM_NAME_LENGTH = 20;
	int MAX_ROOM_USERS = 50;
	String ALLOWED_ROOM_CHARS = "abcdefghijklmnopqrstuvwxyz\u00E6\u00F8\u00E5ABCDEFGHIJKLMNOPQRSTUVWXYZ\u00C6\u00D8\u00C50123456789\u00E8\u00E9\u00EA\u00EB\u00EC\u00ED\u00EE\u00EF\u00F0\u00F1\u00F2\u00F3\u00F4\u00F5\u00F6\u00F9\u00FA\u00FB\u00FC\u00FD\u00FF-_,.:;?+={}[]()/&%#!<\\>'*";

	void setProfile(String nick);
	void createProfile(String nick);
	void deleteProfile(String nick);
	void requestProfiles();
void logPriority(String nick, int priority);
	
	void registerGame(Game game);
	void unregisterGame();
	
	void sendMessageToRoom(String msg);
	void sendPrivateMessage(String nick, String msg);
	void joinRoom(String name);
	void leaveRoom();
	void requestInfo(String nick);
	
	void requestList(int type, int update_key);
	
	void acceptTunnel(HostSequenceID host_seq);
	void openTunnel(int address_to, int seq);
	void closeTunnel(HostSequenceID address_to);
	void routeEvent(HostSequenceID from, ARMIEvent event);
	void multicastEvent(ARMIEvent event);
	void setMulticast(HostSequenceID[] addresses);

	void gameStartedNotify(GameSession game_session);
	void gameQuitNotify(String nick);
	void freeQuitStopNotify();
	void gameLostNotify();
	void gameWonNotify();
	void updateGameStatus(int tick, int[] status);
}
