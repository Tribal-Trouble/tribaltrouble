package com.oddlabs.tt.viewer;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** What a spectator is looking at: the player being followed, or a free camera. */
public final class SpectatorView {
    public static final int FREE_CAMERA = -1;

    private final @NonNull WorldViewer viewer;
    private final @NonNull PlayerView @NonNull [] views;
    private int followed = FREE_CAMERA;
    private boolean snap_pending;
    private @Nullable Runnable listener;

    SpectatorView(@NonNull WorldViewer viewer) {
        this.viewer = viewer;
        this.views = new PlayerView[viewer.getWorld().getPlayers().length];
        for (int i = 0; i < views.length; i++)
            views[i] = new PlayerView();
    }

    public @NonNull PlayerView getView(@NonNull Player player) {
        Player[] players = viewer.getWorld().getPlayers();
        for (int i = 0; i < players.length; i++) {
            if (players[i] == player)
                return views[i];
        }
        throw new IllegalArgumentException("Unknown player " + player);
    }

    /** The followed view once a camera sample has arrived, or null for a free camera. */
    public @Nullable PlayerView getFollowedView() {
        if (followed == FREE_CAMERA || !views[followed].hasCamera())
            return null;
        return views[followed];
    }

    public boolean consumeSnap() {
        boolean snap = snap_pending;
        snap_pending = false;
        return snap;
    }

    void receiveCamera(@NonNull Player player, float x, float y, float z, float horiz_angle, float vert_angle) {
        getView(player).setCamera(x, y, z, horiz_angle, vert_angle);
    }

    public void setListener(@Nullable Runnable listener) {
        this.listener = listener;
    }

    public int getFollowed() {
        return followed;
    }

    public @Nullable Player getFollowedPlayer() {
        Player[] players = viewer.getWorld().getPlayers();
        return followed >= 0 && followed < players.length ? players[followed] : null;
    }

    public void follow(int index) {
        Player[] players = viewer.getWorld().getPlayers();
        if (players.length == 0)
            return;
        followed = Math.floorMod(index, players.length);
        snap_pending = true;
        Player player = players[followed];
        Unit chieftain = player.getChieftain();
        if (chieftain != null && !chieftain.isDead())
            viewer.getCamera().setPos(chieftain.getPositionX(), chieftain.getPositionY());
        else
            viewer.getCamera().setPos(player.getStartX(), player.getStartY());
        changed();
    }

    public void next() {
        follow(followed + 1);
    }

    public void previous() {
        follow(followed == FREE_CAMERA ? viewer.getWorld().getPlayers().length - 1 : followed - 1);
    }

    public void freeCamera() {
        if (followed == FREE_CAMERA)
            return;
        followed = FREE_CAMERA;
        changed();
    }

    private void changed() {
        if (listener != null)
            listener.run();
    }
}
