package com.oddlabs.tt.net;

public interface StallHandler {
	void stopStall();
	void processStall(int tick);
	void peerhubFailed();
}
