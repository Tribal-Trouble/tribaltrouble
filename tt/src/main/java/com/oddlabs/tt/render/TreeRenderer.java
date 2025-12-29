package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.shader.LitShader;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.viewer.Cheat;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class TreeRenderer extends TreePicker implements AutoCloseable {
    private final @NonNull TreeLowDetail tree_low_detail;
    private final WaveAnimation wave_animation = new WaveAnimation();
    private final Cheat cheat;

    private static final SpriteShader spriteShader = new SpriteShader(); // For crown/trunk

    TreeRenderer(@NonNull World world, Cheat cheat, Landscape.@NonNull TerrainType terrain, @NonNull List<int[]> tree_positions, @NonNull List<int[]> palm_tree_positions, SpriteSorter sprite_sorter, RespondManager respond_manager) {
        super(sprite_sorter, respond_manager);
        this.cheat = cheat;
        this.tree_low_detail = new TreeLowDetail(world, getTrees(), getLowDetails(), tree_positions, palm_tree_positions, terrain);
        tree_low_detail.build(world.getTreeRoot());
    }

    public @NonNull TreeLowDetail getLowDetail() {
        return tree_low_detail;
    }

    private void renderLowDetail(@NonNull AbstractTreeGroup group) {
        tree_low_detail.renderLowDetail(group.getLowDetailStart(), group.getLowDetailCount());
    }

    void renderAll(@NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        wave_animation.setTime(LocalEventQueue.getQueue().getTime());
        List<AbstractTreeGroup> low_detail_render_list = getLowDetailRenderList();

        boolean cullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try (var _ = tree_low_detail.setupTrees(state, modelViewStack, projectionStack)) {
            if (cullFaceEnabled) GL11.glDisable(GL11.GL_CULL_FACE);
            
            for (AbstractTreeGroup group : low_detail_render_list) {
                if (Globals.draw_trees && cheat.draw_trees) {
                    modelViewStack.push();
                    renderLowDetail(group);
                    modelViewStack.pop();
                }
            }
        } finally {
            if (cullFaceEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
            low_detail_render_list.clear();
        }

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        try (var _ = spriteShader.use();
             var _ = state.getFog().setup(spriteShader, state.getCurrentZ())) {
            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            spriteShader.setUniform(LitShader.LIGHT_DIR, -1f, 0f, 1f);
            spriteShader.setUniform(LitShader.GLOBAL_AMBIENT, 0.4f, 0.4f, 0.4f);

            if (!blendEnabled) GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            List<TreeSupply>[] render_lists = getRenderLists();
            List<TreeSupply>[] respond_render_lists = getRespondRenderLists();
            AbstractTreeGroup.TreeType[] ordinals = AbstractTreeGroup.TreeType.values();
            for (int i = 0; i < render_lists.length; i++) {
                renderList(tree_low_detail.getTrees().get(ordinals[i]), render_lists[i], false, modelViewStack);
            }
            for (int i = 0; i < respond_render_lists.length; i++) {
                if (!respond_render_lists[i].isEmpty())
                    renderList(tree_low_detail.getTrees().get(ordinals[i]), respond_render_lists[i], true, modelViewStack);
            }
        } finally {
            if (!blendEnabled) GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private void loadMatrix(@NonNull TreeSupply tree, @NonNull MatrixStack stack) {
        stack.multiply(tree.getMatrix());
        if (tree.isEmpty()) {
            float time = tree.getTreeFallProgress();
            stack.translate(0f, 0f, -13f * (time * time * time * time * time * time));
            stack.rotate((float) Math.toRadians(90f * time * time), 1f, 0f, 0f);
        } else {
            float scale = tree.getScale();
            stack.scale(scale, scale, scale);
            wave_animation.mulRotation(stack);
        }
    }

    private void renderList(@NonNull Tree tree, @NonNull List<TreeSupply> render_list, boolean respond, @NonNull MatrixStack modelViewStack) {
        renderPart(tree.getCrown(), 0, respond, render_list, modelViewStack);
        renderPart(tree.getTrunk(), 0, respond, render_list, modelViewStack);
        render_list.clear();
    }

    private void renderPart(@NonNull SpriteList spriteList, int spriteIndex, boolean respond, @NonNull List<TreeSupply> render_list, @NonNull MatrixStack modelViewStack) {
        Sprite sprite = spriteList.getSprite(spriteIndex);

        boolean cullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            if (sprite.culled && !cullFaceEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
            if (!sprite.culled && cullFaceEnabled) GL11.glDisable(GL11.GL_CULL_FACE);

            sprite.setupShaderUniforms(spriteShader, spriteIndex, respond);

            for (TreeSupply group : render_list) {
                if (Globals.draw_trees && cheat.draw_trees) {
                    modelViewStack.push();
                    loadMatrix(group, modelViewStack);
                    spriteShader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
                    spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 1f, 1f, 1f);
                    sprite.renderShader(spriteShader, 0, 0, spriteList);
                    modelViewStack.pop();
                }
            }
        } finally {
            if (sprite.culled && !cullFaceEnabled) GL11.glDisable(GL11.GL_CULL_FACE);
            if (!sprite.culled && cullFaceEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    public void debugRender(@NonNull List<TreeSupply> @NonNull [] render_lists, @NonNull List<TreeSupply> @NonNull [] respond_render_lists) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (List<TreeSupply> render_list : render_lists) {
                for (TreeSupply group : render_list) {
                    RenderTools.draw(group);
                }
            }
            for (List<TreeSupply> respond_render_list : respond_render_lists) {
                for (TreeSupply group : respond_render_list) {
                    RenderTools.draw(group);
                }
            }
        }
    }

    @Override
    boolean isPicking() {
        return false;
    }

    @Override
    public void close() {
        tree_low_detail.close();
    }
}
