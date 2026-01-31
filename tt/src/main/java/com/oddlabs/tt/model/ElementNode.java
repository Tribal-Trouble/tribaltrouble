package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class ElementNode<T extends Element<T>> extends AbstractElementNode<T> {
	private static final int MIN_NODE_SIZE = 4;
	/*
	 * child2 | child3
	 * ----------------
	 * child0 | child1
	 *
	 */

	private final @NonNull AbstractElementNode<T> child0;
	private final @NonNull AbstractElementNode<T> child1;
	private final @NonNull AbstractElementNode<T> child2;
	private final @NonNull AbstractElementNode<T>child3;

	public ElementNode(AbstractElementNode<T> owner/*, int level*/, int size, int x, int y) {
		super(owner/*, level*/);
		int child_size = size >> 1;
		child0 = createChild(/*level, */child_size, x, y);
		child1 = createChild(/*level, */child_size, x + child_size, y);
		child2 = createChild(/*level, */child_size, x, y + child_size);
		child3 = createChild(/*level, */child_size, x + child_size, y + child_size);

		checkBoundsXY(child0);
		checkBoundsXY(child1);
		checkBoundsXY(child2);
		checkBoundsXY(child3);
	}

	private @NonNull AbstractElementNode<T> createChild(/*int level,*/ int size, int x, int y) {
		if (size != MIN_NODE_SIZE)
			return new ElementNode<>(this, /*level + 1, */size, x, y);
		else
			return new ElementLeaf<>(this, /*level + 1, */size, x, y);
	}

	@Override
	protected AbstractElementNode<T> doInsertElement(@NonNull T model) {
		incElementCount();
		if (model.bmin_x >= getCX()) {
			if (model.bmin_y >= getCY())
				return child3.insertElement(model);
			else if (model.bmax_y <= getCY())
				return child1.insertElement(model);
		} else if (model.bmax_x <= getCX()) {
			if (model.bmin_y >= getCY())
				return child2.insertElement(model);
			else if (model.bmax_y <= getCY())
				return child0.insertElement(model);
		}
		return addElement(model);
	}

	@Override
	public void visit(@NonNull ElementNodeVisitor<T> visitor) {
		visitor.visitNode(this);
	}

	public void visitChildren(ElementNodeVisitor<T> visitor) {
		if (getChildCount() > 0) {
			child0.visit(visitor);
			child1.visit(visitor);
			child2.visit(visitor);
			child3.visit(visitor);
		}
	}
}
