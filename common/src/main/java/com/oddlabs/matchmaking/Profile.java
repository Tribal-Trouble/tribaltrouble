package com.oddlabs.matchmaking;

import java.io.Serial;
import java.io.Serializable;

public final class Profile implements Serializable {
    @Serial
    private static final long serialVersionUID = -3399364532017471737L;

    private final String nick;
    private final int rating;
    private final int wins;
    private final int losses;
    private final int invalid;
    private final int revision;

    public Profile(String nick, int rating, int wins, int losses, int invalid, int revision) {
        this.nick = nick;
        this.rating = rating;
        this.wins = wins;
        this.losses = losses;
        this.invalid = invalid;
        this.revision = revision;
    }

    @Override
    public String toString() {
        return nick;
    }

    public String getNick() {
        return nick;
    }

    public int getRating() {
        return rating;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getInvalid() {
        return invalid;
    }

    public int getRevision() {
        return revision;
    }
}
