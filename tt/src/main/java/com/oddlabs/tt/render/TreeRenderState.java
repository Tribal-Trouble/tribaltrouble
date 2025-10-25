package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.TreeSupply;

final class TreeRenderState implements LODObject {
	private final TreePicker tree_renderer;
	private TreeSupply tree_supply;

	TreeRenderState(TreePicker tree_renderer) {
		this.tree_renderer = tree_renderer;
	}

	void setup(TreeSupply tree_supply) {
		this.tree_supply = tree_supply;
	}

    @Override
	public void markDetailPoint() {
		tree_renderer.addToLowDetailRenderList(tree_supply);
	}

    @Override
	public void markDetailPolygon(int level) {
		tree_renderer.markDetailPolygon(tree_supply, level);
	}

    @Override
	public int getTriangleCount(int index) {
		Tree tree = tree_renderer.getTrees()[tree_supply.getTreeTypeIndex()];
            switch (index) {
                case SpriteRenderer.HIGH_POLY:
                    return tree.getTrunk().getSprite(0).getTriangleCount() + tree.getCrown().getSprite(0).getTriangleCount();
                case SpriteRenderer.LOW_POLY:
                    return tree_renderer.getLowDetails()[tree_supply.getTreeTypeIndex()].getPolyCount();
                default:
                    throw new RuntimeException();
            }
	}

    @Override
	public float getEyeDistanceSquared() {
		CameraState camera = tree_renderer.getCamera();
		return RenderTools.getEyeDistanceSquared(tree_supply, camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ());
	}
}
