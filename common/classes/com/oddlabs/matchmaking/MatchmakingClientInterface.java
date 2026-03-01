package com.oddlabs.matchmaking;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.HostSequenceID;

import java.net.InetAddress;

public strictfp interface MatchmakingClientInterface {
    // error codes
    public static final int PROFILE_ERROR_GUEST = 2;

    public static final int USER_ERROR_INVALID_EMAIL = 7;
    public static final int USER_ERROR_NO_SUCH_USER = 8;
    public static final int USER_ERROR_VERSION_TOO_OLD = 9;

    public static final int USERNAME_ERROR_TOO_MANY = 1;
    public static final int USERNAME_ERROR_ALREADY_EXISTS = 3;
    public static final int USERNAME_ERROR_INVALID_CHARACTERS = 4;
    public static final int USERNAME_ERROR_TOO_LONG = 5;
    public static final int USERNAME_ERROR_TOO_SHORT = 6;

    public static final int CHAT_ERROR_TOO_MANY_USERS = 10;
    public static final int CHAT_ERROR_INVALID_NAME = 11;
    public static final int CHAT_ERROR_NO_SUCH_NICK = 12;
    public static final int CHAT_ERROR_SPECTATE_FAILED = 13;

    public void updateProfileList(Profile[] profiles, String last_profile_nick);

    public void updateProfile(Profile profiles);

    public void createProfileError(int error_code);

    public void createProfileSuccess();

    public void joiningChatRoom(String room_name);

    public void error(int error_code);

    public void receiveChatRoomUsers(ChatRoomUser[] users);

    public void receiveChatRoomMessage(String nick, String msg);

    public void receivePrivateMessage(String nick, String msg);

    public void receiveInfo(Profile profile);

    public void updateStart(int type);

    public void updateList(int type, Object[] names);

    public void updateComplete(int next_update_key);

    public void gameWonAck();

    public void tunnelOpened(
            HostSequenceID from,
            InetAddress inet_address,
            InetAddress local_inet_address,
            Profile name);

    public void tunnelClosed(HostSequenceID from);

    public void tunnelAccepted(HostSequenceID from);

    public void receiveRoutedEvent(HostSequenceID from, ARMIEvent event);

    public void receiveSpectatorData(byte[] world_params_data, byte[] event_log_data, int current_tick);

    public void loginOK(String username, TunnelAddress address);

    public void loginError(int error_code);
}
