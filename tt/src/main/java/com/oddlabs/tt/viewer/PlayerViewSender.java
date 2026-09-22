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
    private boolean map_mode_sent;
    private final int @NonNull [] box = new int[4];
    private boolean box_sent;
    private boolean sent_box_active;
    private float sent_box_x1;
    private float sent_box_y1;
    private float sent_box_x2;
    private float sent_box_y2;
    private boolean sent_map_mode;
    private boolean sent_on_map;
    private float sent_cursor_x;
    private float sent_cursor_y;
    private boolean picked;
    private boolean picked_on_map;
    private int picked_mouse_x;
    private int picked_mouse_y;
    private float picked_camera_x;
    private float picked_camera_y;
    private float picked_camera_z;
    private float picked_horiz_angle;
    private float picked_vert_angle;
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
        sendMapMode(out);
        sendSelectionBox(out);
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
        boolean on_map = over_world && pick();
        if (cursor_sent && on_map == sent_on_map && (!on_map || (same(location.x, sent_cursor_x) && same(location.y,
                sent_cursor_y))))
            return;
        sent_cursor_x = on_map ? location.x : 0f;
        sent_cursor_y = on_map ? location.y : 0f;
        out.viewCursor(sent_cursor_x, sent_cursor_y, on_map);
        cursor_sent = true;
        sent_on_map = on_map;
    }

    /** Picks the landscape under the pointer; the last result is reused while the pointer and camera rest. */
    private boolean pick() {
        CameraState state = viewer.getCamera().getState();
        int mouse_x = Renderer.getLocalInput().getMouseX();
        int mouse_y = Renderer.getLocalInput().getMouseY();
        if (picked && mouse_x == picked_mouse_x && mouse_y == picked_mouse_y
                && state.getCurrentX() == picked_camera_x && state.getCurrentY() == picked_camera_y
                && state.getCurrentZ() == picked_camera_z && state.getHorizAngle() == picked_horiz_angle
                && state.getCurrentVertAngle() == picked_vert_angle)
            return picked_on_map;
        picked = true;
        picked_mouse_x = mouse_x;
        picked_mouse_y = mouse_y;
        picked_camera_x = state.getCurrentX();
        picked_camera_y = state.getCurrentY();
        picked_camera_z = state.getCurrentZ();
        picked_horiz_angle = state.getHorizAngle();
        picked_vert_angle = state.getCurrentVertAngle();
        picked_on_map = viewer.getPicker().pickLocation(state, location);
        return picked_on_map;
    }

    private void sendMapMode(@NonNull PlayerInterface out) {
        boolean on = viewer.getDelegate().isOnMap();
        if (map_mode_sent && on == sent_map_mode)
            return;
        out.viewMapMode(on);
        map_mode_sent = true;
        sent_map_mode = on;
    }

    private void sendSelectionBox(@NonNull PlayerInterface out) {
        GUIRoot gui_root = viewer.getGUIRoot();
        boolean active = viewer.getDelegate().getSelectionBox(box) && gui_root.getDelegate() == viewer.getDelegate();
        float x1 = active ? box[0] / (float) gui_root.getWidth() : 0f;
        float y1 = active ? box[1] / (float) gui_root.getHeight() : 0f;
        float x2 = active ? box[2] / (float) gui_root.getWidth() : 0f;
        float y2 = active ? box[3] / (float) gui_root.getHeight() : 0f;
        if (box_sent && active == sent_box_active && (!active || (same(x1, sent_box_x1) && same(y1, sent_box_y1)
                && same(x2, sent_box_x2) && same(y2, sent_box_y2))))
            return;
        out.viewSelectionBox(x1, y1, x2, y2, active);
        box_sent = true;
        sent_box_active = active;
        sent_box_x1 = x1;
        sent_box_y1 = y1;
        sent_box_x2 = x2;
        sent_box_y2 = y2;
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
