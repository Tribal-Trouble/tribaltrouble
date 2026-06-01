package com.oddlabs.tt.gamemode.standard;

import com.oddlabs.matchmaking.GameModeOption;
import com.oddlabs.tt.gamemode.GameModeRules;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class StandardModeRules implements GameModeRules {
    public static final @NonNull String OPTION_RATED = "rated";

    // i18n key from TerrainMenu.properties (existing rated_game / rated_game_tip translations in all 5 locales)
    private static final @NonNull List<@NonNull GameModeOption> OPTIONS = List.of(
            new GameModeOption(OPTION_RATED, GameModeOption.Type.BOOL, Boolean.FALSE, "rated_game"));

    @Override
    public @NonNull List<@NonNull GameModeOption> getOptions() {
        return OPTIONS;
    }

    @Override
    public boolean isPlayerAlive(@NonNull Player player) {
        int units = player.getUnitCountContainer().getNumSupplies();
        return units > 0 || player.hasActiveChieftain() || player.getQuarters() != null;
    }

    @Override
    public void onGameStart(@NonNull WorldViewer viewer) {
        // Standard mode is the original game. Nothing extra to set up.
    }
}
