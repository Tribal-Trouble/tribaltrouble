package com.oddlabs.tt.net;

public final class GameNetwork {
	private final Server server;
	private final Client client;

	public GameNetwork(Server server, Client client) {
		this.server = server;
		this.client = client;
		assert client != null;
	}

	public void closeServer() {
		if (server != null)
			server.close();
	}

	public Client getClient() {
		return client;
	}

	public void close() {
		client.close();
		closeServer();
	}
}
