package com.oddlabs.tt.model;

import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.util.LinkedList;
import com.oddlabs.util.ListElement;
import org.jspecify.annotations.Nullable;

public abstract class Element<T> extends BoundingBox implements ListElement<T> {
	private final AbstractElementNode<T> element_root;
	private @Nullable AbstractElementNode<T>node_parent;

	private @Nullable LinkedList<T> parent;
	private @Nullable ListElement<T> next = null;
	private @Nullable ListElement<T> prior = null;

	private float render_pos_z;

	private float x;
	private float y;
	private float dir_x = 1f;
	private float dir_y = 0f;

	protected Element(AbstractElementNode<T> element_root) {
		this.element_root = element_root;
	}

	public abstract void visit(ElementVisitor visitor);

	protected void register() {
		node_parent = element_root.insertElement(this);
		assert node_parent != null;
	}

	protected final void reregister() {
		node_parent = element_root.reinsertElement(this);
		assert node_parent != null;
	}

	protected final boolean isRegistered() {
		return node_parent != null;
	}

	protected void remove() {
        assert node_parent != null;
		node_parent.removeElement(this);
		node_parent = null;
	}

	public final float getDirectionX() {
		return dir_x;
	}

	public final float getDirectionY() {
		return dir_y;
	}

	public final void setDirection(float dir_x, float dir_y) {
		this.dir_x = dir_x;
		this.dir_y = dir_y;
	}

	public void setPosition(float x, float y) {
//		assert World.getHeightMap().isInside(x, y): x + " " + y;
		this.x = x;
		this.y = y;
	}

	protected void setPositionZ(float z) {
		render_pos_z = z;
	}

	public final float getPositionX() {
		return x;
	}

	public final float getPositionY() {
		return y;
	}

	public final float getPositionZ() {
		return render_pos_z;
	}

	@Override
	public final void setListOwner(@Nullable LinkedList<T> parent) {
		this.parent = parent;
	}

	@Override
	public final @Nullable LinkedList<T> getListOwner() {
		return parent;
	}

	@Override
	public final void setPrior(@Nullable ListElement<T> prior) {
		this.prior = prior;
	}

	@Override
	public final void setNext(@Nullable ListElement<T> next) {
		this.next = next;
	}

	@Override
	public final @Nullable ListElement<T> getPrior() {
		return prior;
	}

	@Override
	public final @Nullable ListElement<T> getNext() {
		return next;
	}
}
