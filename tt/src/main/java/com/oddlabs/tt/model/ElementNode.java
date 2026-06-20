package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ElementNode<T extends Element<T>> extends AbstractElementNode<T> {
    private static final int MIN_NODE_SIZE = 4;
    /*
     * child[2] | child[3]
     * -------------------
     * child[0] | child[1]
     *
     */
    private final @NonNull AbstractElementNode<T> @NonNull [] children;

    public ElementNode(@Nullable AbstractElementNode<T> owner, int size, int x, int y) {
        super(owner);
        int child_size = size >> 1;
        children = new AbstractElementNode[]{
                createChild(child_size, x, y),
                createChild(child_size, x + child_size, y),
                createChild(child_size, x, y + child_size),
                createChild(child_size, x + child_size, y + child_size)
        };

        checkBoundsXY(children[0]);
        checkBoundsXY(children[1]);
        checkBoundsXY(children[2]);
        checkBoundsXY(children[3]);
    }

    private @NonNull AbstractElementNode<T> createChild(int size, int x, int y) {
        if (size != MIN_NODE_SIZE)
            return new ElementNode<>(this, size, x, y);
        else
            return new ElementLeaf<>(this, size, x, y);
    }

    @Override
    protected AbstractElementNode<T> doInsertElement(@NonNull T model) {
        incElementCount();
        if (model.bmin_x >= getCX()) {
            if (model.bmin_y >= getCY())
                return children[3].insertElement(model);
            else if (model.bmax_y <= getCY())
                return children[1].insertElement(model);
        } else if (model.bmax_x <= getCX()) {
            if (model.bmin_y >= getCY())
                return children[2].insertElement(model);
            else if (model.bmax_y <= getCY())
                return children[0].insertElement(model);
        }
        return addElement(model);
    }

    public @NonNull AbstractElementNode<T> @NonNull [] children() {
        return children;
    }
}
