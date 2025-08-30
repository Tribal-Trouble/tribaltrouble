package com.oddlabs.matchserver.models;

import java.util.ArrayList;

public class VersusMatchupResultModel {
    private String player1;
    private String player2;
    private int player1Wins;
    private int player2Wins;
    private int neitherWins;
    ArrayList<VersusMatchupModel> recentMatchups;

    public VersusMatchupResultModel(
            String player1,
            String player2,
            int player1Wins,
            int player2Wins,
            int neitherWins,
            ArrayList<VersusMatchupModel> recentMatchups) {
        this.player1 = player1;
        this.player2 = player2;
        this.player1Wins = player1Wins;
        this.player2Wins = player2Wins;
        this.neitherWins = neitherWins;
        this.recentMatchups = recentMatchups;
    }

    public String getPlayer1() {
        return player1;
    }

    public String getPlayer2() {
        return player2;
    }

    public int getPlayer1Wins() {
        return player1Wins;
    }

    public int getPlayer2Wins() {
        return player2Wins;
    }

    public int getTotalGamesPlayed() {
        return player1Wins + player2Wins + neitherWins;
    }

    public ArrayList<VersusMatchupModel> getRecentMatchups() {
        return recentMatchups;
    }
}
