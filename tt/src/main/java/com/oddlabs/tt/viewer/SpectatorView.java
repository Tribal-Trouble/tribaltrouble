package com.oddlabs.tt.viewer;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** What a spectator is looking at: the player being followed, or a free camera. */
public final class SpectatorView {
    public static final int FREE_CAMERA = -1;

    private final @NonNull WorldViewer viewer;
    private int followed = FREE_CAMERA;
    private @Nullable Runnable listener;

    SpectatorView(@NonNull WorldViewer viewer) {
        this.viewer = viewer;
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
        followed = FREE_CAMERA;
        changed();
    }

    private void changed() {
        if (listener != null)
            listener.run();
    }
}
