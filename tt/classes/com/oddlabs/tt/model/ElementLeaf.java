package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.HeightMap;

public final class ElementLeaf extends AbstractElementNode {
	public ElementLeaf(AbstractElementNode owner/*, int level*/, int size, int x, int y) {
		super(owner/*, level*/);
		setBounds(x*HeightMap.METERS_PER_UNIT_GRID, (x + size)*HeightMap.METERS_PER_UNIT_GRID, y*HeightMap.METERS_PER_UNIT_GRID, (y + size)*HeightMap.METERS_PER_UNIT_GRID, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
	}

    @Override
	protected AbstractElementNode doInsertElement(Element model) {
		incElementCount();
		return addElement(model);
	}

    @Override
	public void visit(ElementNodeVisitor visitor) {
		visitor.visitLeaf(this);
	}
}
