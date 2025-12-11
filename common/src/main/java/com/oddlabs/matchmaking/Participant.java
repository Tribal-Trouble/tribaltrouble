package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class Participant implements Serializable {
	@Serial
	private static final long serialVersionUID = -3344403341742210958L;

	private final int match_id;
	private final @NonNull String nick;
	private final int team;
	private final int race;

	public Participant(int match_id, @NonNull String nick, int team, int race) {
		this.match_id = match_id;
		this.nick = nick;
		this.team = team;
		this.race = race;
	}

	public boolean validate() {
		return team >= 0 && team < MatchmakingServerInterface.MAX_PLAYERS;
	}

	public int getMatchID() {
		return match_id;
	}

	public @NonNull String getNick() {
		return nick;
	}

	public int getTeam() {
		return team;
	}

	public int getRace() {
		return race;
	}

	@Override
	public int hashCode() {
		return match_id ^ team;
	}

	@Override
	public boolean equals(@Nullable Object other) {
        return other instanceof Participant other_part &&
				match_id == other_part.match_id &&
				team == other_part.team &&
				race == other_part.race;
    }
}
