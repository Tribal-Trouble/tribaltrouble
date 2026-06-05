package com.oddlabs.tt.gamemode;

import com.oddlabs.matchmaking.GameModeOption;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Per-mode rule bundle. Each mode supplies one implementation. Add a method here when a new rule needs to vary by
 * mode; each mode's implementation provides it.
 */
public interface GameModeRules {
    @NonNull
    List<@NonNull GameModeOption> getOptions();

    /**
     * Is this player still in the game? Standard mode: has units OR an active chieftain OR a Quarters. Other modes
     * override (e.g. Protect the Chief: chief is alive).
     */
    boolean isPlayerAlive(@NonNull Player player);

    /**
     * Called once at game start after the {@link WorldViewer} is constructed. Standard mode is a no-op. Other modes
     * register triggers, spawn mode-specific objects, and add HUD elements here.
     */
    void onGameStart(@NonNull WorldViewer viewer);
}
