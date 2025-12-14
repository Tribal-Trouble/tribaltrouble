package com.oddlabs.matchserver.models;

import com.oddlabs.matchmaking.Profile;

public class GamePlayerModel {
    private String playerName;
    private String playerRace;
    private Integer playerTeam;
    private Profile profile;

    public GamePlayerModel(String playerName, String playerRace, Integer playerTeam) {
        this.playerName = playerName;
        this.playerRace = playerRace;
        this.playerTeam = playerTeam;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerRace() {
        return playerRace;
    }

    public Integer getPlayerTeam() {
        return playerTeam;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}
