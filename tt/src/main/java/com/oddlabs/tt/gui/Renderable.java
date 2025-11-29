package com.oddlabs.tt.gui;

import com.oddlabs.util.LinkedList;
import com.oddlabs.util.ListElementImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public abstract class Renderable<R extends Renderable<R>> extends ListElementImpl<R> {
	private int x = 0;
	private int y = 0;
	private int width = 0;
	private int height = 0;
	private float scale_x = 1f;
	private float scale_y = 1f;

	private final LinkedList<@NonNull R> children = new LinkedList<>();

	protected @Nullable R parent = null;

	public void setDim(int w, int h) {
		width = w;
		height = h;
	}

	public final int getWidth() {
		return width;
	}

	public final int getHeight() {
		return height;
	}

	public void setPos(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}

	public final int getNumChildren() {
		return children.size();
	}

	public @Nullable R getParent() {
		return parent;
	}

	public final void putLast(@NonNull R child) {
		children.putLast(child);
	}

	final void putFirst(@NonNull R child) {
		children.putFirst(child);
	}

	public void removeChild(@NonNull R child) {
		child.parent = null;
		children.remove(child);
	}

	protected final void clearChildren() {
		while (!children.isEmpty()) {
			getLastChild().remove();
		}
	}

	protected void doAdd() {
	}

	public void addChild(@NonNull R child) {
		child.remove();
		children.addFirst(child);
		child.parent = self();
		child.addTree();
	}

	protected final @Nullable R getLastChild() {
		return children.getLast();
	}

    protected final @Nullable R getFirstChild() {
		return children.getFirst();
	}

	public final void displayChanged(int width, int height) {
		displayChangedNotify(width, height);
		R current = children.getFirst();
		while (current != null) {
			current.displayChanged(width, height);
			current = current.getNext();
		}
	}

	protected void displayChangedNotify(int width, int height) {
	}

	protected final void setScale(float scale_x, float scale_y) {
		this.scale_x = scale_x;
		this.scale_y = scale_y;
	}

	public final float getScaleX() {
		return scale_x;
	}

	public final float getScaleY() {
		return scale_y;
	}

	public final void render() {
		render(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
	}

	protected void render(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		clip_left = Math.max(transformX(clip_left), 0);
		clip_right = Math.min( transformX(clip_right), width);
		clip_bottom = Math.max(transformY(clip_bottom), 0);
		clip_top = Math.min(transformY(clip_top), height);
		if (clip_left >= width || clip_right <= 0 || clip_bottom >= height || clip_top <= 0) {
			return;
		}

		if (!(this instanceof GUIRoot)) {
			GL11.glPushMatrix();
			if (scale_x != 1f || scale_y != 1f) {
				GL11.glScalef(scale_x, scale_y, 1f);
			}
			GL11.glTranslatef(getX(), getY(), 0);
		}

		renderGeometry(clip_left, clip_right, clip_bottom, clip_top);

        R current = children.getLast();
        while (current != null) {
            current.render(clip_left, clip_right, clip_bottom, clip_top);
            current = current.getPrior();
        }

		postRender();

		if (!(this instanceof GUIRoot)) {
			GL11.glPopMatrix();
		}
	}

	       protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
	               renderGeometry();
	       }
	
	       protected void renderGeometry() {
	       }
    protected void postRender() {
    }

	protected abstract boolean isFocusable();

	final float getRootX() {
        return parent == null ? 0 : (parent.getRootX() + getX()) * scale_x;
	}

	final float getRootY() {
        return parent == null ? 0 : (parent.getRootY() + getY()) * scale_y;
    }

	private float transformX(float x) {
		return x/scale_x - getX();
	}

	private float transformY(float y) {
		return y/scale_y - getY();
	}

	final @Nullable R pick(float x, float y) {
		float trans_x = transformX(x);
		float trans_y = transformY(y);
		if (isFocusable() && trans_x >= 0 && trans_y >= 0 && trans_x < getWidth() && trans_y < getHeight()) {
			R current = children.getFirst();
			while (current != null) {
				R picked = current.pick(trans_x, trans_y);
				if (picked != null)
					return picked;
				current = current.getNext();
			}
			return self();
		}
		return null;
	}

	void addTree() {
        doAdd();
        R current = children.getFirst();
        while (current != null) {
            current.addTree();
            current = current.getNext();
        }
	}

	final void removeTree() {
		doRemove();
		R current = children.getFirst();
		while (current != null) {
			current.removeTree();
			current = current.getNext();
		}
	}

	protected void doRemove() {
	}

	public void remove() {
		if (parent != null) {
			parent.removeChild(self());
		}
	}
}
