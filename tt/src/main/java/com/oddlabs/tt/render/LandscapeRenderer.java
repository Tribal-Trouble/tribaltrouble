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
import com.oddlabs.tt.render.shader.LandscapeShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.resource.BlendInfo;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.resource.StructureBlend;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.GLStateHelper;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.vbo.ShortVBO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LandscapeRenderer implements Animated {

    private final List<PatchLevel> @NonNull [] patch_lists;
    private final List<LandscapeLeaf> render_list = new ArrayList<>();
    private final GUIRoot gui_root;
    private final @NonNull World world;
    private final @NonNull Texture highlightMap;
    private final @NonNull Texture shadowMap;
    private final BlendInfo @NonNull [] blendInfos;
    private final @NonNull PatchLevel @NonNull [] @NonNull [] patch_levels;
    private final @NonNull LandscapeTileVertices landscape_vertices;
    private final ShortBuffer shadow_indices_buffer;
    private final @NonNull ShortVBO indices_vbo;
    private final LandscapeShader shader = new LandscapeShader();
    private final @NonNull Texture packedAlpha0;
    private final @NonNull Texture packedAlpha1;

    public @NonNull LandscapeShader getShader() {
        return shader;
    }

    private int current_map_x;
    private int current_map_y;

    private boolean editing;
    private int edit_patch_x0;
    private int edit_patch_y0;
    private int edit_patch_x1;
    private int edit_patch_y1;

    static class PatchFinder {
        private final @Nullable PatchLevel @NonNull [] @NonNull [] patch_levels;
        private final @NonNull ExecutorService executor;
        public PatchFinder(@NonNull ExecutorService executor, @Nullable PatchLevel @NonNull [] @NonNull [] patch_levels) {
            this.executor = executor;
            this.patch_levels = patch_levels;
        }

        public void set(int x, int y, @NonNull PatchLevel value) {
            assert null == patch_levels[x][y];
            patch_levels[x][y] = value;
        }

        public @NonNull PatchLevel get(int x, int y) throws ExecutionException, InterruptedException {
            var size = patch_levels.length;
            int wrappedX = (x + size) % size;
            int wrappedY = (y + size) % size;
            var value = patch_levels[wrappedX][wrappedY];
            if (null == value) {
                value = executor.submit(() -> new PatchLevel(this, wrappedX, wrappedY)).get();
            }
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    public LandscapeRenderer(@NonNull World world, @NonNull WorldInfo world_info, GUIRoot gui_root, @NonNull AnimationManager manager) {
        ShortBuffer indices = world.getLandscapeIndices().getIndices();
        this.indices_vbo = new ShortVBO(GL15.GL_STATIC_DRAW, indices.remaining());
        this.indices_vbo.put(indices);

        this.landscape_vertices = new LandscapeTileVertices(world.getHeightMap(), HeightMap.GRID_UNITS_PER_PATCH_EXP, world.getHeightMap().getPatchesPerWorld());
        this.patch_levels = new PatchLevel[world.getHeightMap().getPatchesPerWorld()][world.getHeightMap().getPatchesPerWorld()];
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var finder = new PatchFinder(executor, patch_levels);
            PatchLevel first = new PatchLevel(finder, 0, 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize patch levels", e);
        }
        this.blendInfos = world_info.blend_infos;
        
        // Initialize VAOs if supported
        landscape_vertices.init(shader);
        
        // Pack alpha maps into two RGBA textures
        GLImage ref = blendInfos[1].getSourceImage(); // Use Dirt alpha as reference for size
        int width = ref.getWidth();
        int height = ref.getHeight();
        GLIntImage img0 = new GLIntImage(width, height, GL11.GL_RGBA);
        GLIntImage img1 = new GLIntImage(width, height, GL11.GL_RGBA);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r0 = 255; // Base alpha is white (1x1 in source, fill here)
                int g0 = blendInfos[1].getSourceImage().getPixel(x, y);
                int b0 = blendInfos[2].getSourceImage().getPixel(x, y);
                int a0 = blendInfos[3].getSourceImage().getPixel(x, y);
                img0.putPixel(x, y, (a0 << 24) | (b0 << 16) | (g0 << 8) | r0);

                int r1 = blendInfos[4].getSourceImage().getPixel(x, y);
                int g1 = blendInfos[5].getSourceImage().getPixel(x, y); // Highlight
                int b1 = blendInfos[6].getSourceImage().getPixel(x, y); // Shadow
                int a1 = blendInfos[7].getSourceImage().getPixel(x, y); // Seabottom
                img1.putPixel(x, y, (a1 << 24) | (b1 << 16) | (g1 << 8) | r1);
            }
        }
        
        packedAlpha0 = new Texture(new GLImage[]{img0}, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
        packedAlpha1 = new Texture(new GLImage[]{img1}, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);

        this.highlightMap = world_info.blend_infos[5].getAlphaMap();
        this.shadowMap = world_info.blend_infos[6].getAlphaMap();

        this.gui_root = gui_root;
        this.world = world;
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

    public @NonNull PatchLevel getPatchLevelFromCoordinates(float x_f, float y_f) {
        int patch_x = world.getHeightMap().coordinateToPatch(x_f);
        int patch_y = world.getHeightMap().coordinateToPatch(y_f);
        return getPatchLevel(patch_x, patch_y);
    }

    private @NonNull PatchLevel getPatchLevel(int patch_x, int patch_y) {
        return patch_levels[patch_y][patch_x];
    }

    public @NonNull PatchLevel getPatchLevel(@NonNull LandscapeLeaf leaf) {
        return getPatchLevel(leaf.getPatchX(), leaf.getPatchY());
    }

    private PatchLevel getPatchWrapped(int patch_x, int patch_y) {
        var size = patch_levels.length;
        patch_x = (patch_x + size) % size;
        patch_y = (patch_y + size) % size;
        return getPatchLevel(patch_x, patch_y);
    }

    private void clearRenderList() {
        render_list.clear();
    }

    private void renderPatch(@NonNull LandscapeLeaf leaf) {
        landscape_vertices.bind(leaf.getPatchX(), leaf.getPatchY(), shader);
        PatchLevel patch_level = getPatchLevel(leaf);
        int patch_index = world.getLandscapeIndices().getPatchIndex(patch_level.getLevel(), patch_level.getBorderSet());
        int triangle_index = world.getLandscapeIndices().getTriangleIndex(patch_index);
        int triangle_index2 = world.getLandscapeIndices().getTriangleIndex(patch_index + 1);
        int num_triangles = triangle_index2 - triangle_index;
        indices_vbo.drawElements(GL11.GL_TRIANGLES, num_triangles * 3, triangle_index * 3);
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
        if (!editing) return;
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

    public void renderAll(@NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        try (var _ = shader.use();
             var _ = GLStateHelper.blend(false);
             var _ = GLStateHelper.depthTest(true);
             var _ = GLStateHelper.cullFace(false);
             var _ = state.getFog().setup(shader, state.getCurrentZ())) {

            shader.setUniformMatrix4(LandscapeShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniformMatrix4(LandscapeShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniform(LandscapeShader.Uniforms.LIGHT_DIR, -1f, 0f, 1f);
            shader.setUniform(LandscapeShader.Uniforms.GLOBAL_AMBIENT, 0.65f, 0.65f, 0.65f);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, packedAlpha0.getHandle());
            shader.setUniform("u_packedAlpha0", 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, packedAlpha1.getHandle());
            shader.setUniform("u_packedAlpha1", 1);

            int texUnit = 2;
            for (int i = 0; i < blendInfos.length; i++) {
                if (blendInfos[i] instanceof StructureBlend blend) {
                    GL13.glActiveTexture(GL13.GL_TEXTURE0 + texUnit);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, blend.getStructureMap().getHandle());
                    shader.setUniform("u_structure" + i, texUnit++);
                }
            }

            for (LandscapeLeaf patch : render_list) {
                if (Globals.isBoundsEnabled(BoundingMode.LANDSCAPE)) {
                    RenderTools.draw(patch, BoundingMode.LANDSCAPE, 1f, 0f, 0f);
                    shader.use();
                }
                if (Globals.draw_landscape) renderPatch(patch);
            }
            landscape_vertices.unbind(shader);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);

        } finally {
            com.oddlabs.tt.vbo.VBO.releaseIndexVBO();
        }
    }

    public void debugRender(@NonNull CameraState frustum_state) {
        if (Globals.isBoundsEnabled(BoundingMode.LANDSCAPE)) {
            for (LandscapeLeaf patch : render_list) {
                RenderTools.draw(patch, BoundingMode.LANDSCAPE, 1f, 0f, 0f);
            }
        }
    }

    private int calculateLevel(@NonNull LandscapeLeaf leaf) {
        CameraState camera = gui_root.getDelegate().getCamera().getState();
        float dist2 = RenderTools.getEyeDistanceSquared(leaf, camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ());
        int i;
        float[] errors = leaf.getErrors();
        for (i = 0; i < errors.length; i++) {
            if (dist2 >= errors[i]) break;
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

    public void reload(int patch_x, int patch_y) {
        landscape_vertices.reload(patch_x, patch_y);
    }

    void renderShadow(@NonNull ShaderProgram shader, int patch_x, int patch_y, int start_x, int start_y, int end_x, int end_y) {
        landscape_vertices.bindAttributes(shader, patch_x, patch_y);
        PatchLevel patch_level = getPatchLevel(patch_x, patch_y);
        shadow_indices_buffer.clear();
        world.getLandscapeIndices().fillCoverIndices(shadow_indices_buffer, patch_level.getLevel(), patch_level.getBorderSet(), start_x, start_y, end_x, end_y);
        shadow_indices_buffer.flip();
        GL11.glDrawElements(GL11.GL_TRIANGLES, shadow_indices_buffer);
        
        int posLoc = shader.getAttributeLocation("a_position");
        if (posLoc != -1) GL20.glDisableVertexAttribArray(posLoc);
        int normLoc = shader.getAttributeLocation("a_normal");
        if (normLoc != -1) GL20.glDisableVertexAttribArray(normLoc);
        int texLoc = shader.getAttributeLocation("a_texCoord0");
        if (texLoc != -1) GL20.glDisableVertexAttribArray(texLoc);
    }

    private static final class Visitor implements PatchGroupVisitor {
        private @NonNull CameraState camera;
        private boolean visible_override;
        private @NonNull Collection<LandscapeLeaf> result;

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
