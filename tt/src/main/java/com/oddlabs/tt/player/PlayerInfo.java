package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Race;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class PlayerInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 3;

    public static final int TEAM_NEUTRAL = -1;

    private final int race;
    private final @NonNull String name;
    private final int team;

    public PlayerInfo(int team, @NonNull Race race, @NonNull String name) {
        this.team = team;
        this.race = race.getValue();
        this.name = name;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof PlayerInfo player &&
                team == player.team &&
                race == player.race;
    }

    public @NonNull Race getRace() {
        return Race.fromValue(race);
    }

    public @NonNull String getName() {
        return name;
    }

    public int getTeam() {
        return team;
    }

    @Override
    public @NonNull String toString() {
        return name;
    }
}
