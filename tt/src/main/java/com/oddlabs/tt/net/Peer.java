package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.net.IllegalARMIEventException;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.PlayerInterface;
import org.jspecify.annotations.NonNull;

import java.util.LinkedList;
import java.util.List;

public final class Peer implements PeerHubInterface {
	private final GameArgumentReader argument_reader;
	private final int peer_index;
	private final Player player;
	private final PeerHub peer_hub;
	private final List<GameEvent> event_queue = new LinkedList<>();
	private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(PlayerInterface.class);
	private final PeerHubInterface peerhub_interface;
	
	public Peer(PeerHub peer_hub, int peer_index, Player player, GameArgumentReader argument_reader, PeerHubInterface peerhub_interface) {
		this.peerhub_interface = peerhub_interface;
		this.peer_index = peer_index;
		this.peer_hub = peer_hub;
		this.player = player;
		this.argument_reader = argument_reader;
	}

	public int getPeerIndex() {
		return peer_index;
	}

        @Override
	public @NonNull String toString() {
		return "player: " + player.toString();
	}

	public void addEvent(int tick, ARMIEvent event) {
		event_queue.add(new GameEvent(tick, event));
	}

	public void executeEvents(int tick) throws IllegalARMIEventException {
		while (!event_queue.isEmpty()) {
			GameEvent game_event = event_queue.getFirst();
			if (game_event.tick != tick)
				return;
			event_queue.removeFirst();
			game_event.event.execute(interface_methods, argument_reader, player);
		}
	}
	
        @Override
	public void chat(String text, boolean team) {
		peer_hub.receiveChat(player.getPlayerInfo().getName(), text, team);
	}

        @Override
	public void beacon(float x, float y) {
		peer_hub.receiveBeacon(x, y, player.getPlayerInfo().getName());
	}

	public PeerHubInterface getPeerHubInterface() {
		return peerhub_interface;
	}
	
	public PlayerInfo getPlayerInfo() {
		return player.getPlayerInfo();
	}
	
	public Player getPlayer() {
		return player;
	}

	private final static class GameEvent {
		private final int tick;
		private final ARMIEvent event;
		
		GameEvent(int tick, ARMIEvent event) {
			this.tick = tick;
			this.event = event;
		}
	}
}
