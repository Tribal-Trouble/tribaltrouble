package com.oddlabs.router;

public interface RouterInterface {
	int PORT = 11221;

	void login(SessionID id, SessionInfo info, int client_id);
}
