package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import com.oddlabs.tt.landscape.PatchGroup;
import com.oddlabs.tt.landscape.PatchGroupVisitor;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.shader.LandscapeShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.vbo.FloatVBO;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class LandscapeRenderer implements SceneRenderer, Animated {

    private final List<@NonNull LandscapeLeaf> render_list = new ArrayList<>();
    private final @NonNull World world;
    private final @NonNull Texture diffuseMap;
    private final @NonNull Texture normalMap;
    private final @NonNull Texture detailMap;
    private final @NonNull PatchMesh patchMesh;
    private final LandscapeShader shader = new LandscapeShader();
    private final Vector4f lightDir = new Vector4f();
    private FloatVBO instanceVBO;
    private FloatBuffer instanceBuffer;

    public @NonNull LandscapeShader getShader() {
        return shader;
    }

    public LandscapeRenderer(@NonNull World world, @NonNull WorldInfo world_info, @NonNull AnimationManager manager) {
        this.world = world;
        this.diffuseMap = world_info.maps().diffuse();
        this.normalMap = world_info.maps().normal();
        this.detailMap = world_info.detail();
        this.patchMesh = new PatchMesh();
        this.instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 2); // Initial capacity
        this.instanceBuffer = BufferUtils.createFloatBuffer(1024 * 2);

        manager.registerAnimation(this);
    }

    public @NonNull List<@NonNull LandscapeLeaf> getVisiblePatches() {
        return render_list;
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

    @Override
    public void render(@NonNull RenderContext context, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        try (var _ = shader.use();
             var _ = context.withBlendMode(BlendMode.NONE);
             var _ = context.withDepthMode(DepthMode.READ_WRITE);
             var _ = context.withCullMode(CullMode.NONE)) {

            // Set VTF Uniforms
            shader.setUniform(LandscapeShader.Uniforms.WORLD_SIZE, (float) world.getHeightMap().getMetersPerWorld());
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_SCALE, Globals.LANDSCAPE_DETAIL_REPEAT_RATE);

            context.setTexture(0, diffuseMap);
            shader.setUniform(LandscapeShader.Uniforms.DIFFUSE_MAP, 0);

            context.setTexture(1, normalMap);
            shader.setUniform(LandscapeShader.Uniforms.NORMAL_MAP, 1);

            context.setTexture(2, detailMap);
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_MAP, 2);

            context.setTexture(3, world.getHeightMap().getHeightTexture());
            shader.setUniform(LandscapeShader.Uniforms.HEIGHT_MAP, 3);

            if (Globals.draw_landscape && !render_list.isEmpty()) {
                int instanceCount = render_list.size();
                int requiredFloats = instanceCount * 2;

                // Resize buffer if needed
                if (instanceBuffer.capacity() < requiredFloats) {
                    int newCapacity = Math.max(instanceBuffer.capacity() * 2, requiredFloats);
                    instanceBuffer = BufferUtils.createFloatBuffer(newCapacity);
                    instanceVBO.close();
                    instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, newCapacity);
                }

                instanceBuffer.clear();
                float patchSize = world.getHeightMap().getMetersPerPatch();
                for (LandscapeLeaf leaf : render_list) {
                    instanceBuffer.put(leaf.getPatchX() * patchSize);
                    instanceBuffer.put(leaf.getPatchY() * patchSize);
                }
                instanceBuffer.flip();

                instanceVBO.makeCurrent();
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);

                patchMesh.bind();

                // Setup instance attribute (Location 4: in_InstancePatchOffset)
                int offsetLoc = 4; // Hardcoded location from shader layout
                GL20.glEnableVertexAttribArray(offsetLoc);
                GL20.glVertexAttribPointer(offsetLoc, 2, GL11.GL_FLOAT, false, 0, 0);
                GL33.glVertexAttribDivisor(offsetLoc, 1);

                patchMesh.drawInstanced(instanceCount);

                // Cleanup instance attribute
                GL33.glVertexAttribDivisor(offsetLoc, 0);
                GL20.glDisableVertexAttribArray(offsetLoc);

                patchMesh.unbind();
                // Unbind instance VBO to avoid leaking
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }

            context.setActiveTexture(0);
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

    // Shadow rendering supported
    void renderShadow(@NonNull ShaderProgram shader, int patch_x, int patch_y, int start_x, int start_y, int end_x, int end_y) {
        // Legacy shadow rendering (non-instanced for now as it renders specific sub-regions)
        // Would need to update shader to support instance attribute if we wanted to batch this too
        // But shadow rendering usually uses a specific projection/program
        // For now, we just set the attribute to the single offset
        // Since we removed the uniform, we must use the attribute
        // This requires binding a VBO with 1 instance data

        // Quick fix: Set attribute value directly using glVertexAttrib2f (Valid if attribute array is disabled)
        // But we need to ensure the attribute array is disabled.

        float patchSize = world.getHeightMap().getMetersPerPatch();
        GL20.glDisableVertexAttribArray(4); // Ensure array is disabled
        GL20.glVertexAttrib2f(4, patch_x * patchSize, patch_y * patchSize);

        patchMesh.bind();
        patchMesh.draw();
        patchMesh.unbind();
    }

    private static final class Visitor implements PatchGroupVisitor {
        private final @NonNull CameraState camera;
        private boolean visible_override;
        private final @NonNull Collection<LandscapeLeaf> result;

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