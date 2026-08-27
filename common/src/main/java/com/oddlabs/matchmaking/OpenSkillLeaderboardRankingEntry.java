package com.oddlabs.matchmaking;

import java.io.Serializable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record OpenSkillLeaderboardRankingEntry(
                                               int rank,
                                               String nick,
                                               int rating,
                                               boolean provisional,
                                               double mu,
                                               double sigma
) implements Serializable {
}
