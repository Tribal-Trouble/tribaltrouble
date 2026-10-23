package com.oddlabs.converter;

import com.oddlabs.converter.xml.BoneEntry;
import com.oddlabs.converter.xml.SkeletonFile;
import com.oddlabs.converter.xml.Xml;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkeletonLoader {
    private SkeletonLoader() {
    }

    public static @NonNull Skeleton loadSkeleton(@NonNull Path file) {
        try {
            return buildSkeleton(Xml.read(file, SkeletonFile.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static @NonNull Skeleton buildSkeleton(@NonNull SkeletonFile skeleton) {
        Map<String, Bone> name_to_bone_map = new HashMap<>();
        Map<String, float[]> initial_pose = AnimationLoader.pose(skeleton.initPose().transforms());
        Map<String, String> bone_parent_map = new HashMap<>();
        for (BoneEntry bone : skeleton.bones().entries())
            bone_parent_map.put(bone.name(), bone.parent());
        Map<String, List<String>> bone_children_map = new HashMap<>();
        String root = null;
        for (String name : bone_parent_map.keySet()) {
            String parent = bone_parent_map.get(name);
            if (bone_parent_map.get(parent) == null) {
                if (root != null) {
                    IO.println("WARNING: Multiple roots in skeleton, root = " + root + ", additional root = " + name);
                    parent = root;
                    bone_parent_map.put(name, parent);
                } else
                    root = name;
            }
            List<String> parent_children = bone_children_map.computeIfAbsent(parent, _ -> new ArrayList<>());
            parent_children.add(name);
        }
        Bone bone_root = buildBone((byte) 0, bone_children_map, root, name_to_bone_map);
        return new Skeleton(bone_root, initial_pose, name_to_bone_map);
    }

    private static @NonNull Bone buildBone(byte index,
            @NonNull Map<@NonNull String, @NonNull List<@NonNull String>> bone_children_map, @NonNull String bone_name,
            @NonNull Map<@NonNull String, @NonNull Bone> name_to_bone_map) {
        List<String> children_list = bone_children_map.getOrDefault(bone_name, List.of());
        Bone[] children_array = new Bone[children_list.size()];
        for (int i = 0; i < children_array.length; i++) {
            String child_name = children_list.get(i);
            Bone child_bone = buildBone(index, bone_children_map, child_name, name_to_bone_map);
            children_array[i] = child_bone;
            index = (byte) (child_bone.index() + 1);
        }
        Bone bone = new Bone(bone_name, index, children_array);
        name_to_bone_map.put(bone_name, bone);
        return bone;
    }
}
