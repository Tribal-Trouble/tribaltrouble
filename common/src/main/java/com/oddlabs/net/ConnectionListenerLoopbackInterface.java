package com.oddlabs.net;

import java.io.IOException;
import java.net.InetAddress;

public interface ConnectionListenerLoopbackInterface {
    void error(IOException e);

    void incoming(InetAddress remote_address);
}
