package com.oddlabs.tt.viewer;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.net.DistributableTable;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Set;

final class PlayerViewSender implements Animated {
    private static final int TICKS_PER_SAMPLE = 5;
    private static final float EPSILON = 0.001f;

    private final @NonNull WorldViewer viewer;
    private final @NonNull LandscapeLocation location = new LandscapeLocation();
    private int ticks;
    private boolean camera_sent;
    private float sent_x;
    private float sent_y;
    private float sent_z;
    private float sent_horiz_angle;
    private float sent_vert_angle;
    private boolean cursor_sent;
    private boolean sent_on_map;
    private float sent_cursor_x;
    private float sent_cursor_y;
    private int @NonNull [] sent_selection = new int[0];

    PlayerViewSender(@NonNull WorldViewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void animate(float t) {
        if (++ticks % TICKS_PER_SAMPLE != 0)
            return;
        PeerHub peerhub = viewer.getPeerHub();
        if (!peerhub.isSynchronized())
            return;
        PlayerInterface out = peerhub.getPlayerInterface();
        sendCamera(out);
        sendCursor(out);
        sendSelection(out);
    }

    private void sendCamera(@NonNull PlayerInterface out) {
        CameraState state = viewer.getCamera().getState();
        float x = state.getTargetX();
        float y = state.getTargetY();
        float z = state.getTargetZ();
        float horiz_angle = state.getTargetHorizAngle();
        float vert_angle = state.getTargetVertAngle();
        if (camera_sent && same(x, sent_x) && same(y, sent_y) && same(z, sent_z) && same(horiz_angle, sent_horiz_angle)
                && same(vert_angle, sent_vert_angle))
            return;
        out.viewCamera(x, y, z, horiz_angle, vert_angle);
        camera_sent = true;
        sent_x = x;
        sent_y = y;
        sent_z = z;
        sent_horiz_angle = horiz_angle;
        sent_vert_angle = vert_angle;
    }

    private void sendCursor(@NonNull PlayerInterface out) {
        GUIRoot gui_root = viewer.getGUIRoot();
        boolean over_world = gui_root.getDelegate().getCamera() == viewer.getCamera()
                && gui_root.getCurrentGUIObject().canHoverBehind()
                && Renderer.getLocalInput().getInputProvider().isCursorInWindow();
        boolean on_map = over_world && viewer.getPicker().pickLocation(viewer.getCamera().getState(), location);
        if (cursor_sent && on_map == sent_on_map && (!on_map || (same(location.x, sent_cursor_x) && same(location.y,
                sent_cursor_y))))
            return;
        sent_cursor_x = on_map ? location.x : 0f;
        sent_cursor_y = on_map ? location.y : 0f;
        out.viewCursor(sent_cursor_x, sent_cursor_y, on_map);
        cursor_sent = true;
        sent_on_map = on_map;
    }

    private void sendSelection(@NonNull PlayerInterface out) {
        Set<Selectable<?>> set = viewer.getSelection().getCurrentSelection().getSet();
        DistributableTable table = viewer.getDistributableTable();
        int[] ids = set.stream().mapToInt(table::getName).toArray();
        if (Arrays.equals(ids, sent_selection))
            return;
        out.viewSelection(set.toArray(new Selectable<?>[0]));
        sent_selection = ids;
    }

    private static boolean same(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }
}
