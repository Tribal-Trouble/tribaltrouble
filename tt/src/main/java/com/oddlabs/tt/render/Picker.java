package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.MapCamera;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Army;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.ModelToolTip;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.util.DebugRender;
import com.oddlabs.tt.model.Target;
import com.oddlabs.tt.gui.ToolTip;
import com.oddlabs.tt.viewer.Selection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Handles world element picking and selection by unprojecting screen coordinates
 * and performing ray-casting against bounding boxes.
 */
public final class Picker implements Updatable<TimerAnimation> {
    private static final int PICK_SIZE = 5;
    private static final int SELECTION_THRESHOLD = 5;
    private static final float PATCH_PICK_PRECISION = .1f;
    private static final float PATCH_PICK_STEP = 2f;
    private static final float TOOL_TIP_DELAY = .1f;

    public record LandscapeLocation(float x, float y) implements Target {

        @Override
        public int getGridX() {
            return UnitGrid.toGridCoordinate(x);
        }

        @Override
        public int getGridY() {
            return UnitGrid.toGridCoordinate(y);
        }

        @Override
        public float getPositionX() {
            return x;
        }

        @Override
        public float getPositionY() {
            return y;
        }

        @Override
        public float getSize() {
            return 0;
        }

        @Override
        public boolean isDead() {
            return false;
        }
    }

    private final Matrix4f proj = new Matrix4f();

    private final Vector3f hit_result = new Vector3f();
    private final Vector3f dir_vector = new Vector3f();

    private final List<@Nullable Target> element_pick_list = new ArrayList<>();
    private final List<@NonNull TreeSupply> tree_pick_list = new ArrayList<>();

    private final CameraState tmp_camera = new CameraState();
    private final SortedSet<@NonNull LandscapeLeaf> patch_pick_set = new TreeSet<>(new LandscapeLeafComparator());
    private final SpriteSorter sprite_sorter = new SpriteSorter();
    private final TimerAnimation tool_tip_timer = new TimerAnimation(this, TOOL_TIP_DELAY);
    private final @NonNull LandscapeRenderer landscape_renderer;
    private final @NonNull ElementRenderer<?> element_renderer;
    private final @NonNull TreePicker tree_renderer;
    private final @NonNull RenderQueues render_queues;
    private final @NonNull RespondManager respond_manager;
    private final @NonNull Player local_player;
    private final @NonNull GUIRoot gui_root;

    private @Nullable Target current_hovered;
    private @Nullable ToolTip current_tooltip;
    private boolean render_tool_tip = false;

    private float patch_hit_x;
    private float patch_hit_y;
    private float patch_hit_z;

    private Selectable<?> @NonNull [] old_target_selection = Selectable.newArray(0);
    private @Nullable Action old_target_action;
    private boolean old_target_aggressive;

    private int old_landscape_target_grid_x;
    private int old_landscape_target_grid_y;

    private @Nullable Target old_set_target_target;

    public Picker(@NonNull AnimationManager manager, @NonNull Player local_player, @NonNull GUIRoot gui_root,
            @NonNull RenderQueues render_queues, @NonNull LandscapeRenderer landscape_renderer, Selection selection) {
        this.local_player = local_player;
        this.gui_root = gui_root;
        this.render_queues = render_queues;
        this.respond_manager = new RespondManager(manager);
        this.element_renderer = new ElementRenderer<>(local_player, render_queues, this, true, sprite_sorter,
                selection);
        this.tree_renderer = new TreePicker(sprite_sorter, respond_manager);
        this.landscape_renderer = landscape_renderer;
    }

    public @NonNull RespondManager getRespondManager() {
        return respond_manager;
    }

    private <T extends Target> @Nullable T getNearestPick(@NonNull List<? extends T> pick_list, @NonNull Class<
            ?> filter) {
        T nearest_pickable = null;
        float nearest_squared_distance = Float.POSITIVE_INFINITY;
        for (int i = 0; i < pick_list.size(); i++) {
            T pickable = pick_list.get(i);
            pick_list.set(i, null);
            float squared_distance = RenderTools.getCameraDistanceSquared(((BoundingBox) pickable), tmp_camera
                    .getCurrentX(), tmp_camera.getCurrentY(), tmp_camera.getCurrentZ());
            if (filter.isInstance(pickable) && squared_distance < nearest_squared_distance) {
                nearest_squared_distance = squared_distance;
                nearest_pickable = pickable;
            }
        }
        return nearest_pickable;
    }

