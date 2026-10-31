package com.oddlabs.converter.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/** A skeleton file: the bones with their parents, and the rest pose as one transform per bone. */
public record SkeletonFile(@NonNull Bones bones, @JsonProperty("init_pose") @NonNull InitPose initPose) {
}
