package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.resource.WorldGenerator;

public interface GameClientInterface {
    void setWorldGeneratorAndPlayerSlot(Game game, WorldGenerator generator, short player_slot, int player_count);

    void setPlayers(PlayerSlot[] players);

    void startGame(int session_id);

    void chat(int player_slot, String chat);
}
