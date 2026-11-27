package com.oddlabs.tt.input.scripted;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.delegate.InGameDelegate;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.joml.Vector3f;

import java.util.Locale;

/**
 * Text dump of the running game for scripted input: local player, camera, and every unit and building with world and
 * screen coordinates (framebuffer pixels, top-left origin, matching screenshots and the GUI tree).
 */
public final class GameStateDump {
    private GameStateDump() {
    }

    public static @NonNull String dump(@NonNull GUIRoot root, int framebuffer_width, int framebuffer_height) {
        if (!(root.getDelegate() instanceof InGameDelegate delegate)) {
            return "err not in game (delegate " + root.getDelegate().getClass().getSimpleName() + ")\n";
        }
        WorldViewer viewer = delegate.getViewer();
        Player local = viewer.getLocalPlayer();
        Player[] players = viewer.getWorld().getPlayers();
        CameraState camera = viewer.getCamera().getState();
        int[] viewport = {0, 0, framebuffer_width, framebuffer_height};
        Vector3f screen = new Vector3f();
        StringBuilder out = new StringBuilder("ok\n");
        out.append("delegate ").append(delegate.getClass().getSimpleName()).append('\n');
        out.append("tick ").append(LocalEventQueue.getQueue().getHighPrecisionManager().getTick()).append('\n');
        PeerHub hub = viewer.getPeerHub();
        out.append("simtick ").append(hub.getTick()).append('\n');
        for (int[] c : hub.getRecentChecksums()) {
            out.append("checksum ").append(c[0]).append(' ').append(c[1]).append('\n');
        }
        out.append(String.format(Locale.ROOT, "camera target %.1f %.1f %.1f%n", camera.getTargetX(),
                camera.getTargetY(), camera.getTargetZ()));
        for (int i = 0; i < players.length; i++) {
            Player p = players[i];
            out.append("player ").append(i).append(' ').append(p == local ? "local " : "").append(
                    p.getPlayerInfo().getName()).append(" team ").append(p.getPlayerInfo().getTeam()).append(
                            " units ").append(p.getUnitCountContainer().getNumSupplies()).append('/').append(
                                    p.getUnitCountContainer().getMaxSupplyCount()).append(" buildings ").append(
                                            p.getBuildingCountContainer().getNumSupplies()).append('/').append(
                                                    p.getBuildingCountContainer().getMaxSupplyCount()).append('\n');
        }
        var selection = viewer.getSelection().getCurrentSelection();
        for (int i = 0; i < players.length; i++) {
            for (Selectable<?> s : players[i].getUnits().getSet()) {
                if (s.isDead()) {
                    continue;
                }
                camera.getProjectionModelView().project(s.getPositionX(), s.getPositionY(), s.getPositionZ(),
                        viewport, screen);
                String kind = s instanceof Building ? "building" : s instanceof Unit ? "unit" : "object";
                int hp = s instanceof Unit u ? u.getHitPoints() : s instanceof Building b ? b.getHitPoints() : -1;
                out.append(kind).append(' ').append(i).append(' ').append(s.getTemplate().getName()).append(
                        String.format(Locale.ROOT, " world %.1f %.1f %.1f screen %d %d hp %d", s.getPositionX(),
                                s.getPositionY(), s.getPositionZ(), Math.round(screen.x),
                                Math.round(framebuffer_height - screen.y), hp)).append(
                                        selection.contains(s) ? " selected" : "").append('\n');
            }
        }
        return out.toString();
    }

    /**
     * Legal placement cells for {@code building_index} within {@code radius} grid cells of the current selection (or
     * the camera target when nothing is selected), as {@code site gx gy world x y screen px py}. Clicking any reported
     * screen point in placing mode places the building there.
     */
    public static @NonNull String sites(@NonNull GUIRoot root, int building_index, int radius,
            int framebuffer_width, int framebuffer_height) {
        if (!(root.getDelegate() instanceof InGameDelegate delegate)) {
            return "err not in game (delegate " + root.getDelegate().getClass().getSimpleName() + ")\n";
        }
        WorldViewer viewer = delegate.getViewer();
        Player local = viewer.getLocalPlayer();
        BuildingTemplate template = local.getRace().getBuildingTemplate(building_index);
        UnitGrid unit_grid = viewer.getWorld().getUnitGrid();
        HeightMap height_map = viewer.getWorld().getHeightMap();
        CameraState camera = viewer.getCamera().getState();
        float center_x = camera.getTargetX();
        float center_y = camera.getTargetY();
        var selection = viewer.getSelection().getCurrentSelection().getSet();
        if (!selection.isEmpty()) {
            center_x = 0;
            center_y = 0;
            for (Selectable<?> s : selection) {
                center_x += s.getPositionX();
                center_y += s.getPositionY();
            }
            center_x /= selection.size();
            center_y /= selection.size();
        }
        int cx = UnitGrid.toGridCoordinate(center_x);
        int cy = UnitGrid.toGridCoordinate(center_y);
        int size = template.getPlacingSize();
        int min = size;
        int max = unit_grid.getGridSize() - size - 1;
        int[] viewport = {0, 0, framebuffer_width, framebuffer_height};
        Vector3f screen = new Vector3f();
        StringBuilder out = new StringBuilder();
        int legal = 0;
        for (int gy = Math.max(min, cy - radius); gy <= Math.min(max, cy + radius); gy++) {
            for (int gx = Math.max(min, cx - radius); gx <= Math.min(max, cx + radius); gx++) {
                if (!template.isPlacingLegal(local, gx, gy)) {
                    continue;
                }
                legal++;
                float x = UnitGrid.coordinateFromGrid(gx);
                float y = UnitGrid.coordinateFromGrid(gy);
                camera.getProjectionModelView().project(x, y, height_map.getNearestHeight(x, y), viewport, screen);
                out.append(String.format(Locale.ROOT, "site %d %d world %.1f %.1f screen %d %d%n", gx, gy, x, y,
                        Math.round(screen.x), Math.round(framebuffer_height - screen.y)));
            }
        }
        return "ok\n" + template.getName() + " size " + size + " center " + cx + " " + cy + " radius " + radius + " legal " + legal + "\n" + out;
    }
}
