package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import org.jspecify.annotations.NonNull;

public interface SceneRenderer {
    void render(@NonNull CameraState state, @NonNull MatrixStack modelView, @NonNull MatrixStack projection);
}
