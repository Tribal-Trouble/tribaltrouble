package com.oddlabs.converter;

import com.oddlabs.converter.xml.ClipFile;
import com.oddlabs.converter.xml.Frame;
import com.oddlabs.converter.xml.BoneTransform;
import com.oddlabs.converter.xml.Xml;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnimationLoader {
    private AnimationLoader() {
    }

    /** The clip as one pose per frame, in frame order, each pose mapping a bone name to its matrix. */
    public static @NonNull Map<@NonNull String, float @NonNull []> @NonNull [] loadAnimation(@NonNull Path file) {
        ClipFile clip;
        try {
            clip = Xml.read(file, ClipFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        @SuppressWarnings("unchecked") var poses = (Map<String, float[]>[]) new Map[clip.frames().size()];
        for (Frame frame : clip.frames()) {
            if (frame.index() >= poses.length || poses[frame.index()] != null)
                throw new IllegalArgumentException(
                        file + " numbers its frames with gaps or repeats at " + frame.index());
            poses[frame.index()] = pose(frame.transforms());
        }
        return poses;
    }

    static @NonNull Map<@NonNull String, float @NonNull []> pose(@NonNull List<BoneTransform> transforms) {
        Map<String, float[]> pose = new HashMap<>();
        for (BoneTransform transform : transforms)
            pose.put(transform.name(), transform.matrix());
        return pose;
    }
}
