package com.oddlabs.net;

import java.io.IOException;

public interface ConnectionListenerInterface {
	void error(AbstractConnectionListener listener, IOException e);
	void incomingConnection(AbstractConnectionListener listener, Object address);
}
