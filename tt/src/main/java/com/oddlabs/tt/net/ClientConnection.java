package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.AbstractConnection;
import org.jspecify.annotations.NonNull;

final class ClientConnection {
	private final AbstractConnection connection;
	private final @NonNull GameClientInterface gameclient_interface;
	private final ClientInfo client;

	public ClientConnection(AbstractConnection conn, ClientInfo client) {
		this.connection = conn;
		this.gameclient_interface = (GameClientInterface)ARMIEvent.createProxy(connection, GameClientInterface.class);
		this.client = client;
	}

	public GameClientInterface getClientInterface() {
		return gameclient_interface;
	}

	public AbstractConnection getConnection() {
		return connection;
	}

	public ClientInfo getClient() {
		return client;
	}
}
