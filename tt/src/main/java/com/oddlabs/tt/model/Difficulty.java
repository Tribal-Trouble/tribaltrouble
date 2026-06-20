package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Game difficulty levels supporting legacy serialization and AI mapping.
 */
public enum Difficulty {
    EASY(0, 1),
    NORMAL(1, 0),
    HARD(2, 2);

    private final int aiValue;
    private final int campaignValue;

    private static final Set<Difficulty> VALUES = EnumSet.allOf(Difficulty.class);

    Difficulty(int aiValue, int campaignValue) {
        this.aiValue = aiValue;
        this.campaignValue = campaignValue;
    }

    /**
     * Returns the integer value representing this difficulty in AI difficulty arrays.
     *
     * @return AI difficulty value
     */
    public int getAiValue() {
        return aiValue;
    }

    /**
     * Returns the integer value representing this difficulty in campaign serialization.
     *
     * @return campaign difficulty value
     */
    public int getCampaignValue() {
        return campaignValue;
    }

    /**
     * Resolves the {@link Difficulty} associated with the given AI integer value.
     *
     * @param aiValue AI difficulty integer
     * @return corresponding {@link Difficulty}
     */
    public static @NonNull Difficulty fromAiValue(int aiValue) {
        for (Difficulty diff : VALUES) {
            if (diff.aiValue == aiValue) {
                return diff;
            }
        }
        throw new IllegalArgumentException("Unknown AI difficulty value: " + aiValue);
    }

    /**
     * Resolves the {@link Difficulty} associated with the given campaign serialization integer value.
     *
     * @param campaignValue campaign serialization difficulty integer
     * @return corresponding {@link Difficulty}
     */
    public static @NonNull Difficulty fromCampaignValue(int campaignValue) {
        for (Difficulty diff : VALUES) {
            if (diff.campaignValue == campaignValue) {
                return diff;
            }
        }
        throw new IllegalArgumentException("Unknown campaign difficulty value: " + campaignValue);
    }
}
