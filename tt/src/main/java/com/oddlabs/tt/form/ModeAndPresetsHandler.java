package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.matchmaking.Preset;
import org.jspecify.annotations.NonNull;

/**
 * Reactions {@link ModeAndPresetsPanel} fires up to whoever owns it (typically {@link TerrainMenu}). The panel renders
 * the cards and detects clicks; the handler decides what those clicks mean for the rest of the dialog (apply a preset
 * to form fields, open the save modal, write to disk, etc.).
 */
public interface ModeAndPresetsHandler {
    void modeChosen(@NonNull GameMode mode);

    void presetChosen(@NonNull Preset preset);

    void presetDeleted(@NonNull Preset preset);

    void saveClicked();
}
