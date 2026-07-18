package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A non-focusable icon button that trains a chieftain. */
public class ChieftainButton extends NonFocusIconButton {
    private final @NonNull PlayerInterface player_interface;
    private @Nullable Building current_building;

    public ChieftainButton(@NonNull WorldViewer viewer, @NonNull PlayerInterface player_interface,
            @NonNull ModeIconQuads icon) {
        super(icon, GameAction.TRAIN_CHIEFTAIN, () -> ActionButtonPanel.i18n(
                "train_chieftain_tip", Renderer.getLocalInput().getInputManager().getBindingString(
                        GameAction.TRAIN_CHIEFTAIN)));
        this.player_interface = player_interface;
        setCanFocus(true);
    }

    public final void setBuilding(@NonNull Building current_building) {
        this.current_building = current_building;
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        player_interface.trainChieftain(current_building, !current_building.getChieftainContainer().isTraining());
    }

    @Override
    protected final void postRender(@NonNull GUIRenderer renderer) {
        if (current_building.isAlive() && current_building.getChieftainContainer().isTraining()) {
            var watchQuad = GUIIcons.getIcons().getWatch(getProgress());
            renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
        }
    }

    protected final float getProgress() {
        return current_building.isAlive() ? current_building.getChieftainContainer().getBuildProgress() : 0;
    }
}
