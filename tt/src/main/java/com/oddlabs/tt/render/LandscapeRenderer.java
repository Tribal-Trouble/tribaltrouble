package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.AbstractPatchGroup;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import com.oddlabs.tt.landscape.PatchGroup;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.shader.LandscapeShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.scenery.Sky;
import com.oddlabs.tt.scenery.Water;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Renders the 3D terrain landscape.
 */
public final class LandscapeRenderer implements SceneRenderer, Animated {
    private final List<@NonNull LandscapeLeaf> render_list = new ArrayList<>();
    private final @NonNull World world;
    private final @NonNull Texture diffuseMap;
    private final @NonNull Texture normalMap;
    private final @NonNull Texture detailMap;
    private final @NonNull Texture detailNormalMap;
    private final PatchMesh patchMesh = new PatchMesh();
    private final LandscapeShader shader = new LandscapeShader();
    private @Nullable Water water;
    private @NonNull FloatVBO instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 3);
    private @NonNull FloatBuffer instanceBuffer = BufferUtils.createFloatBuffer(1024 * 3);

    public @NonNull LandscapeShader getShader() {
        return shader;
    }

    public void setWater(@NonNull Water water) {
        this.water = water;
    }

    public LandscapeRenderer(@NonNull World world, @NonNull WorldInfo<Texture> world_info,
            @NonNull AnimationManager manager) {
        this.world = world;
        this.diffuseMap = world_info.maps().diffuse();
        this.normalMap = world_info.maps().normal();
        this.detailMap = world_info.detail();
        this.detailNormalMap = world_info.detailNormal();

        manager.registerAnimation(this);
    }

    public @NonNull Collection<@NonNull LandscapeLeaf> getVisiblePatches() {
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

    private void doPrepareAll(@NonNull CameraState camera, final boolean visible_override, @NonNull Collection<
            LandscapeLeaf> result) {
        traverse(world.getPatchRoot(), camera, visible_override, result);
    }

    private void traverse(@NonNull AbstractPatchGroup node, @NonNull CameraState camera, boolean visible_override,
            @NonNull Collection<LandscapeLeaf> result) {
        switch (node) {
            case PatchGroup group -> {
                RenderTools.FrustumIntersection frustum_state = RenderTools.FrustumIntersection.ALL_OUTSIDE;
                if (visible_override || (frustum_state = RenderTools.inFrustum(group, camera.getFrustum()))
                        != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
                    boolean next_visible_override = visible_override || frustum_state
                            == RenderTools.FrustumIntersection.ALL_INSIDE;
                    for (AbstractPatchGroup child : group.children()) {
                        traverse(child, camera, next_visible_override, result);
                    }
                }
            }
            case LandscapeLeaf leaf -> {
                if (visible_override || RenderTools.inFrustum(leaf, camera.getFrustum())
                        != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
                    result.add(leaf);
                }
            }
        }
    }

    @Override
    public void render(@NonNull RenderContext context, @NonNull CameraState state, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ALPHA); var _ = context.withDepthMode(
                DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.NONE)) {

            // Set VTF Uniforms
            shader.setUniform(LandscapeShader.Uniforms.WORLD_SIZE, (float) world.getHeightMap().getMetersPerWorld());
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_SCALE, Globals.LANDSCAPE_DETAIL_REPEAT_RATE);

            Color stdColor = Sky.SEA_BOTTOM_COLOR.get(world.getTerrainType());
            shader.setUniformColor3(LandscapeShader.Uniforms.SEA_BOTTOM_COLOR, stdColor);

            context.setTexture(0, diffuseMap);
            shader.setUniform(LandscapeShader.Uniforms.DIFFUSE_MAP, 0);

            context.setTexture(1, normalMap);
            shader.setUniform(LandscapeShader.Uniforms.NORMAL_MAP, 1);

            context.setTexture(2, detailMap);
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_MAP, 2);

            context.setTexture(3, world.getHeightMap()
                    .getClientState(HeightMapVisual.class)
                    .map(HeightMapVisual::getHeightTexture).orElseThrow());
            shader.setUniform(LandscapeShader.Uniforms.HEIGHT_MAP, 3);

            context.setTexture(4, detailNormalMap);
            shader.setUniform(LandscapeShader.Uniforms.DETAIL_NORMAL_MAP, 4);

            if (Globals.draw_landscape && !render_list.isEmpty()) {
                int instanceCount = render_list.size();
                int requiredFloats = instanceCount * 3;

                // Resize buffer if needed
                if (instanceBuffer.capacity() < requiredFloats) {
                    int newCapacity = Math.max(instanceBuffer.capacity() * 2, requiredFloats);
                    instanceBuffer = BufferUtils.createFloatBuffer(newCapacity);
                    instanceVBO.close();
                    instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, newCapacity);
                }

                instanceBuffer.clear();
                float patchSize = world.getHeightMap().getMetersPerPatch();
                int patchesPerWorld = world.getHeightMap().getPatchesPerWorld();
                Water activeWater = water;
                BitSet activeOceanPatches = activeWater != null ? activeWater.getOceanPatches() : null;
                for (LandscapeLeaf leaf : render_list) {
                    int px = leaf.getPatchX();
                    int py = leaf.getPatchY();
                    float waveScale = (activeOceanPatches != null && activeOceanPatches.get(py * patchesPerWorld + px))
                            ? 1.0f : 0.0f;
                    instanceBuffer.put(px * patchSize);
                    instanceBuffer.put(py * patchSize);
                    instanceBuffer.put(waveScale);
                }
                instanceBuffer.flip();

                instanceVBO.bind(context);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);

                patchMesh.bind();

                // Setup instance attribute (Location 4: in_InstancePatchOffset)
                int offsetLoc = 4; // Hardcoded location from shader layout
                GL20.glEnableVertexAttribArray(offsetLoc);
                GL20.glVertexAttribPointer(offsetLoc, 3, GL11.GL_FLOAT, false, 0, 0);
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
}
