package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record Tree(@NonNull SpriteList trunk, @NonNull SpriteList crown,
                   @NonNull List<@NonNull SpriteList> props) {


    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Tree other_tree && crown == other_tree.crown && trunk == other_tree.trunk;
    }
}
