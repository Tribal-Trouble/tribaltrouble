package com.oddlabs.router;

public strictfp interface RouterInterface {
    public static final int PORT = 11221;

    void login(SessionID id, SessionInfo info, int client_id);

    void loginSpectator(SessionID id);
}
