package com.oddlabs.tt.gui;

import com.oddlabs.tt.delegate.SelectionDelegate;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.viewer.PlayerView;
import com.oddlabs.tt.viewer.SpectatorView;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.function.BooleanSupplier;

/** The watched player's drag selection rectangle, scaled to this viewport, in the game's usual selection color. */
public final class SpectatorSelectionBox extends GUIObject {
    private final @NonNull WorldViewer viewer;
    private final @NonNull GUIRoot gui_root;
    private final @NonNull BooleanSupplier visible;

    public SpectatorSelectionBox(@NonNull WorldViewer viewer, @NonNull GUIRoot gui_root,
            @NonNull BooleanSupplier visible) {
        this.viewer = viewer;
        this.gui_root = gui_root;
        this.visible = visible;
        displayChangedNotify(gui_root.getWidth(), gui_root.getHeight());
    }

    @Override
    protected void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        SpectatorView view = viewer.getSpectatorView();
        Player followed = view != null && visible.getAsBoolean() ? view.getFollowedPlayer() : null;
        PlayerView player_view = followed != null ? view.getView(followed) : null;
        if (player_view == null || !player_view.isSelectionBoxActive())
            return;
        float width = gui_root.getWidth();
        float height = gui_root.getHeight();
        SelectionDelegate.drawSelectionBox(renderer, player_view.getSelectionBoxX1() * width,
                player_view.getSelectionBoxY1() * height, player_view.getSelectionBoxX2() * width,
                player_view.getSelectionBoxY2() * height);
    }
}
