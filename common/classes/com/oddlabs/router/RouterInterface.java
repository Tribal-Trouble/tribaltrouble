package com.oddlabs.router;

public interface RouterInterface {
	public final static int PORT = 11221;

	void login(SessionID id, SessionInfo info, int client_id);
}