    public float getScale() {
        return gui_root.getGlobalScale();
    }

    public void pickTarget(@NonNull Army selected_army, @NonNull CameraState camera,
            @NonNull PlayerInterface player_interface, int x, int y, @NonNull Action action) {
        int[] viewport = new int[4];
        float scale = getScale();
        setupPicking(camera, x * scale, y * scale, PICK_SIZE, PICK_SIZE, viewport);
        pickObjects();
        Target nearest_pickable = getNearestPick(element_pick_list, Target.class);
        Selectable<?>[] selection = selected_army.filter(Abilities.TARGET);
        if (nearest_pickable != null) {
            if (!(nearest_pickable instanceof SceneryModel sceneryModel) || sceneryModel.isOccupying())
                respond_manager.addResponder(nearest_pickable);
            if (isNewSetTarget(selection, nearest_pickable, action, Renderer.getRenderer()
                    .getSettings().aggressive_units))
                player_interface.setTarget(selection, nearest_pickable, action, Renderer.getRenderer()
                        .getSettings().aggressive_units);
        } else {
            pickResources();
            final TreeSupply supply = getNearestPick(tree_pick_list, Target.class);
            if (supply != null) {
                //	Target target = (Target)supply;
                respond_manager.addResponder(supply, () -> supply.changeRespondingTrees(-1));
                supply.changeRespondingTrees(1);
                if (isNewSetTarget(selection, supply, action, Renderer.getRenderer().getSettings().aggressive_units))
                    player_interface.setTarget(selection, supply, action, Renderer.getRenderer()
                            .getSettings().aggressive_units);
            } else if (nearestLandscape(Math.round(x * scale), Math.round(y * scale), viewport)) {
                new LandscapeTargetRespond(local_player.getWorld(), patch_hit_x, patch_hit_y);
                int grid_x = UnitGrid.toGridCoordinate(patch_hit_x);
                int grid_y = UnitGrid.toGridCoordinate(patch_hit_y);
                if (isNewLandscapeTarget(selection, grid_x, grid_y, action, Renderer.getRenderer()
                        .getSettings().aggressive_units))
                    player_interface.setLandscapeTarget(selection, grid_x, grid_y, action, Renderer.getRenderer()
                            .getSettings().aggressive_units);
            }
        }
    }

    private boolean isNewSetTarget(Selectable<?> @NonNull [] selection, @NonNull Target target, @NonNull Action action,
            boolean aggressive) {
        old_landscape_target_grid_x = -1;
        old_landscape_target_grid_y = -1;

        boolean new_target = isNewOrder(selection, action, aggressive);

        new_target |= target != old_set_target_target;

        old_set_target_target = target;
        return new_target;
    }

    private boolean isNewLandscapeTarget(Selectable<?> @NonNull [] selection, int grid_x, int grid_y,
            @NonNull Action action, boolean aggressive) {
        old_set_target_target = null;

        boolean new_target = isNewOrder(selection, action, aggressive);

        new_target |= grid_x != old_landscape_target_grid_x;
        new_target |= grid_y != old_landscape_target_grid_y;

        old_landscape_target_grid_x = grid_x;
        old_landscape_target_grid_y = grid_y;
        return new_target;
    }

    private boolean isNewOrder(Selectable<?> @NonNull [] selection, @NonNull Action action, boolean aggressive) {
        boolean new_order = false;
        if (selection.length == old_target_selection.length) {
            for (int i = 0; i < selection.length; i++) {
                new_order |= selection[i] != old_target_selection[i];
            }
        } else {
            new_order = true;
        }

        new_order |= action != old_target_action;
        new_order |= aggressive != old_target_aggressive;

        old_target_selection = selection;
        old_target_action = action;
        old_target_aggressive = aggressive;

        return new_order;
    }

