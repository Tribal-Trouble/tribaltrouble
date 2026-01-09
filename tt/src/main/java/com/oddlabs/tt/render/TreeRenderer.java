package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.logging.Logger;

public final class TreeRenderer extends TreePicker implements SceneRenderer {
    private static final Logger logger = Logger.getLogger(TreeRenderer.class.getName());
    private final InstancedSpriteRenderer instancedSpriteRenderer;
    private final WaveAnimation wave_animation = new WaveAnimation();
    private final Cheat cheat;
    private final Matrix4f tempMatrix = new Matrix4f();

    TreeRenderer(Cheat cheat, SpriteSorter sprite_sorter, RespondManager respond_manager, InstancedSpriteRenderer instancedSpriteRenderer) {
        super(sprite_sorter, respond_manager);
        this.cheat = cheat;
        this.instancedSpriteRenderer = instancedSpriteRenderer;
    }

    @Override
    public void render(@NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        wave_animation.setTime(LocalEventQueue.getQueue().getTime());
        
        if (!Globals.draw_trees || !cheat.draw_trees) {
            // Just clear lists if not drawing
            clearLists();
            return;
        }

        List<TreeSupply>[] render_lists = getRenderLists();
        List<TreeSupply>[] respond_render_lists = getRespondRenderLists();
        
        AbstractTreeGroup.TreeType[] ordinals = AbstractTreeGroup.TreeType.values();
        
        for (int i = 0; i < render_lists.length; i++) {
            renderList(getTrees().get(ordinals[i]), render_lists[i], false);
        }
        for (int i = 0; i < respond_render_lists.length; i++) {
            if (!respond_render_lists[i].isEmpty())
                renderList(getTrees().get(ordinals[i]), respond_render_lists[i], true);
        }
    }
    
    private void clearLists() {
        for (List<TreeSupply> list : getRenderLists()) list.clear();
        for (List<TreeSupply> list : getRespondRenderLists()) list.clear();
    }

    private void prepareMatrix(@NonNull TreeSupply tree) {
        tempMatrix.set(tree.getMatrix());
        if (tree.isEmpty()) {
            float time = tree.getTreeFallProgress();
            tempMatrix.translate(0f, 0f, -13f * (time * time * time * time * time * time));
            tempMatrix.rotate((float) Math.toRadians(90f * time * time), 1f, 0f, 0f);
        } else {
            float scale = tree.getScale();
            tempMatrix.scale(scale, scale, scale);
            wave_animation.mulRotation(tempMatrix);
        }
    }

    private void renderList(@NonNull Tree tree, @NonNull List<TreeSupply> render_list, boolean respond) {
        SpriteList crownList = tree.getCrown();
        SpriteList trunkList = tree.getTrunk();
        
        for (TreeSupply supply : render_list) {
            prepareMatrix(supply);
            // Render Crown (Sprite 0). Blend = false, DepthWrite = true for opaque trees.
            instancedSpriteRenderer.add(crownList, 0, 0, 0f, 0, respond, false, true, true, tempMatrix, Color.WHITE, Color.WHITE);
            // Render Trunk (Sprite 0). Blend = false, DepthWrite = true.
            instancedSpriteRenderer.add(trunkList, 0, 0, 0f, 0, respond, false, true, true, tempMatrix, Color.WHITE, Color.WHITE);
        }
        render_list.clear();
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
}
