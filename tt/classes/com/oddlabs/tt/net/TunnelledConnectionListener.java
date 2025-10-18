package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.HostSequenceID;

import java.net.InetAddress;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.List;

public final class TunnelledConnectionListener extends AbstractConnectionListener {
	private final List incoming_connections = new LinkedList();
	private boolean open = true;

	public TunnelledConnectionListener(ConnectionListenerInterface listener_interface) {
		super(listener_interface);
		Network.getMatchmakingClient().registerTunnelledListener(this);
	}

	public void requestTunnelledConnection(HostSequenceID address, InetAddress inet_address, InetAddress local_address, Profile profile) {
		TunnelledConnection conn = new TunnelledConnection(address, null);
		incoming_connections.add(conn);
		notifyIncomingConnection(new TunnelIdentifier(profile, new TunnelAddress(address.getHostID(), inet_address, local_address)));
	}
	
	private TunnelledConnection getNextTunnel() {
		return (TunnelledConnection)incoming_connections.remove(0);
	}

        @Override
	protected AbstractConnection doAcceptConnection(ConnectionInterface connection_interface) {
		TunnelledConnection conn = getNextTunnel();
		conn.setConnectionInterface(connection_interface);
		conn.accept();
		return conn;
	}
	
        @Override
	public void rejectConnection() {
		getNextTunnel().close();
	}

	public void connectionClosed() {
		open = false;
		notifyError(new ClosedChannelException());
	}

        @Override
	public void close() {
		if (open) {
			Network.getMatchmakingClient().unregisterTunnelledListener(this);
			open = false;
		}
	}
}
