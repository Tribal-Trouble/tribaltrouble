package com.oddlabs.matchmaking;

import java.net.InetAddress;
import java.security.SignedObject;

public interface MatchmakingServerLoginInterface {
	void setLocalRemoteAddress(InetAddress local_remote_address);
	void login(Login login, SignedObject reg_key, int revision);
	void loginAsGuest(int revision);
	void createUser(Login login, LoginDetails login_details, SignedObject reg_key, int revision);
}
