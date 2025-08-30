package com.oddlabs.matchserver.models;

import com.oddlabs.matchserver.WebsiteLinkHelper;

public class VersusMatchupModel {
    private String player1;
    private String player2;
    private String winner;
    private int game_id;
    private String game_name;
    private String map_seed;
    private java.sql.Timestamp startTime;

    public VersusMatchupModel(
            String player1,
            String player2,
            String winner,
            int game_id,
            String game_name,
            String map_seed,
            java.sql.Timestamp startTime) {
        this.player1 = player1;
        this.player2 = player2;
        this.winner = winner;
        this.game_id = game_id;
        this.game_name = game_name;
        this.map_seed = map_seed;
        this.startTime = startTime;
    }

    public int getGameId() {
        return game_id;
    }

    public java.sql.Timestamp getStartTime() {
        return startTime;
    }

    public String getGameName() {
        return game_name;
    }

    public String getMapSeed() {
        return map_seed;
    }

    public String getPlayer1() {
        return player1;
    }

    public String getPlayer2() {
        return player2;
    }

    public String getWinner() {
        return winner;
    }

    public String getGameReplayUrl() {
        return WebsiteLinkHelper.getReplayUrl(game_id);
    }
}
