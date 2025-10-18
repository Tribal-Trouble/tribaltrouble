package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.resource.WorldGenerator;

public interface ConfigurationListener extends ErrorListener {
	public void connected(Client client, Game game, WorldGenerator generator, int player_slot);
	public void setPlayers(PlayerSlot[] players);
	public void gameStarted();
}
