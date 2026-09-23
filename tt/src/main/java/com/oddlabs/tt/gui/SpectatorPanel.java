package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.PlayerView;
import com.oddlabs.tt.viewer.Selection;
import com.oddlabs.tt.viewer.SpectatorView;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** The watched player's action panel, read only: it shows their selection, their rally point and their submenu. */
public final class SpectatorPanel extends GUIObject implements Animated {
    private final @NonNull WorldViewer viewer;
    private final @NonNull GUIRoot gui_root;
    private final @NonNull BooleanSupplier visible;

    private record Copy(@NonNull ActionButtonPanel panel, @NonNull Selection selection) {
    }

    private final @NonNull Map<Player, Copy> copies = new HashMap<>();
    private @Nullable Player shown;
    private @Nullable Selection selection;
    private @Nullable ActionButtonPanel panel;
    private int selection_version = -1;
    private boolean just_shown;

    public SpectatorPanel(@NonNull WorldViewer viewer, @NonNull GUIRoot gui_root, @NonNull BooleanSupplier visible) {
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
    protected void doAdd() {
        super.doAdd();
        viewer.getAnimationManagerLocal().registerAnimation(this);
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        viewer.getAnimationManagerLocal().removeAnimation(this);
    }

    @Override
    public void animate(float t) {
        SpectatorView view = viewer.getSpectatorView();
        Player followed = view != null && visible.getAsBoolean() ? view.getFollowedPlayer() : null;
        if (followed != shown)
            show(followed);
        if (followed == null || panel == null || selection == null)
            return;
        PlayerView player_view = view.getView(followed);
        boolean resend = player_view.getSelectionVersion() != selection_version;
        if (resend || hasDead(selection)) {
            selection_version = player_view.getSelectionVersion();
            selection.clearSelection();
            for (Selectable<?> s : player_view.getSelection()) {
                if (!s.isDead())
                    selection.getCurrentSelection().add(s);
            }
            if (just_shown)
                panel.animate(t);
        }
        just_shown = false;
        panel.refreshCounters();
        panel.setSubmenu(player_view.getPanelSubmenu());
    }

    private static boolean hasDead(@NonNull Selection selection) {
        for (Selectable<?> s : selection.getCurrentSelection().getSet()) {
            if (s.isDead())
                return true;
        }
        return false;
    }

    private void show(@Nullable Player player) {
        if (panel != null)
            panel.remove();
        if (selection != null)
            selection.clearSelection();
        panel = null;
        viewer.getRenderer().setSelectedBuilding(null);
        selection = null;
        selection_version = -1;
        shown = player;
        if (player == null)
            return;
        just_shown = true;
        Copy copy = copies.computeIfAbsent(player, this::copyFor);
        selection = copy.selection();
        panel = copy.panel();
        addChild(panel);
    }

    private @NonNull Copy copyFor(@NonNull Player player) {
        Selection copy_selection = new Selection(player);
        return new Copy(new ActionButtonPanel(viewer, viewer.getCamera(), gui_root.getWidth(), gui_root.getHeight(),
                player, copy_selection, true), copy_selection);
    }
}