    public @NonNull Selectable<?> @NonNull [] pickBoxed(@NonNull CameraState camera, int x1, int y1, int x2, int y2,
            int clicks) {
        int[] viewport = new int[4];
        float scale = gui_root.getGlobalScale();
        float sx1 = x1 * scale;
        float sy1 = y1 * scale;
        float sx2 = x2 * scale;
        float sy2 = y2 * scale;

        float cx = (sx1 + sx2) * 0.5f;
        float cy = (sy1 + sy2) * 0.5f;
        int width = (int) Math.abs(sx1 - sx2) + 1;
        int height = (int) Math.abs(sy1 - sy2) + 1;
        width = Math.max(width, PICK_SIZE);
        height = Math.max(height, PICK_SIZE);
        setupPicking(camera, cx, cy, width, height, viewport);
        pickObjects();
        return Math.abs(x1 - x2) < SELECTION_THRESHOLD && Math.abs(y1 - y2) < SELECTION_THRESHOLD
                ? createSinglePick(camera, clicks)
                : createBoxedPick();
    }

    private @NonNull Selectable<?> @NonNull [] createSinglePick(@NonNull CameraState camera, int clicks) {
        var nearest = (Selectable<?>) getNearestPick(element_pick_list, Selectable.class);
        if (nearest != null) {
            if (clicks > 1) {
                if (nearest.getAbilities().hasAbilities(Abilities.THROW)) {
                    return pickAll(camera, Abilities.THROW);
                } else if (nearest.getAbilities().hasAbilities(Abilities.HARVEST)) {
                    return pickAll(camera, Abilities.HARVEST);
                } else {
                    return Selectable.newArray(nearest);
                }
            } else {
                return Selectable.newArray(nearest);
            }
        } else {
            return Selectable.newArray(0);
        }
    }

    private @NonNull Selectable<?> @NonNull [] createBoxedPick() {
        var array = element_pick_list.stream()
                .filter(element -> element instanceof Selectable)
                .toArray(Selectable::newArray);
        element_pick_list.clear();
        return array;
    }

    private @NonNull Selectable<?> @NonNull [] pickAll(@NonNull CameraState camera, int ability_filter) {
        Selectable<?>[] complete_list = pickBoxed(camera, 0, 0, gui_root.getWidth() - 1, gui_root.getHeight() - 1, 2);
        return Arrays.stream(complete_list)
                .filter(s -> s.getAbilities().hasAbilities(ability_filter))
                .toArray(Selectable::newArray);
    }

    public void pickRotate(@NonNull GameCamera camera) {
        int[] viewport = new int[4];
        int x = gui_root.getWidth() / 2;
        int y = camera.getRotateY();
        float scale = getScale();
        setupPicking(camera.getState(), x * scale, y * scale, PICK_SIZE, PICK_SIZE, viewport);
        if (!nearestLandscape(Math.round(x * scale), Math.round(y * scale), viewport) || patch_hit_z < local_player
                .getWorld().getHeightMap().getSeaLevelMeters()) {
            float dz = tmp_camera.getCurrentZ() - local_player.getWorld().getHeightMap().getSeaLevelMeters();
            float factor = dz / dir_vector.z();
            patch_hit_x = tmp_camera.getCurrentX() - factor * dir_vector.x();
            patch_hit_y = tmp_camera.getCurrentY() - factor * dir_vector.y();
            patch_hit_z = local_player.getWorld().getHeightMap().getSeaLevelMeters();
        }
        int grid_x = UnitGrid.toGridCoordinate(patch_hit_x);
        int grid_y = UnitGrid.toGridCoordinate(patch_hit_y);
        camera.setRotationPoint(new LandscapeTarget(grid_x, grid_y));
    }

    private void calcPosAndDir(int pixel_x, int pixel_y, int[] viewport) {
        Vector3f hit2 = new Vector3f();

        tmp_camera.getProjectionModelView().unproject(pixel_x, pixel_y, 0.0f, viewport, hit_result);
        tmp_camera.getProjectionModelView().unproject(pixel_x, pixel_y, 1.0f, viewport, hit2);

        hit2.sub(hit_result, dir_vector).normalize();
    }

