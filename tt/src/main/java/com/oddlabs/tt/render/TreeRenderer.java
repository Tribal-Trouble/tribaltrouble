package com.oddlabs.tt.render;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.viewer.Cheat;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class TreeRenderer extends TreePicker {
	private final TreeLowDetail tree_low_detail;
	private final WaveAnimation wave_animation = new WaveAnimation();
	private final Cheat cheat;

	TreeRenderer(World world, Cheat cheat, Landscape.TerrainType terrain, List<int[]> tree_positions, List<int[]> palm_tree_positions, SpriteSorter sprite_sorter, RespondManager respond_manager) {
		super(sprite_sorter, respond_manager);
		this.cheat = cheat;
		this.tree_low_detail = new TreeLowDetail(world, getTrees(), getLowDetails(), tree_positions, palm_tree_positions, terrain);
		tree_low_detail.build(world.getTreeRoot());
	}

	public TreeLowDetail getLowDetail() {
		return tree_low_detail;
	}

	private void renderLowDetail(AbstractTreeGroup group) {
		tree_low_detail.renderLowDetail(group.getLowDetailStart(), group.getLowDetailCount());
	}

	void renderAll() {
		wave_animation.setTime(LocalEventQueue.getQueue().getTime());
		List<AbstractTreeGroup> low_detail_render_list = getLowDetailRenderList();
		tree_low_detail.setupTrees();
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, .3f);
		GL11.glDisable(GL11.GL_CULL_FACE);
		for (int i = 0; i < low_detail_render_list.size(); i++) {
			AbstractTreeGroup group = low_detail_render_list.get(i);
			low_detail_render_list.set(i, null);
			if (Globals.draw_trees && cheat.draw_trees)
				renderLowDetail(group);
		}
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		low_detail_render_list.clear();
		List<TreeSupply>[] render_lists = getRenderLists();
		List<TreeSupply>[] respond_render_lists = getRespondRenderLists();
		for (int i = 0; i < render_lists.length; i++) {
            renderList(tree_low_detail.getTrees()[i], render_lists[i], false);
        }
		for (int i = 0; i < respond_render_lists.length; i++) {
            if (!respond_render_lists[i].isEmpty())
                renderList(tree_low_detail.getTrees()[i], respond_render_lists[i], true);
        }
	}

	private void loadMatrix(TreeSupply tree) {
		tree_low_detail.loadMatrix(tree.getMatrix());
		if (tree.isEmpty()) {
			float time = tree.getTreeFallProgress();
			GL11.glTranslatef(0f, 0f, -13f*(time*time*time*time*time*time));
			GL11.glRotatef(90f*time*time, 1f, 0f, 0f);
		} else {
			float scale = tree.getScale();
			GL11.glScalef(scale, scale, scale);
			wave_animation.mulRotation();
		}
	}

	private void renderList(Tree tree, List<TreeSupply> render_list, boolean respond) {
		tree.getCrown().getSprite(0).setup(0, respond);
        for (TreeSupply group : render_list) {
            if (Globals.isBoundsEnabled(Globals.BOUNDING_PLAYERS))
                RenderTools.draw(group);
            if (Globals.draw_trees && cheat.draw_trees) {
                GL11.glPushMatrix();
                loadMatrix(group);
                tree.getCrown().render(0, 0, 0);
                GL11.glPopMatrix();
            }
        }
		tree.getCrown().reset(0, respond, false);
		tree.getTrunk().getSprite(0).setup(0, respond);
		for (int i = 0; i < render_list.size(); i++) {
			TreeSupply group = render_list.get(i);
			render_list.set(i, null);
			if (Globals.draw_trees && cheat.draw_trees) {
				GL11.glPushMatrix();
				loadMatrix(group);
				tree.getTrunk().render(0, 0, 0);
				GL11.glPopMatrix();
			}
		}
		tree.getTrunk().reset(0, respond, false);
		render_list.clear();
	}

        @Override
	 boolean isPicking() {
		return false;
	}
}
