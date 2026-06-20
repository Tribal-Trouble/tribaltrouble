package com.oddlabs.tt.landscape;


import com.oddlabs.tt.model.BoundingBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract sealed class AbstractPatchGroup extends BoundingBox permits PatchGroup, LandscapeLeaf {
    private final @Nullable AbstractPatchGroup parent;

    protected AbstractPatchGroup(@NonNull HeightMap heightmap, float patch_size, int x, int y,
            @Nullable AbstractPatchGroup parent) {
        this.parent = parent;
    }

    final void editHeight(float height) {
        checkBoundsZ(height);
        if (parent != null)
            parent.editHeight(height);
    }
}
