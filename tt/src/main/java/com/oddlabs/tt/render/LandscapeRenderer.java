package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import com.oddlabs.tt.landscape.PatchGroup;
import com.oddlabs.tt.landscape.PatchGroupVisitor;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.shader.LandscapeShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.GLStateHelper;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class LandscapeRenderer implements Animated {

    private final List<@NonNull LandscapeLeaf> render_list = new ArrayList<>();
    private final GUIRoot gui_root;
    private final @NonNull World world;
    private final @NonNull Texture diffuseMap;
    private final @NonNull Texture normalMap;
    private final @NonNull Texture detailMap;
    private final @NonNull PatchMesh patchMesh;
    private final LandscapeShader shader = new LandscapeShader();

    public @NonNull LandscapeShader getShader() {
        return shader;
    }

    public LandscapeRenderer(@NonNull World world, @NonNull WorldInfo world_info, GUIRoot gui_root, @NonNull AnimationManager manager) {
        this.world = world;
        this.gui_root = gui_root;
        this.diffuseMap = world_info.maps.diffuse();
        this.normalMap = world_info.maps.normal();
        this.detailMap = world_info.detail;
        this.patchMesh = new PatchMesh();
        
        manager.registerAnimation(this);
    }

    public @NonNull HeightMap getHeightMap() {
        return world.getHeightMap();
    }

    public void pick(@NonNull CameraState camera, boolean visible_override, @NonNull Set<LandscapeLeaf> set) {
        doPrepareAll(camera, visible_override, set);
    }

    public void prepareAll(@NonNull CameraState camera, boolean visible_override) {
        render_list.clear();
        doPrepareAll(camera, visible_override, render_list);
    }
    
    private void doPrepareAll(@NonNull CameraState camera, final boolean visible_override, @NonNull Collection<LandscapeLeaf> result) {
        var patch_visitor = new Visitor(camera, visible_override, result);
        world.getPatchRoot().visit(patch_visitor);
    }

    public void renderAll(@NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        try (var _ = shader.use();
             var _ = GLStateHelper.blend(false);
             var _ = GLStateHelper.depthTest(true);
             var _ = GLStateHelper.cullFace(false);
             var _ = state.getFog().setup(shader, state.getCurrentZ())) {

            shader.setUniformMatrix4(LandscapeShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniformMatrix4(LandscapeShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniform(LandscapeShader.Uniforms.LIGHT_DIRECTION, -1f, 0f, 1f);
            shader.setUniform(LandscapeShader.Uniforms.GLOBAL_AMBIENT, 0.65f, 0.65f, 0.65f);
            
            // Set VTF Uniforms
            shader.setUniform(LandscapeShader.Uniforms.WORLD_SIZE, (float) world.getHeightMap().getMetersPerWorld());
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_SCALE, Globals.LANDSCAPE_DETAIL_REPEAT_RATE);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuseMap.getHandle());
            shader.setUniform(LandscapeShader.Uniforms.DIFFUSE_MAP, 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMap.getHandle());
            shader.setUniform(LandscapeShader.Uniforms.NORMAL_MAP, 1);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, detailMap.getHandle());
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_MAP, 2);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE3);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, world.getHeightMap().getHeightTexture().getHandle());
            shader.setUniform(LandscapeShader.Uniforms.HEIGHT_MAP, 3);

            if (Globals.draw_landscape) {
                patchMesh.bind(shader);
                float patchSize = world.getHeightMap().getMetersPerPatch();
                for (LandscapeLeaf leaf : render_list) {
                    shader.setUniform(LandscapeShader.Uniforms.PATCH_OFFSET, leaf.getPatchX() * patchSize, leaf.getPatchY() * patchSize);
                    patchMesh.draw();
                }
                patchMesh.unbind(shader);
            }
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }
    }

    public void debugRender(@NonNull CameraState frustum_state) {
        if (Globals.isBoundsEnabled(BoundingMode.LANDSCAPE)) {
            for (LandscapeLeaf patch : render_list) {
                RenderTools.draw(patch, BoundingMode.LANDSCAPE, 1f, 0f, 0f);
            }
        }
    }

    @Override
    public void animate(float t) {
        // No animation needed for static VTF geometry
    }

    @Override
    public void updateChecksum(@NonNull StateChecksum sum) {
    }

    // Shadow rendering supported
    void renderShadow(@NonNull ShaderProgram shader, int patch_x, int patch_y, int start_x, int start_y, int end_x, int end_y) {
        float patchSize = world.getHeightMap().getMetersPerPatch();
        shader.setUniform("u_PatchOffset", patch_x * patchSize, patch_y * patchSize);
        patchMesh.bind(shader);
        patchMesh.draw();
        patchMesh.unbind(shader);
    }

    private static final class Visitor implements PatchGroupVisitor {
        private @NonNull CameraState camera;
        private boolean visible_override;
        private @NonNull Collection<LandscapeLeaf> result;

        private Visitor(@NonNull CameraState camera, boolean visible_override, @NonNull Collection<LandscapeLeaf> result) {
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