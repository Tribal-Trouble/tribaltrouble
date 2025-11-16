package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import com.oddlabs.tt.landscape.LandscapeTileIndices;
import com.oddlabs.tt.landscape.PatchGroup;
import com.oddlabs.tt.landscape.PatchGroupVisitor;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.vbo.ShortVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class LandscapeRenderer implements Animated {

    private final List<PatchLevel> @NonNull [] patch_lists;
    private final List<LandscapeLeaf> render_list = new ArrayList<>();
    private final GUIRoot gui_root;
    private final @NonNull World world;
    private final Texture[][] colormaps;
    private final Texture detail;
    private final PatchLevel[] @NonNull [] patch_levels;
    private final @NonNull LandscapeTileVertices landscape_vertices;
    private final ShortBuffer shadow_indices_buffer;
    private final @NonNull ShortVBO indices_vbo;
    private final @NonNull AnimationManager manager;

    private int current_map_x;
    private int current_map_y;

    private boolean editing;
    private int edit_patch_x0;
    private int edit_patch_y0;
    private int edit_patch_x1;
    private int edit_patch_y1;

    @SuppressWarnings("unchecked")
    public LandscapeRenderer(@NonNull World world, @NonNull WorldInfo world_info, GUIRoot gui_root, @NonNull AnimationManager manager) {
        ShortBuffer indices = world.getLandscapeIndices().getIndices();
        this.indices_vbo = new ShortVBO(GL15.GL_STATIC_DRAW, indices.remaining());
        this.indices_vbo.put(indices);

        this.landscape_vertices = new LandscapeTileVertices(world.getHeightMap(), HeightMap.GRID_UNITS_PER_PATCH_EXP, world.getHeightMap().getPatchesPerWorld());
        this.patch_levels = new PatchLevel[world.getHeightMap().getPatchesPerWorld()][world.getHeightMap().getPatchesPerWorld()];
        for (PatchLevel[] patch_level : patch_levels) {
            for (int x = 0; x < patch_levels.length; x++) {
                patch_level[x] = new PatchLevel();
            }
        }
        for (int y = 0; y < patch_levels.length; y++) {
            for (int x = 0; x < patch_levels.length; x++) {
                PatchLevel right = getPatchWrapped(x + 1, y);
                PatchLevel top = getPatchWrapped(x, y + 1);
                patch_levels[y][x].init(right, top);
            }
        }
        this.detail = world_info.detail;
        this.colormaps = world_info.colormaps;
        this.gui_root = gui_root;
        this.world = world;
        this.manager = manager;
        int levels = LandscapeTileIndices.getNumLOD(HeightMap.GRID_UNITS_PER_PATCH_EXP);
        patch_lists = (List<PatchLevel>[]) new ArrayList<?>[levels];
        for (int i = 0; i < patch_lists.length; i++) {
            patch_lists[i] = new ArrayList<>();
        }
        manager.registerAnimation(this);
        this.shadow_indices_buffer = BufferUtils.createShortBuffer(LandscapeTileIndices.getNumTriangles(world.getLandscapeIndices().getNumLOD() - 1) * 3);
        resetEditing();
    }

    public @NonNull HeightMap getHeightMap() {
        return world.getHeightMap();
    }

    public PatchLevel getPatchLevelFromCoordinates(float x_f, float y_f) {
        int patch_x = world.getHeightMap().coordinateToPatch(x_f);
        int patch_y = world.getHeightMap().coordinateToPatch(y_f);
        return getPatchLevel(patch_x, patch_y);
    }

    private PatchLevel getPatchLevel(int patch_x, int patch_y) {
        return patch_levels[patch_y][patch_x];
    }

    public PatchLevel getPatchLevel(@NonNull LandscapeLeaf leaf) {
        return getPatchLevel(leaf.getPatchX(), leaf.getPatchY());
    }

    private PatchLevel getPatchWrapped(int patch_x, int patch_y) {
        patch_x = (patch_x + patch_levels.length) % patch_levels.length;
        patch_y = (patch_y + patch_levels.length) % patch_levels.length;
        return getPatchLevel(patch_x, patch_y);
    }

    public void bindDetail() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, detail.getHandle());
    }

    void bindMap(int x, int y) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colormaps[y][x].getHandle());
    }

    void bindLeaf(@NonNull LandscapeLeaf leaf) {
        landscape_vertices.bind(leaf.getPatchX(), leaf.getPatchY());
    }

    private void clearRenderList() {
        for (int i = 0; i < render_list.size(); i++) {
            LandscapeLeaf patch = render_list.get(i);
            render_list.set(i, null);
        }
        render_list.clear();
    }

    private void renderPatch(@NonNull LandscapeLeaf leaf) {
        bindLeaf(leaf);
        PatchLevel patch_level = getPatchLevel(leaf);
        int patch_index = world.getLandscapeIndices().getPatchIndex(patch_level.getLevel(), patch_level.getBorderSet());
        int triangle_index = world.getLandscapeIndices().getTriangleIndex(patch_index);
        int triangle_index2 = world.getLandscapeIndices().getTriangleIndex(patch_index + 1);
        int num_triangles = triangle_index2 - triangle_index;
        indices_vbo.drawElements(GL11.GL_TRIANGLES, num_triangles * 3, triangle_index * 3);
    }

    private void doRenderAll() {
        for (LandscapeLeaf patch : render_list) {
            RenderTools.draw(patch, BoundingMode.LANDSCAPE, 1f, 0f, 0f);
            setupColormap(patch.getColorMapX(), patch.getColorMapY());
            if (Globals.draw_landscape)
                renderPatch(patch);
        }
    }

    public void pick(@NonNull CameraState camera, boolean visible_override, @NonNull Set<LandscapeLeaf> set) {
        doPrepareAll(camera, visible_override, set);
    }

    public void prepareAll(@NonNull CameraState camera, boolean visible_override) {
        clearRenderList();
        doPrepareAll(camera, visible_override, render_list);
    }

    private static final Visitor patch_visitor = new Visitor();

    private void doPrepareAll(@NonNull CameraState camera, final boolean visible_override, @NonNull Collection<LandscapeLeaf> result) {
        endEdit();
        patch_visitor.setup(camera, visible_override, result);
        world.getPatchRoot().visit(patch_visitor);
    }

    public void endEdit() {
        if (!editing)
            return;

        for (int y = edit_patch_y0; y <= edit_patch_y1; y++) {
            for (int x = edit_patch_x0; x <= edit_patch_x1; x++) {
                reload(x, y);
            }
        }
        resetEditing();
    }

    private void resetEditing() {
        edit_patch_x0 = Integer.MAX_VALUE;
        edit_patch_y0 = Integer.MAX_VALUE;
        edit_patch_x1 = Integer.MIN_VALUE;
        edit_patch_y1 = Integer.MIN_VALUE;
        editing = false;
    }

    public void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1) {
        editing = true;
        edit_patch_x0 = Math.min(edit_patch_x0, patch_x0);
        edit_patch_y0 = Math.min(edit_patch_y0, patch_y0);
        edit_patch_x1 = Math.max(edit_patch_x1, patch_x1);
        edit_patch_y1 = Math.max(edit_patch_y1, patch_y1);
    }

    public void renderAll() {
        setupLandscape();
        doRenderAll();
        disableLandscape();
    }

    private int calculateLevel(@NonNull LandscapeLeaf leaf) {
        CameraState camera = gui_root.getDelegate().getCamera().getState();
        float dist2 = RenderTools.getEyeDistanceSquared(leaf, camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ());
        // Find appropriate patch size
        int i;
        float[] errors = leaf.getErrors();
        for (i = 0; i < errors.length; i++) {
            if (dist2 >= errors[i])
                break;
        }
        return i;
    }

    private final PatchGroupVisitor level_updater = new PatchGroupVisitor() {
        @Override
        public void visitGroup(@NonNull PatchGroup group) {
            group.visitChildren(this);
        }

        @Override
        public void visitLeaf(@NonNull LandscapeLeaf leaf) {
            int wanted_level = calculateLevel(leaf);
            PatchLevel patch_level = getPatchLevel(leaf);
            patch_lists[wanted_level].add(patch_level);
        }
    };

    @Override
    public void animate(float t) {
        world.getPatchRoot().visit(level_updater);
        for (int i = patch_lists.length - 1; i >= 0; i--) {
            List<PatchLevel> patches = patch_lists[i];
            for (int j = 0; j < patches.size(); j++) {
                PatchLevel patch_level = patches.get(j);
                patch_level.setLevel(i);
                patch_level.adjustLevel();
                patches.set(j, null);
            }
            patches.clear();
        }
    }

    @Override
    public void updateChecksum(@NonNull StateChecksum sum) {
    }

    private void setupColormap(int map_x, int map_y) {
        if (current_map_x != map_x || current_map_y != map_y) {
            bindMap(map_x, map_y);
            float tex_translate_x = map_x * world.getHeightMap().getMetersPerChunk() - world.getHeightMap().getMetersPerChunkBorder();
            float tex_translate_y = map_y * world.getHeightMap().getMetersPerChunk() - world.getHeightMap().getMetersPerChunkBorder();
            GLUtils.setupTexGen(world.getHeightMap().getChunkTexScale(), world.getHeightMap().getChunkTexScale(), -tex_translate_x, -tex_translate_y);

            current_map_x = map_x;
            current_map_y = map_y;
        }
    }

    private void setupLandscape() {
        current_map_x = -1;
        current_map_y = -1;
        GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
        GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
        GLStateStack.switchState(GLState.VERTEX_ARRAY | GLState.NORMAL_ARRAY | GLState.TEXCOORD0_ARRAY);
        if (Globals.draw_detail) {
            GLState.activeTexture(GL13.GL_TEXTURE1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            bindDetail();
            GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
            GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
            GLUtils.setupTexGen(Globals.LANDSCAPE_DETAIL_REPEAT_RATE, Globals.LANDSCAPE_DETAIL_REPEAT_RATE, 0, 0);
            GLState.activeTexture(GL13.GL_TEXTURE0);
        }
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_BLEND);
    }

    private static void disableLandscape() {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (Globals.draw_detail) {
            GLState.activeTexture(GL13.GL_TEXTURE1);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GLState.activeTexture(GL13.GL_TEXTURE0);
        }
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
    }

    public void reload(int patch_x, int patch_y) {
        landscape_vertices.reload(patch_x, patch_y);
    }

    void renderShadow(int patch_x, int patch_y, int start_x, int start_y, int end_x, int end_y) {
        landscape_vertices.bind(patch_x, patch_y);
        PatchLevel patch_level = getPatchLevel(patch_x, patch_y);
        shadow_indices_buffer.clear();
        world.getLandscapeIndices().fillCoverIndices(shadow_indices_buffer, patch_level.getLevel(), patch_level.getBorderSet(), start_x, start_y, end_x, end_y);
        shadow_indices_buffer.flip();
        GL11.glDrawElements(GL11.GL_TRIANGLES, shadow_indices_buffer);
    }

    private static final class Visitor implements PatchGroupVisitor {

        private CameraState camera;
        private boolean visible_override;
        private Collection<LandscapeLeaf> result;

        private void setup(@NonNull CameraState camera, boolean visible_override, @NonNull Collection<LandscapeLeaf> result) {
            this.camera = camera;
            this.visible_override = visible_override;
            this.result = result;
        }

        @Override
        public void visitGroup(@NonNull PatchGroup group) {
            RenderTools.FrustumIntersection frustum_state = RenderTools.FrustumIntersection.ALL_OUTSIDE;
            if (visible_override || (frustum_state = RenderTools.inFrustum(group, camera.getFrustum())) != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
                boolean old_override = visible_override;
                visible_override = visible_override || frustum_state == RenderTools.FrustumIntersection.ALL_INSIDE;
                group.visitChildren(this);
                visible_override = old_override;
            }
        }

        @Override
        public void visitLeaf(@NonNull LandscapeLeaf leaf) {
            if (visible_override || RenderTools.inFrustum(leaf, camera.getFrustum()) != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
                result.add(leaf);
            }
        }
    }
}
