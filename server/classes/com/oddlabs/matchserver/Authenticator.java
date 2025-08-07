package com.oddlabs.matchserver;

import com.oddlabs.util.KeyManager;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.HostSequenceID;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.SecureConnection;
import com.oddlabs.net.IllegalARMIEventException;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.matchmaking.MatchmakingServerLoginInterface;
import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.matchmaking.Login;
import com.oddlabs.matchmaking.LoginDetails;
import com.oddlabs.registration.RegistrationInfo;
import com.oddlabs.registration.RegistrationKey;
import com.oddlabs.util.Compatibility;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SignedObject;
import java.security.GeneralSecurityException;

public final strictfp class Authenticator implements MatchmakingServerLoginInterface, ConnectionInterface {
	private static int guest_postfix = 1;
	
	private final SecureConnection conn;
	private final MatchmakingClientInterface client_interface;
	private final MatchmakingServer server;
	private final InetAddress remote_address;
	private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(MatchmakingServerLoginInterface.class);
	private final int host_id;
	private InetAddress local_remote_address;

	public Authenticator(MatchmakingServer server, SecureConnection conn, InetAddress remote_address, int host_id) {
		this.conn = conn;
		this.server = server;
		this.remote_address = remote_address;
		this.client_interface = (MatchmakingClientInterface)ARMIEvent.createProxy(conn, MatchmakingClientInterface.class);
		this.host_id = host_id;
		conn.setConnectionInterface(this);
	}

	public final void handle(Object sender, ARMIEvent event) {
		try {
			event.execute(interface_methods, this);
		} catch (IllegalARMIEventException e) {
			System.out.println("Exception: " + e);
			error(e);
		}
	}

	public final void writeBufferDrained(AbstractConnection conn) {
	}

	public final void error(AbstractConnection conn, IOException e) {
		error(e);
	}
	
	private final void error(Exception e) {
		close();
		MatchmakingServer.getLogger().warning("Exception e = " + e);
	}

	public final void connected(AbstractConnection conn) {
	}

	public final void setLocalRemoteAddress(InetAddress local_remote_address) {
		this.local_remote_address = local_remote_address;
	}

	public final static void checkUsername(String name) throws InvalidUsernameException {
		int min_username_length = DBInterface.getSettingsInt("min_username_length");
		if (name.length() < min_username_length)
			throw new InvalidUsernameException(MatchmakingClientInterface.USERNAME_ERROR_TOO_SHORT);

		int max_username_length = DBInterface.getSettingsInt("max_username_length");
		if (name.length() > max_username_length)
			throw new InvalidUsernameException(MatchmakingClientInterface.USERNAME_ERROR_TOO_LONG);

		String allowed_chars = DBInterface.getSetting("allowed_chars");
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (allowed_chars.indexOf(c) == -1)
				throw new InvalidUsernameException(MatchmakingClientInterface.USERNAME_ERROR_INVALID_CHARACTERS);
		}
	}

	public final void createUser(Login login, LoginDetails login_details, SignedObject reg_key, int revision) {
		// We are not passing the registration key anymore. We do not care anymore.
		// We keep the registration key so as to not break older clients that do send
		// the registration key.
		if (login == null || !login.isValid()) {
			close();
			return;
		}
		
		// check revision
		if (!revisionOK(revision)) {
			return;
		}
		
		try {
			checkUsername(login.getUsername());
		} catch (InvalidUsernameException e) {
			System.out.println("Exception: " + e);
			client_interface.loginError(e.getErrorCode());
			return;
		}
		
		if (login_details == null || !login_details.isValid()) {
			client_interface.loginError(MatchmakingClientInterface.USER_ERROR_INVALID_EMAIL);
			return;
		}
		
		if (DBInterface.usernameExists(login.getUsername())) {
			client_interface.loginError(MatchmakingClientInterface.USERNAME_ERROR_ALREADY_EXISTS);
			return;
		}

		DBInterface.createUser(login, login_details, null);
		MatchmakingServer.getLogger().info("Created user " + login.getUsername() + " with email address " + login_details.getEmail() + " with key " + null);
		doLogin(login.getUsername(), null, revision);
	}

	public final void login(Login login, SignedObject reg_key, int revision) {
		if (login == null || !login.isValid()) {
			close();
			return;
		}
		
		if (!revisionOK(revision)) {
			return;
		}

		String username = login.getUsername().trim();
		if (!DBInterface.queryUser(username, login.getPasswordDigest())) {
			client_interface.loginError(MatchmakingClientInterface.USER_ERROR_NO_SUCH_USER);
			return;
		}
		
		// We are not passing the registration key anymore. We do not care anymore.
		// We keep the registration key so as to not break older clients that do send
		// the registration key.
		doLogin(username, null, revision);
	}

	public final void loginAsGuest(int revision) {
		if (revisionOK(revision)) {
			String username = "Guest" + guest_postfix++; 
			doLogin(username, null, revision);
		}
	}

	private final boolean revisionOK(int revision) {
		if (revision != Compatibility.API_VERSION) {
			client_interface.loginError(MatchmakingClientInterface.USER_ERROR_VERSION_TOO_OLD);
			System.out.println("revision = " + revision + " | DBInterface.getSettingsInt(revision) = " + DBInterface.getSettingsInt("revision"));
			return false;
		} else
			return true;
	}

	private final void doLogin(String username, String reg_key_encoded, int revision) {
		if (local_remote_address != null) {
			client_interface.loginOK(username, new TunnelAddress(getHostID(), remote_address, local_remote_address));
			server.loginClient(remote_address, local_remote_address, username, conn.getWrappedConnectionAndShutdown(), reg_key_encoded, revision, host_id);
		} else {
			error(new IllegalStateException("Client didnt set local_remote_address"));
		}
	}
	
	public final int getHostID() {
		return host_id;
	}

	private final void close() {
		conn.close();
	}
}
