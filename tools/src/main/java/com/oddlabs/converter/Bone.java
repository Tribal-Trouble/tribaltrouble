package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;

public record Bone(@NonNull String name, byte index, @NonNull Bone @NonNull [] children) {
    public Bone(@NonNull String name, byte index, @NonNull Bone @NonNull [] children) {
        this.name = name;
        this.children = children;
        this.index = index;
    }


}

