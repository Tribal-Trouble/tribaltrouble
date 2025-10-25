package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.IllegalARMIEventException;

import java.io.IOException;

public final class ClientInfo implements GameServerInterface, ConnectionInterface {
	private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(GameServerInterface.class);
	private final Server server;
	private final PlayerSlot player_slot;

	public ClientInfo(Server server, PlayerSlot player_slot) {
		this.player_slot = player_slot;
		this.server = server;
	}

        @Override
	public void handle(Object sender, ARMIEvent armi_event) {
		try {
			armi_event.execute(interface_methods, this);
		} catch (IllegalARMIEventException e) {
			server.handleError((AbstractConnection)sender, e);
		}
	}

        @Override
	public void writeBufferDrained(AbstractConnection conn) {
	}

        @Override
	public void error(AbstractConnection conn, IOException e) {
		server.handleError(conn, e);
	}

        @Override
	public void connected(AbstractConnection conn) { 
	}

	public PlayerSlot getPlayerSlot() {
		return player_slot;
	}

        @Override
	public void resetSlotState(int slot, boolean open) {
		server.resetSlotState(player_slot, slot, open);
	}

        @Override
	public void setPlayerSlot(int slot, int type, int race, int team, boolean ready, int ai_difficulty) {
		server.setPlayerSlot(player_slot, slot, type, race, team, ready, ai_difficulty);
	}

        @Override
	public void startServer() {
		server.startServer(player_slot);
	}

        @Override
	public void chat(String chat) {
		server.chat(player_slot, chat);
	}
}