    private boolean nearestLandscape(int pixel_x, int pixel_y, int[] viewport) {
        pickLandscape();
        calcPosAndDir(pixel_x, pixel_y, viewport);
        return doNearestLandscape(hit_result.x(), hit_result.y(), hit_result.z(), dir_vector.x(), dir_vector.y(),
                dir_vector.z());
    }

    /**
     * Unprojects a 2D screen coordinate into a 3D world coordinate.
     *
     * @param winx The window x-coordinate.
     * @param winy The window y-coordinate.
     * @param winz The window z-coordinate (depth).
     * @param proj The combined projection-model-view matrix from the camera.
     * @param viewport The viewport buffer.
     */
    private void unproject(float winx, float winy, float winz, @NonNull Matrix4f proj, int[] viewport) {
        proj.unproject(winx, winy, winz, viewport, hit_result);
    }

    private static float computeTMax(float bmin, float bmax, float c, float d) {
        if (d == 0) {
            return Float.POSITIVE_INFINITY;
        }
        float t1 = (bmin - c) / d;
        float t2 = (bmax - c) / d;
        return Math.max(t1, t2);
    }

    private static float computeTMin(float bmin, float bmax, float c, float d) {
        if (d == 0) {
            return Float.NEGATIVE_INFINITY;
        }
        float t1 = (bmin - c) / d;
        float t2 = (bmax - c) / d;
        return Math.min(t1, t2);
    }

    private boolean doNearestLandscape(float x, float y, float z, float dx, float dy, float dz) {
        if (getHeight(x, y) > z) {
            return false;
        }
        while (!patch_pick_set.isEmpty()) {
            BoundingBox bb = patch_pick_set.getFirst();
            assert patch_pick_set.contains(bb);
            patch_pick_set.remove(bb);
            float tx_min = computeTMin(bb.bmin_x, bb.bmax_x, x, dx);
            float ty_min = computeTMin(bb.bmin_y, bb.bmax_y, y, dy);
            float tz_min = computeTMin(bb.bmin_z, bb.bmax_z, z, dz);
            float tx_max = computeTMax(bb.bmin_x, bb.bmax_x, x, dx);
            float ty_max = computeTMax(bb.bmin_y, bb.bmax_y, y, dy);
            float tz_max = computeTMax(bb.bmin_z, bb.bmax_z, z, dz);

            float t_min = Math.max(tx_min, Math.max(ty_min, tz_min));
            float t_max = Math.min(tx_max, Math.min(ty_max, tz_max));
            if (t_min < 0)
                t_min = 0;
            // If t_min is greater than t_max, the pick ray does not intersect the BB, therefore we skip it
            if (t_min >= t_max) {
                continue;
            }
            float t_min_x = x + t_min * dx;
            float t_min_y = y + t_min * dy;
            float t_min_z = z + t_min * dz;
            float t_min_height = getHeight(t_min_x, t_min_y);
            if (t_min_height >= 0.001f + t_min_z) {
                // Ray enters the bounding box already below the terrain surface.
                // This is a valid degenerate case (e.g. steep camera angles in map overview mode).
                continue;
            }
            boolean found_t_range = false;
            for (float t_scan = t_min; t_scan <= t_max; t_scan += PATCH_PICK_STEP) {
                float t_scan_next = Math.min(t_scan + PATCH_PICK_STEP, t_max);
                float t_scan_x = x + t_scan_next * dx;
                float t_scan_y = y + t_scan_next * dy;
                float t_scan_z = z + t_scan_next * dz;
                float t_scan_height = getHeight(t_scan_x, t_scan_y);
                if (t_scan_height >= t_scan_z - 0.001f) {
                    t_min = t_scan;
                    t_max = t_scan_next;
                    found_t_range = true;
                    break;
                }
            }
            if (!found_t_range) {
                continue;
            }
            float t_mid_x;
            float t_mid_y;
            float t_mid_z;
            float t_mid_height;
            float height_diff;
            float old_t_mid;
            float t_mid = Float.NaN;
            do {
                old_t_mid = t_mid;
                t_mid = (t_max + t_min) * .5f;
                t_mid_x = x + t_mid * dx;
                t_mid_y = y + t_mid * dy;
                t_mid_z = z + t_mid * dz;
                t_mid_height = getHeight(t_mid_x, t_mid_y);
                height_diff = t_mid_height - t_mid_z;
                if (height_diff >= 0)
                    t_max = t_mid;
                else
                    t_min = t_mid;
            } while (Math.abs(height_diff) > PATCH_PICK_PRECISION && t_mid != old_t_mid);
            patch_hit_x = t_mid_x;
            patch_hit_y = t_mid_y;
            patch_hit_z = t_mid_height;
            return true;
        }
        return false;
    }

