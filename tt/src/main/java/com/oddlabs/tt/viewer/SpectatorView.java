package com.oddlabs.tt.viewer;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.model.Selectable;
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
    private int last_followed = FREE_CAMERA;
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
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(horiz_angle)
                || !Float.isFinite(vert_angle))
            return;
        int size = viewer.getWorld().getHeightMap().getMetersPerWorld();
        getView(player).setCamera(Math.clamp(x, -size, 2f * size), Math.clamp(y, -size, 2f * size),
                Math.clamp(z, 0f, GameCamera.CINEMATIC_MAX_Z), horiz_angle, vert_angle);
    }

    void receiveCursor(@NonNull Player player, float x, float y, boolean on_map) {
        if (!Float.isFinite(x) || !Float.isFinite(y))
            return;
        int size = viewer.getWorld().getHeightMap().getMetersPerWorld();
        getView(player).setCursor(Math.clamp(x, 0f, size), Math.clamp(y, 0f, size), on_map);
    }

    void receiveSelection(@NonNull Player player, Selectable<?> @NonNull [] selection) {
        getView(player).setSelection(selection);
        if (getFollowedPlayer() == player)
            showFollowedBuilding();
    }

    /**
     * The renderer draws the rally point of whichever building it is told about; the spectator selects nothing itself.
     */
    private void showFollowedBuilding() {
        viewer.getRenderer().setSelectedBuilding(followed == FREE_CAMERA ? null : views[followed].getBuilding());
    }

    void playerLeft(@NonNull Player player) {
        PlayerView view = getView(player);
        view.setCursor(0f, 0f, false);
        view.setSelection(new Selectable<?>[0]);
        showFollowedBuilding();
    }

    public boolean isSelectedByFollowed(@NonNull Selectable<?> selectable) {
        return followed != FREE_CAMERA && views[followed].isSelected(selectable);
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
        viewer.getCamera().stopAutoMotion();
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
        last_followed = followed;
        followed = FREE_CAMERA;
        changed();
    }

    /** Free camera while following; back onto the last watched player while free. */
    public void toggleFreeCamera() {
        if (followed != FREE_CAMERA)
            freeCamera();
        else if (last_followed != FREE_CAMERA)
            follow(last_followed);
    }

    private void changed() {
        showFollowedBuilding();
        if (listener != null)
            listener.run();
    }
}
