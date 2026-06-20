package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.LinkedList;
import com.oddlabs.util.ListElementImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A renderable component with position, size, and optionally renderable children. */
public abstract class Renderable<R extends Renderable<R>> extends ListElementImpl<R> {
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;

    private final LinkedList<@NonNull R> children = new LinkedList<>();

    protected @Nullable R parent = null;

    public @NonNull Renderable<R> setDim(int w, int h) {
        width = w;
        height = h;
        return this;
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
            current.displayChanged(getWidth(), getHeight());
            current = current.getNext();
        }
    }

    /**
     * Window or, in fullscreen mode the display, dimensions changed.
     *
     * @param width width of the window/display in pixels
     * @param height height of the window/display in pixels.
     */
    protected void displayChangedNotify(int width, int height) {
    }

    public final void render(@NonNull GUIRenderer renderer) {
        render(renderer, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY);
    }

    /**
     * Render the component and children first applying appropriate clipping, transformation and setting the drawing
     * origin to relative (0,0).
     */
    protected void render(@NonNull GUIRenderer renderer, float clip_left, float clip_right, float clip_bottom,
            float clip_top) {
        if (this instanceof Clipped) {
            renderer.pushClip(getRootX(), getRootY(), getWidth(), getHeight());
            renderClipped(renderer, clip_left, clip_right, clip_bottom, clip_top);
            renderer.popClip();
        } else {
            renderClipped(renderer, clip_left, clip_right, clip_bottom, clip_top);
        }
    }

    private void renderClipped(@NonNull GUIRenderer renderer, float clip_left, float clip_right, float clip_bottom,
            float clip_top) {
        clip_left = Math.max(transformX(clip_left), 0);
        clip_right = Math.min(transformX(clip_right), width);
        clip_bottom = Math.max(transformY(clip_bottom), 0);
        clip_top = Math.min(transformY(clip_top), height);
        if (clip_left >= width || clip_right <= 0 || clip_bottom >= height || clip_top <= 0) {
            return;
        }

        if (!(this instanceof GUIRoot)) {
            renderer.getMatrixStack().push();
            renderer.getMatrixStack().translate(getX(), getY(), 0);
        }

        renderGeometry(renderer);

        R current = children.getLast();
        while (current != null) {
            current.render(renderer, clip_left, clip_right, clip_bottom, clip_top);
            current = current.getPrior();
        }

        postRender(renderer);

        if (!(this instanceof GUIRoot)) {
            renderer.getMatrixStack().pop();
        }
    }

    /**
     * Renders the geometry of this object. This method should only issue drawing commands for this specific object.
     * It should operate in its own local coordinate space, from (0,0) to (getWidth(), getHeight()).
     * The parent {@link #render(GUIRenderer, float, float, float, float)} method is responsible for setting up
     * the transformation matrix and clipping.
     *
     * @param renderer The renderer to use for drawing.
     */
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        // nothing to show
    }

    /**
     * Geometry rendering done after the children have been rendered. Useful for badging, etc.
     */
    protected void postRender(@NonNull GUIRenderer renderer) {
        // nothing to do
    }

    protected abstract boolean isFocusable();

    final float getRootX() {
        return parent == null ? 0 : (parent.getRootX() + getX());
    }

    final float getRootY() {
        return parent == null ? 0 : (parent.getRootY() + getY());
    }

    private float transformX(float x) {
        return x - getX();
    }

    private float transformY(float y) {
        return y - getY();
    }

    protected @Nullable R pick(float x, float y) {
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

    protected float getGlobalScale() {
        return parent != null ? parent.getGlobalScale() : 1.0f;
    }
}
