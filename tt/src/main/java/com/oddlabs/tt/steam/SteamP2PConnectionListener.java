package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamNativeHandle;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.tt.p2p.P2PIdentifier;
import org.jspecify.annotations.NonNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Accepts incoming Steam P2P connections on one channel, forwarding them to a
 * {@link ConnectionListenerInterface} exactly like the TCP and tunnelled listeners do. The
 * listener interface answers each {@code incomingConnection} callback with either
 * {@code acceptConnection} or {@code rejectConnection}.
 */
public final class SteamP2PConnectionListener extends AbstractConnectionListener {
    private final int channel;
    private final List<SteamP2PConnection> incoming_connections = new LinkedList<>();
    private boolean open = true;

    public SteamP2PConnectionListener(int channel, ConnectionListenerInterface listener_interface) {
        super(listener_interface);
        this.channel = channel;
        SteamP2P.getInstance().setListener(channel, this);
    }

    void incoming(@NonNull SteamP2PConnection conn) {
        incoming_connections.add(conn);
        String name = SteamManager.getInstance() != null ? SteamManager.getInstance().getFriendPersonaName(
                conn.getRemoteID()) : conn.getRemoteID().toString();
        notifyIncomingConnection(new P2PIdentifier(SteamNativeHandle.getNativeHandle(conn.getRemoteID()), name));
    }

    private SteamP2PConnection getNextConnection() {
        return incoming_connections.removeFirst();
    }

    @Override
    protected @NonNull AbstractConnection doAcceptConnection(ConnectionInterface conn_interface) {
        SteamP2PConnection conn = getNextConnection();
        conn.setConnectionInterface(conn_interface);
        conn.accept();
        return conn;
    }

    @Override
    public void rejectConnection() {
        getNextConnection().reject();
    }

    @Override
    public void close() {
        if (open) {
            open = false;
            if (SteamP2P.isCreated())
                SteamP2P.getInstance().setListener(channel, null);
        }
    }
}
