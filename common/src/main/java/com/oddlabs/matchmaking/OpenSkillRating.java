package com.oddlabs.matchmaking;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record OpenSkillRating(
                              String nick,
                              double mu,
                              double sigma
) {
}
