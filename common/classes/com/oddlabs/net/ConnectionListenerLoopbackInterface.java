package com.oddlabs.net;

import java.io.IOException;
import java.net.InetAddress;

public interface ConnectionListenerLoopbackInterface {
	public void error(IOException e);
	public void incoming(InetAddress remote_address);
}
