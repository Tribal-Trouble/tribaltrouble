package com.oddlabs.converter.xml;

import com.oddlabs.geometry.AnimationInfo;
import org.jspecify.annotations.NonNull;

/** A clip file: loop or plain (plays once), with the world units a walk covers per cycle. */
public record Animation(@NonNull String name, float wpc, AnimationInfo.@NonNull AnimationType type,
                        @NonNull String path) {

    public Animation {
        if (wpc == 0f)
            throw new IllegalArgumentException("Animation " + name + " needs a wpc other than 0");
    }
}
