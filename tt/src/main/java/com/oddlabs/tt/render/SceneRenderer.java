package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.state.RenderContext;
import org.jspecify.annotations.NonNull;

public interface SceneRenderer {
    void render(@NonNull RenderContext context, @NonNull CameraState state, @NonNull MatrixStack modelView, @NonNull MatrixStack projection);
}
