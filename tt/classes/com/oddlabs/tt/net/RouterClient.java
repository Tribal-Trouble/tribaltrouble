package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.Connection;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.IllegalARMIEventException;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.GameInterface;
import com.oddlabs.router.RouterClientInterface;
import com.oddlabs.router.RouterInterface;
import com.oddlabs.router.SessionID;
import com.oddlabs.router.SessionInfo;
import com.oddlabs.util.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class RouterClient implements ConnectionInterface {
	private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(RouterClientInterface.class);
	private final AbstractConnection connection;
	private final GameInterface game_interface;
	private final RouterHandler router_handler;

	public RouterClient(NetworkSelector network, RouterHandler router_handler, int port) {
		this.router_handler = router_handler;
		this.connection = new Connection(network, new InetSocketAddress(Utils.getLoopbackAddress(), port), this);
		this.game_interface = (GameInterface)ARMIEvent.createProxy(connection, GameInterface.class);
	}

	public RouterClient(NetworkSelector network, String address, RouterHandler router_handler) {
		this.router_handler = router_handler;
		this.connection = new Connection(network, address, RouterInterface.PORT, this);
		this.game_interface = (GameInterface)ARMIEvent.createProxy(connection, GameInterface.class);
	}

	public void connect(SessionID session_id, SessionInfo session_info, int client_id) {
		RouterInterface router_interface = (RouterInterface)ARMIEvent.createProxy(connection, RouterInterface.class);
		router_interface.login(session_id, session_info, client_id);
	}

	public GameInterface getInterface() {
		return game_interface;
	}
	
        @Override
	public void handle(Object sender, ARMIEvent armi_event) {
		try {
			armi_event.execute(interface_methods, router_handler);
		} catch (IllegalARMIEventException e) {
			close();
			router_handler.routerFailed(e);
		}
	}

        @Override
	public void writeBufferDrained(AbstractConnection conn) {
	}

        @Override
	public void connected(AbstractConnection conn) {
	}

        @Override
	public void error(AbstractConnection conn, IOException e) {
		router_handler.routerFailed(e);
	}

	public void close() {
		connection.close();
	}
}
