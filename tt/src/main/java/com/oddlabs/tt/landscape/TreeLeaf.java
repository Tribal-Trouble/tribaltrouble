package com.oddlabs.tt.landscape;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class TreeLeaf extends AbstractTreeGroup {
    private @NonNull TreeSupply @NonNull [] infos = new TreeSupply[0];

    public TreeLeaf(@Nullable AbstractTreeGroup parent) {
        super(parent);
    }

    void insertTree(@NonNull TreeSupply tree) {
        TreeSupply[] new_infos = Arrays.copyOf(infos, infos.length + 1);
        new_infos[new_infos.length - 1] = tree;
        infos = new_infos;
    }

    @Override
    protected boolean initBounds() {
        if (infos.length != 0) {
            TreeSupply info = infos[0];
            info.initBounds();
            setBounds(info);
            for (int i = 1; i < infos.length; i++) {
                info = infos[i];
                info.initBounds();
                checkBounds(info);
            }
            super.initBounds();
            return true;
        }
        return false;
    }

    public @NonNull TreeSupply @NonNull [] getTrees() {
        return infos;
    }
}
