package com.oddlabs.router;

import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionListener;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.NetworkSelector;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Router implements ConnectionListenerInterface {
    private final Logger logger;
    private final @NonNull SessionManager manager;
    private final @NonNull AbstractConnectionListener listener;
    private final Set<RouterClient> clients = new LinkedHashSet<>();
    private final RouterListener router_listener;
    private final int port;

    public Router(@NonNull NetworkSelector network, Logger logger) {
        this(network, null, RouterInterface.PORT, logger, null);
    }

    public Router(@NonNull NetworkSelector network, InetAddress address, int port, Logger logger,
            RouterListener router_listener) {
        this.manager = new SessionManager(network.getTimeManager(), logger);
        this.logger = logger;
        this.router_listener = router_listener;
        ConnectionListener tmp_listener = new ConnectionListener(network, address, port, this);
        this.listener = tmp_listener;
        this.port = tmp_listener.getPort();
    }

    public int getPort() {
        return port;
    }

    public long getNextTimeout() {
        return manager.getNextTimeout();
    }

    public void process() {
        manager.process();
    }

    @Override
    public void incomingConnection(@NonNull AbstractConnectionListener connection_listener, Object remote_address) {
        logger.log(Level.INFO, "Incoming connection from {0}", remote_address);
        AbstractConnection conn = connection_listener.acceptConnection(null);
        RouterClient client = new RouterClient(manager, conn, logger, this);
        clients.add(client);
        conn.setConnectionInterface(client);
    }

    void removeClient(RouterClient client) {
        clients.remove(client);
    }

    @Override
    public void error(AbstractConnectionListener conn_id, IOException e) {
        close();
        logger.log(Level.SEVERE, "Server socket failed: {0}", e);
        if (router_listener != null)
            router_listener.routerFailed(e);
    }

    public void close() {
        if (listener != null)
            listener.close();
        Iterator<RouterClient> it = clients.iterator();
        while (it.hasNext()) {
            RouterClient client = it.next();
            it.remove();
            client.close(false);
        }
    }
}
