package com.oddlabs.tt.viewer;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.ActionButtonPanel;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.net.ChatCommand;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.BuildingGhostRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.RenderQueues;
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
    private @Nullable BuildingGhostRenderer ghost;

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

    void receiveMapMode(@NonNull Player player, boolean on) {
        getView(player).setMapMode(on);
    }

    /** Beacons the watched player would see: their own and their teammates'. */
    void receiveBeacon(@NonNull Player player, float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y))
            return;
        int size = viewer.getWorld().getHeightMap().getMetersPerWorld();
        x = Math.clamp(x, 0f, size);
        y = Math.clamp(y, 0f, size);
        Player followed = getFollowedPlayer();
        if (followed == null || !viewer.getPeerHub().isSynchronized())
            return;
        if (player != followed && player.getPlayerInfo().getTeam() != followed.getPlayerInfo().getTeam())
            return;
        if (ChatCommand.isIgnoring(player.getPlayerInfo().getName()))
            return;
        viewer.getNotificationManager().newBeacon(viewer.getAnimationManagerLocal(), followed, x, y);
    }

    void receiveSelectionBox(@NonNull Player player, float x1, float y1, float x2, float y2, boolean active) {
        if (!Float.isFinite(x1) || !Float.isFinite(y1) || !Float.isFinite(x2) || !Float.isFinite(y2))
            return;
        getView(player).setSelectionBox(Math.clamp(x1, 0f, 1f), Math.clamp(y1, 0f, 1f), Math.clamp(x2, 0f, 1f),
                Math.clamp(y2, 0f, 1f), active);
    }

    void receivePlacing(@NonNull Player player, int building_index, int grid_x, int grid_y, boolean placing) {
        int grid_size = viewer.getWorld().getUnitGrid().getGridSize();
        if (building_index < 0 || building_index >= Race.NUM_BUILDINGS || grid_x < 0
                || grid_x >= grid_size || grid_y < 0 || grid_y >= grid_size)
            placing = false;
        getView(player).setPlacing(building_index, grid_x, grid_y, placing);
    }

    /** Draws the building the watched player is placing, after the delegate has drawn its own 3D overlay. */
    public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues queues,
            @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        Player followed = getFollowedPlayer();
        if (followed == null || !Globals.draw_hud || viewer.getGUIRoot().getDelegate() != viewer.getDelegate())
            return;
        PlayerView view = getView(followed);
        if (!view.isPlacing() || view.isMapMode() || viewer.getDelegate().isInMapMode())
            return;
        if (ghost == null)
            ghost = new BuildingGhostRenderer();
        ghost.render(viewer.getWorld(), followed.getRace().getBuildingTemplate(view.getPlacingBuildingIndex()),
                view.getPlacingGridX(), view.getPlacingGridY(), renderer, queues, modelViewStack, projectionStack);
    }

    void receivePanelMenu(@NonNull Player player, int submenu) {
        if (submenu < ActionButtonPanel.SUBMENU_NONE || submenu > ActionButtonPanel.SUBMENU_TRANSPORT)
            submenu = ActionButtonPanel.SUBMENU_NONE;
        getView(player).setPanelSubmenu(submenu);
    }

    void receiveSelection(@NonNull Player player, Selectable<?> @NonNull [] selection) {
        getView(player).setSelection(selection);
    }

    void playerLeft(@NonNull Player player) {
        PlayerView view = getView(player);
        view.setCursor(0f, 0f, false);
        view.setMapMode(false);
        view.setSelectionBox(0f, 0f, 0f, 0f, false);
        view.setPlacing(0, 0, 0, false);
        view.setPanelSubmenu(ActionButtonPanel.SUBMENU_NONE);
        view.setSelection(new Selectable<?>[0]);
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
        viewer.getNotificationManager().clear();
        if (listener != null)
            listener.run();
    }
}
