package com.oddlabs.tt.landscape;


import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;

public abstract class AbstractPatchGroup extends BoundingBox {
    private final AbstractPatchGroup parent;

    protected AbstractPatchGroup(@NonNull HeightMap heightmap, float patch_size, int x, int y,
            AbstractPatchGroup parent) {
        this.parent = parent;
    }

    final void editHeight(float height) {
        checkBoundsZ(height);
        if (parent != null)
            parent.editHeight(height);
    }

    public abstract void visit(@NonNull PatchGroupVisitor visitor);
}