    private float getHeight(float x, float y) {
        return local_player.getWorld().getHeightMap().getNearestHeight(x, y);
    }

    public void pickMapGoto(int x, int y, @NonNull MapCamera camera) {
        int[] viewport = new int[4];
        float scale = getScale();
        setupPicking(camera.getState(), x * scale, y * scale, PICK_SIZE, PICK_SIZE, viewport);
        if (nearestLandscape(Math.round(x * scale), Math.round(y * scale), viewport))
            camera.mapGoto(patch_hit_x, patch_hit_y);
    }

    public @NonNull Optional<Target> pickRallyPoint(@NonNull CameraState camera, int x, int y,
            @NonNull Building building) {
        int[] viewport = new int[4];
        float scale = getScale();
        setupPicking(camera, x * scale, y * scale, PICK_SIZE, PICK_SIZE, viewport);
        pickObjects();
        Target nearest = getNearestPick(element_pick_list, Target.class);
        if (nearest instanceof Building) {
            return Optional.of(nearest);
        } else if (nearestLandscape(Math.round(x * scale), Math.round(y * scale), viewport)) {
            int grid_x = UnitGrid.toGridCoordinate(patch_hit_x);
            int grid_y = UnitGrid.toGridCoordinate(patch_hit_y);
            return Optional.of(building.getUnitGrid().findGridTargets(grid_x, grid_y, 1, false)[0]);
        } else {
            return Optional.empty();
        }
    }

    public void pickHoverPhysical(@NonNull CameraState camera, int physical_x, int physical_y) {
        int[] viewport = new int[4];
        setupPicking(camera, physical_x, physical_y, PICK_SIZE, PICK_SIZE, viewport);
        pickObjects();
        Target nearest = getNearestPick(element_pick_list, Target.class);
        Target new_current_hovered;
        if (nearest != null) {
            new_current_hovered = nearest;
        } else {
            pickResources();
            new_current_hovered = getNearestPick(tree_pick_list, Target.class);
        }
        if (current_hovered != new_current_hovered) {
            tool_tip_timer.resetTime();
            boolean old_tip = current_hovered instanceof ModelToolTip;
            boolean new_tip = new_current_hovered instanceof ModelToolTip;
            if (!old_tip && new_tip) {
                tool_tip_timer.start();
                render_tool_tip = false;
            }
            if (old_tip && !new_tip) {
                if (!render_tool_tip)
                    tool_tip_timer.stop();
                else
                    render_tool_tip = false;
            }
            current_hovered = new_current_hovered;
            current_tooltip = new_tip ? new ToolTipAdapter((ModelToolTip) current_hovered, local_player) : null;
        }
    }

    public void pickHover(@NonNull CameraState camera, int x, int y) {
        int[] viewport = new int[4];
        float scale = gui_root.getGlobalScale();
        setupPicking(camera, x * scale, y * scale, PICK_SIZE, PICK_SIZE, viewport);
        pickObjects();
        Target nearest = getNearestPick(element_pick_list, Target.class);
        Target new_current_hovered;
        if (nearest != null) {
            new_current_hovered = nearest;
        } else {
            pickResources();
            new_current_hovered = getNearestPick(tree_pick_list, Target.class);
        }
        if (current_hovered != new_current_hovered) {
            tool_tip_timer.resetTime();
            boolean old_tip = current_hovered instanceof ModelToolTip;
            boolean new_tip = new_current_hovered instanceof ModelToolTip;
            if (!old_tip && new_tip) {
                tool_tip_timer.start();
                render_tool_tip = false;
            }
            if (old_tip && !new_tip) {
                if (!render_tool_tip)
                    tool_tip_timer.stop();
                else
                    render_tool_tip = false;
            }
            current_hovered = new_current_hovered;
            current_tooltip = new_tip ? new ToolTipAdapter((ModelToolTip) current_hovered, local_player) : null;
        }
    }

