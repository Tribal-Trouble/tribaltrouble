package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;

import java.util.Map;

public final class Skeleton {
    private final @NonNull Bone bone_root;
    private final @NonNull Map<@NonNull String, float @NonNull []> initial_pose;
    private final @NonNull Map<@NonNull String, @NonNull Bone> name_to_bone_map;

    public Skeleton(@NonNull Bone bone_root, @NonNull Map<@NonNull String, float[]> initial_pose,
            @NonNull Map<@NonNull String, @NonNull Bone> name_to_bone_map) {
        this.bone_root = bone_root;
        this.initial_pose = initial_pose;
        this.name_to_bone_map = name_to_bone_map;
    }

    public @NonNull Map<@NonNull String, @NonNull Bone> getNameToBoneMap() {
        return name_to_bone_map;
    }

    public @NonNull Bone getBoneRoot() {
        return bone_root;
    }

    public @NonNull Map<@NonNull String, float @NonNull []> getInitialPose() {
        return initial_pose;
    }
}
