package com.oddlabs.converter2;

import java.util.Map;

public final class Skeleton {
	private final Bone bone_root;
	private final Map<String,float[]> initial_pose;
	private final Map<String,Bone> name_to_bone_map;

	public Skeleton(Bone bone_root, Map<String,float[]> initial_pose, Map<String,Bone> name_to_bone_map) {
		this.bone_root = bone_root;
		this.initial_pose = initial_pose;
		this.name_to_bone_map = name_to_bone_map;
	}

	public Map<String,Bone> getNameToBoneMap() {
		return name_to_bone_map;
	}

	public Bone getBoneRoot() {
		return bone_root;
	}

	public Map<String,float[]> getInitialPose() {
		return initial_pose;
	}
}
