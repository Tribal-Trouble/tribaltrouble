package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.matchmaking.Login;
import com.oddlabs.matchmaking.LoginDetails;
import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.MatchmakingServerLoginInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.Connection;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.HostSequenceID;
import com.oddlabs.net.IllegalARMIEventException;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.net.SecureConnection;
import com.oddlabs.tt.form.ChatErrorForm;
import com.oddlabs.tt.form.InfoForm;
import com.oddlabs.tt.gui.ChatRoomInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SignedObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public final class MatchmakingClient implements MatchmakingClientInterface, ConnectionInterface {
    private static final String MATCHMAKING_HOST = "127.0.0.1";

	private static final int STATE_NOT_CONNECTED = 1;
	private static final int STATE_AWAITING_OK = 2;
	private static final int STATE_LOGGED_IN = 4;

	private final Map<HostSequenceID,TunnelledConnection> tunnels = new HashMap<>();
	private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(MatchmakingClientInterface.class);
	private final @NonNull ChatRoomHistory chat_room_history;
	private final @NonNull InGameChatHistory in_game_chat_history;
	private @Nullable GUIRoot chat_gui_root;
	private int current_seq_id = 1;
	private @Nullable SecureConnection conn;
	private @Nullable MatchmakingServerInterface matchmaking_interface;
	private @Nullable MatchmakingServerLoginInterface matchmaking_login_interface;
	private @Nullable TunnelledConnectionListener tunnelled_listener;
	private TunnelAddress local_address;
	private String username = Utils.getBundleString(ResourceBundle.getBundle(MatchmakingClient.class.getName()), "player");
	private @Nullable Profile active_profile = null;
	private int state = STATE_NOT_CONNECTED;
	private boolean update_allowed;
	private final Set<Integer> update_requested_types = new LinkedHashSet<>();
	private int update_key = 0;
	private @Nullable ProfileListener create_profile_listener;
	private @Nullable ChatRoomInfo chat_room_info;

	private Login login;
	private LoginDetails login_details;

	MatchmakingClient() {
		this.chat_room_history = new ChatRoomHistory();
		this.in_game_chat_history = new InGameChatHistory();
		Network.getChatHub().addListener(chat_room_history);
		Network.getChatHub().addListener(in_game_chat_history);
	}

	public @NonNull List<@NonNull String> getChatRoomHistory() {
		return chat_room_history.getMessages();
	}

	public @NonNull List<@NonNull String> getInGameChatHistory() {
		return in_game_chat_history.getMessages();
	}

	public void clearInGameChatHistory() {
		in_game_chat_history.clear();
	}

	@Override
	public void writeBufferDrained(AbstractConnection conn) {
	}

	@Override
	public void loginOK(String username, TunnelAddress address) {
		if (state == STATE_AWAITING_OK) {
			this.update_allowed = true;
			this.username = username;
			this.local_address = address;
			this.matchmaking_login_interface = null;
			this.matchmaking_interface = (MatchmakingServerInterface)ARMIEvent.createProxy(conn.getWrappedConnectionAndShutdown(), MatchmakingServerInterface.class);
			state = STATE_LOGGED_IN;
			MatchmakingListener listener = Network.getMatchmakingListener();
			listener.loggedIn();
		}
	}

	public void requestProfiles() {
		matchmaking_interface.requestProfiles();

	}

	public void requestList(int type) {
		if (update_allowed) {
			update_allowed = false;
			matchmaking_interface.requestList(type, update_key);
		} else
			update_requested_types.add(type);
	}

	public @Nullable Profile getProfile() {
		return active_profile;
	}

/*	public final String getNick() {
		if (active_profile == null)
			return username;
		else
			return active_profile.getNick();
	}
*/
	public void setProfile(String nick) {
		matchmaking_interface.setProfile(nick);
	}

	@Override
	public void updateProfile(Profile profile) {
		active_profile = profile;
	}

	public void setCreatingProfileListener(ProfileListener listener) {
		create_profile_listener = listener;
	}

	@Override
	public void createProfileSuccess() {
		if (create_profile_listener != null) {
			create_profile_listener.success();
			create_profile_listener = null;
		}
	}

	@Override
	public void createProfileError(int error_code) {
		if (create_profile_listener != null) {
			create_profile_listener.error(error_code);
			create_profile_listener = null;
		}
	}

	public void createProfile(String nick) {
		matchmaking_interface.createProfile(nick);
	}

	public void deleteProfile(String nick) {
		matchmaking_interface.deleteProfile(nick);
	}

	public void logPriority(String nick, int priority) {
		matchmaking_interface.logPriority(nick, priority);
	}

	@Override
	public void gameWonAck() {
		PeerHub.receivedAck();
	}

	@Override
	public void updateStart(int type) {
		MatchmakingListener listener = Network.getMatchmakingListener();
		if (listener != null) {
			listener.clearList(type);
		}
	}

	@Override
	public void updateList(int type, /*GameHost[]*/Object[] names) {
		MatchmakingListener listener = Network.getMatchmakingListener();
		if (listener != null)
			listener.receivedList(type, names);
	}

	@Override
	public void updateComplete(int next_update_key) {
		this.update_key = next_update_key;
		update_allowed = true;
		if (!update_requested_types.isEmpty()) {
			Iterator<Integer> it = update_requested_types.iterator();
			int type = it.next();
			it.remove();
			requestList(type);
		}
	}

	@Override
	public void updateProfileList(Profile[] profiles, String last_profile_nick) {
		MatchmakingListener listener = Network.getMatchmakingListener();
		if (listener != null)
			listener.receivedProfiles(profiles, last_profile_nick);
	}

	@Override
	public void joiningChatRoom(String room_name) {
		assert chat_room_info == null;
		chat_room_info = new ChatRoomInfo(room_name);

		chat_room_history.clear();
		MatchmakingListener listener = Network.getMatchmakingListener();
		if (listener != null)
			listener.joinedChat(chat_room_info);
	}

	@Override
	public void receiveChatRoomUsers(ChatRoomUser[] users) {
		if (chat_room_info != null) {
			chat_room_info.setUsers(users);

			MatchmakingListener listener = Network.getMatchmakingListener();
			chat_room_history.update(chat_room_info.getUsers());
			if (listener != null)
				listener.updateChatRoom(chat_room_info);
		}
	}

	@Override
	public void receiveChatRoomMessage(String owner, String msg) {
		Network.getChatHub().chat(new ChatMessage(owner, msg, ChatMessage.Type.CHATROOM));
	}

	@Override
	public void receivePrivateMessage(String nick, String msg) {
		Network.getChatHub().chat(new ChatMessage(nick, msg, ChatMessage.Type.PRIVATE));
	}

	public void sendPrivateMessage(GUIRoot gui_root, String nick, String message) {
		this.chat_gui_root = gui_root;
		getInterface().sendPrivateMessage(nick, message);
	}

	public void joinRoom(GUIRoot gui_root, String name) {
		this.chat_gui_root = gui_root;
		getInterface().joinRoom(name);
	}

	public void requestInfo(GUIRoot gui_root, String nick) {
		this.chat_gui_root = gui_root;
		getInterface().requestInfo(nick);
	}

	@Override
	public void receiveInfo(@NonNull Profile profile) {
		if (chat_gui_root != null) {
			chat_gui_root.addModalForm(new InfoForm(profile));
			chat_gui_root = null;
		}
	}

	public void leaveChatRoom() {
		if (isConnected())
			getInterface().leaveRoom();
		chat_room_info = null;
	}

	public @Nullable ChatRoomInfo getChatRoomInfo() {
		return chat_room_info;
	}

	@Override
	public void error(int error_code) {
		if (chat_gui_root != null) {
			chat_gui_root.addModalForm(new ChatErrorForm(error_code));
			chat_gui_root = null;
		}
	}

	public boolean isConnected() {
		return state == STATE_LOGGED_IN;
	}

	private void open(@NonNull NetworkSelector network) {
		close();

		this.conn = new SecureConnection(network.getDeterministic(), new Connection(network, MATCHMAKING_HOST, MatchmakingServerInterface.MATCHMAKING_SERVER_PORT, this), null);
		this.matchmaking_login_interface = (MatchmakingServerLoginInterface)ARMIEvent.createProxy(conn, MatchmakingServerLoginInterface.class);
	}

	public void login(@NonNull NetworkSelector network, Login login, LoginDetails login_details) {
		this.login = login;
		this.login_details = login_details;
		open(network);
		state = STATE_AWAITING_OK;
	}

	@Override
	public void loginError(int error_code) {
		close();
		MatchmakingListener listener = Network.getMatchmakingListener();
		listener.loginError(error_code);
	}

	public @Nullable MatchmakingServerLoginInterface getLoginInterface() {
		assert !isConnected();
		return matchmaking_login_interface;
	}

	public @Nullable MatchmakingServerInterface getInterface() {
		assert isConnected();
		return matchmaking_interface;
	}

	public TunnelAddress getLocalAddress() {
		return local_address;
	}

	public String getUsername() {
		return username;
	}

	public void registerTunnel(HostSequenceID host_seq, TunnelledConnection conn) {
		Object old = tunnels.put(host_seq, conn);
		assert old == null;
	}

	public @NonNull HostSequenceID registerTunnel(int address, TunnelledConnection conn) {
		int seq_id = current_seq_id++;
		HostSequenceID host_seq = new HostSequenceID(local_address.getHostID(), seq_id);
		matchmaking_interface.openTunnel(address, seq_id);
		registerTunnel(host_seq, conn);
		return host_seq;
	}

	public void unregisterTunnel(HostSequenceID host_seq, TunnelledConnection conn) {
		if (isConnected()) {
			Object old = tunnels.remove(host_seq);
			assert conn == old;
			closeTunnel(host_seq);
		}
	}

	public void closeTunnel(HostSequenceID host_seq) {
		matchmaking_interface.closeTunnel(host_seq);
	}

	public void registerTunnelledListener(TunnelledConnectionListener listener) {
		assert tunnelled_listener == null;
		this.tunnelled_listener = listener;
	}

	public void unregisterTunnelledListener(TunnelledConnectionListener listener) {
		assert tunnelled_listener == listener;
		tunnelled_listener = null;
	}

	private TunnelledConnection removeTunnel(HostSequenceID from) {
		return tunnels.remove(from);
	}

	private TunnelledConnection getTunnel(HostSequenceID from) {
		return tunnels.get(from);
	}

	@Override
	public void tunnelClosed(HostSequenceID from) {
		TunnelledConnection tunnel = removeTunnel(from);
		if (tunnel != null)
			tunnel.tunnelClosed();
	}

	@Override
	public void tunnelAccepted(HostSequenceID from) {
		TunnelledConnection tunnel = getTunnel(from);
		if (tunnel != null)
			tunnel.connected();
		else
			closeTunnel(from);
	}

	@Override
	public void tunnelOpened(@NonNull HostSequenceID from, InetAddress inet_from, InetAddress local_inet_from, Profile other) {
		if (tunnelled_listener != null) {
			tunnelled_listener.requestTunnelledConnection(from, inet_from, local_inet_from, other);
		} else
			closeTunnel(from);
	}

	@Override
	public void receiveRoutedEvent(HostSequenceID from, ARMIEvent event) {
		TunnelledConnection tunnel = getTunnel(from);
		if (tunnel != null)
			tunnel.receiveEvent(event);
		else
			closeTunnel(from);
	}

	@Override
	public void handle(Object sender, @NonNull ARMIEvent event) {
		try {
			event.execute(interface_methods, this);
		} catch (IllegalARMIEventException e) {
			handleError(e);
		}
	}

	private void handleError(Exception e) {
		ErrorListener listener = Network.getMatchmakingListener();
		close();
		if (listener != null)
			listener.connectionLost();
	}

	@Override
	public void connected(AbstractConnection connection) {
                // FIXME replace registratration key.
		SignedObject signed_key = null;
		Connection wrapped_connection = (Connection)conn.getWrappedConnection();
		matchmaking_login_interface.setLocalRemoteAddress(wrapped_connection.getLocalAddress());
			IO.println("wrapped_connection.getLocalAddress()	 = " + wrapped_connection.getLocalAddress());
		int revision = LocalInput.getRevision();
		if (!Renderer.isRegistered())
                    matchmaking_login_interface.loginAsGuest(revision);
		else if (login_details != null)
			matchmaking_login_interface.createUser(login, login_details, signed_key, revision);
		else
			matchmaking_login_interface.login(login, signed_key, revision);
	}

	@Override
	public void error(AbstractConnection conn, IOException e) {
		handleError(e);
	}

	public void close() {
		//if (state == STATE_NOT_CONNECTED)
		//	return;
		if (tunnelled_listener != null) {
			tunnelled_listener.connectionClosed();
			tunnelled_listener = null;
		}
        for (TunnelledConnection tunnel : tunnels.values()) {
            tunnel.tunnelClosed();
        }
		tunnels.clear();
		if (conn != null) {
			conn.close();
			conn = null;
		}
		state = STATE_NOT_CONNECTED;
		matchmaking_interface = null;
		active_profile = null;
	}
}