    @Override
    public void update(@NonNull TimerAnimation anim) {
        render_tool_tip = true;
        tool_tip_timer.stop();
    }

    public @Nullable ToolTip getCurrentToolTip() {
        return canRenderToolTip() ? current_tooltip : null;
    }

    public @Nullable Target getCurrentHovered() {
        return current_hovered;
    }

    public boolean canRenderToolTip() {
        return render_tool_tip;
    }

    public void resetCurrentHovered() {
        current_hovered = null;
        current_tooltip = null;
    }

    public Optional<LandscapeLocation> pickLocation(@NonNull CameraState camera) {
        int[] viewport = new int[4];
        int x = Renderer.getLocalInput().getMouseX();
        int y = Renderer.getLocalInput().getMouseY();
        setupPicking(camera, x, y, PICK_SIZE, PICK_SIZE, viewport);

        return !nearestLandscape(x, y, viewport) ? Optional.empty() : Optional.of(new LandscapeLocation(patch_hit_x,
                patch_hit_y));
    }

    private void setupPicking(@NonNull CameraState camera, float x_center, float y_center, int width, int height,
            int @NonNull [] viewport) {
        proj.identity();
        var context = Renderer.getRenderer().getRenderContext();
        viewport[0] = 0;
        viewport[1] = 0;
        viewport[2] = context.getViewportWidth();
        viewport[3] = context.getViewportHeight();

        if (width > 0 && height > 0) {
            Vector3f temp_vector = new Vector3f((viewport[2] - 2 * (x_center - viewport[0])) / width, (viewport[3] - 2
                    * (y_center - viewport[1])) / height, 0);
            proj.translate(temp_vector.x, temp_vector.y, temp_vector.z);
            temp_vector.set((float) viewport[2] / width, (float) viewport[3] / height, 1.0f);
            proj.scale(temp_vector.x, temp_vector.y, temp_vector.z);
        }

        gui_root.multProjection(proj);

        tmp_camera.set(camera);
        tmp_camera.setView(proj, width, height);
    }

    private void pickLandscape() {
        patch_pick_set.clear();
        landscape_renderer.pick(tmp_camera, false, patch_pick_set);
    }

    private void pickObjects() {
        element_pick_list.clear();
        element_renderer.setup(tmp_camera);
        element_renderer.visit(local_player.getWorld().getElementRoot());
        sprite_sorter.distributeModels();
        render_queues.getAllPicks(element_pick_list::add);
    }

    private void pickResources() {
        tree_pick_list.clear();
        tree_renderer.setup(tmp_camera);
        tree_renderer.visit(local_player.getWorld().getTreeRoot());
        sprite_sorter.distributeModels();
        tree_renderer.getAllPicks(tree_pick_list);
    }

    public void debugRender() {
        DebugRender.drawPoint(patch_hit_x, patch_hit_y, patch_hit_z, 10f, 1f, 1f, 1f);
    }

    private final class LandscapeLeafComparator implements Comparator<LandscapeLeaf> {
        private int compare(@NonNull CameraState camera_state, @NonNull LandscapeLeaf l1, @NonNull LandscapeLeaf l2) {
            float l1_dist = RenderTools.getCameraDistanceXYSquared(l1, camera_state.getCurrentX(), camera_state
                    .getCurrentY());
            float l2_dist = RenderTools.getCameraDistanceXYSquared(l2, camera_state.getCurrentX(), camera_state
                    .getCurrentY());
            if (l1_dist < l2_dist)
                return -1;
            else if (l1_dist > l2_dist)
                return 1;
            else if (l1.bmin_x < l2.bmin_x)
                return -1;
            else if (l1.bmin_x > l2.bmin_x)
                return 1;
            else if (l1.bmin_y < l2.bmin_y)
                return -1;
            else if (l1.bmin_y > l2.bmin_y)
                return 1;
            else {
                assert l1 == l2;
                return 0;
            }
        }

        @Override
        public int compare(@NonNull LandscapeLeaf l1, @NonNull LandscapeLeaf l2) {
            return compare(Picker.this.tmp_camera, l1, l2);
        }
    }
}
