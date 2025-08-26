package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.resource.WorldGenerator;

public strictfp interface GameClientInterface {
    public void setWorldGeneratorAndPlayerSlot(
            Game game, WorldGenerator generator, short player_slot, int player_count);

    public void setPlayers(PlayerSlot[] players);

    public void startGame(int session_id);

    public void chat(int player_slot, String chat);
}
