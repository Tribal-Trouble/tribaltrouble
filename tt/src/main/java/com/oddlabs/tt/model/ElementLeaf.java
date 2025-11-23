package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.HeightMap;
import org.jspecify.annotations.NonNull;

public final class ElementLeaf<T extends Element<T>> extends AbstractElementNode<T> {
	public ElementLeaf(AbstractElementNode<T> owner/*, int level*/, int size, int x, int y) {
		super(owner/*, level*/);
		setBounds(x*HeightMap.METERS_PER_UNIT_GRID, (x + size)*HeightMap.METERS_PER_UNIT_GRID, y*HeightMap.METERS_PER_UNIT_GRID, (y + size)*HeightMap.METERS_PER_UNIT_GRID, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
	}

	@Override
	protected @NonNull AbstractElementNode<T> doInsertElement(@NonNull T model) {
		incElementCount();
		return addElement(model);
	}

	@Override
	public void visit(@NonNull ElementNodeVisitor<T> visitor) {
		visitor.visitLeaf(this);
	}
}
