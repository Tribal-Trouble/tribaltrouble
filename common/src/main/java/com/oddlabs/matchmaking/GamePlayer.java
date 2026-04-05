package com.oddlabs.matchmaking;

import java.io.Serializable;

public class GamePlayer implements Serializable {
    private String _nick;
    private int _team;
    private int _race;
    private PlayerTypes _playerType;

    public GamePlayer(String nick, int team, int race, PlayerTypes playerType) {
        _nick = nick;
        _team = team;
        _race = race;
        _playerType = playerType;
        if (_playerType != PlayerTypes.Human) {
            switch (_playerType) {
                case AIEasy:
                    _nick = "AI Easy";
                    break;
                case AINormal:
                    _nick = "AI Normal";
                    break;
                case AIHard:
                    _nick = "AI Hard";
                    break;
                default:
                    _nick = "Unknown player type";
            }
        }
    }

    public PlayerTypes getPlayerType() {
        return _playerType;
    }

    public String getNick() {
        return _nick;
    }

    public int getTeam() {
        return _team;
    }

    public int getRace() {
        return _race;
    }
}
