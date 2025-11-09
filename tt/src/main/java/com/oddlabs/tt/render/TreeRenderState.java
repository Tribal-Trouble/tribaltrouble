package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.TreeSupply;
import org.jspecify.annotations.NonNull;

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
	public void markDetailPolygon(PolyDetail level) {
		tree_renderer.markDetailPolygon(tree_supply, level);
	}

	@Override
	public int getTriangleCount(@NonNull PolyDetail level) {
        int index = level.ordinal();
		Tree tree = tree_renderer.getTrees().get(tree_supply.getTreeType());
        return switch (PolyDetail.values()[index]) {
            case HIGH_POLY ->
                    tree.getTrunk().getSprite(0).getTriangleCount() + tree.getCrown().getSprite(0).getTriangleCount();
            case LOW_POLY -> tree_renderer.getLowDetails().get(tree_supply.getTreeType()).getPolyCount();
        };
	}

	@Override
	public float getEyeDistanceSquared() {
		CameraState camera = tree_renderer.getCamera();
		return RenderTools.getEyeDistanceSquared(tree_supply, camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ());
	}
}
