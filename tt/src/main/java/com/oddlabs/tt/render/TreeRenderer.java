package com.oddlabs.tt.render;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.viewer.Cheat;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.List;

public final class TreeRenderer extends TreePicker {
	private final @NonNull TreeLowDetail tree_low_detail;
	private final WaveAnimation wave_animation = new WaveAnimation();
	private final Cheat cheat;

    private static SpriteShader spriteShader; // For crown/trunk
    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

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

	void renderAll(@NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) { // Modified to accept stacks
        if (spriteShader == null) {
            spriteShader = new SpriteShader();
        }

		wave_animation.setTime(LocalEventQueue.getQueue().getTime());
		List<AbstractTreeGroup> low_detail_render_list = getLowDetailRenderList();
		
        // Setup low-detail trees (shader-based)
		tree_low_detail.setupTrees(modelViewStack, projectionStack);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT); // Save/Restore GL state
        GL11.glDisable(GL11.GL_CULL_FACE); // Low-detail trees are often billboards
        // Alpha test is handled by shader discard.
        
		for (int i = 0; i < low_detail_render_list.size(); i++) {
			AbstractTreeGroup group = low_detail_render_list.get(i);
			low_detail_render_list.set(i, null);
			if (Globals.draw_trees && cheat.draw_trees) {
                modelViewStack.push();
				renderLowDetail(group); // Render low-detail trees
                modelViewStack.pop();
            }
		}
		GL11.glPopAttrib(); // Restore saved GL state
		tree_low_detail.unbindTrees();
		low_detail_render_list.clear();

        // Setup high-detail trees (SpriteShader-based)
        spriteShader.use();
        projectionStack.toBuffer(matrixBuffer);
        spriteShader.setUniformMatrix4(SpriteShader.Uniforms.PROJECTION_MATRIX, false, matrixBuffer);
        spriteShader.setUniform(SpriteShader.Uniforms.LIGHT_DIR, -1f, 0f, 1f); // Fixed light dir
        
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND); // Trees often have alpha
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Alpha test is handled by shader discard or blending.
        // Culling will be handled by Sprite.setupShader.
        
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
        GL11.glPopAttrib();
        ShaderProgram.unbind();
	}

	private void loadMatrix(@NonNull TreeSupply tree, @NonNull MatrixStack stack) { // Modified to use MatrixStack
		// tree_low_detail.loadMatrix(tree.getMatrix()); // No longer directly call GL mult matrix.
		stack.multiply(tree.getMatrix()); // Apply base tree matrix
		if (tree.isEmpty()) {
			float time = tree.getTreeFallProgress();
			stack.translate(0f, 0f, -13f*(time*time*time*time*time*time));
			stack.rotate((float)Math.toRadians(90f*time*time), 1f, 0f, 0f); // JOML rotate expects radians
		} else {
			float scale = tree.getScale();
			stack.scale(scale, scale, scale);
			wave_animation.mulRotation(); // Wave animation is still FFP-like.
                                         // If it modifies GL state, it needs to be changed.
		}
	}

	private void renderList(@NonNull Tree tree, @NonNull List<TreeSupply> render_list, boolean respond, @NonNull MatrixStack modelViewStack) {
		tree.getCrown().getSprite(0).setupShader(spriteShader, 0, respond, tree.getCrown()); // Pass shader
        for (TreeSupply group : render_list) {
            if (Globals.isBoundsEnabled(BoundingMode.PLAYERS))
                RenderTools.draw(group);
            if (Globals.draw_trees && cheat.draw_trees) {
                modelViewStack.push();
                loadMatrix(group, modelViewStack); // Pass stack
                
                modelViewStack.toBuffer(matrixBuffer);
                spriteShader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, matrixBuffer);

                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 1f, 1f, 1f); // Default white color for trees

                tree.getCrown().getSprite(0).renderShader(spriteShader, 0, 0, tree.getCrown()); // Use shader render
                modelViewStack.pop();
            }
        }
		tree.getCrown().getSprite(0).resetShader(spriteShader, respond, false); // Pass shader
		tree.getTrunk().getSprite(0).setupShader(spriteShader, 0, respond, tree.getTrunk()); // Pass shader
		for (int i = 0; i < render_list.size(); i++) {
			TreeSupply group = render_list.get(i);
			render_list.set(i, null);
			if (Globals.draw_trees && cheat.draw_trees) {
				modelViewStack.push();
                loadMatrix(group, modelViewStack); // Pass stack

                modelViewStack.toBuffer(matrixBuffer);
                spriteShader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, matrixBuffer);

                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 1f, 1f, 1f); // Default white color for trees

				tree.getTrunk().getSprite(0).renderShader(spriteShader, 0, 0, tree.getTrunk()); // Use shader render
				modelViewStack.pop();
			}
		}
		tree.getTrunk().getSprite(0).resetShader(spriteShader, respond, false); // Pass shader
		render_list.clear();
	}

	 @Override
	 boolean isPicking() {
		return false;
	}
}
