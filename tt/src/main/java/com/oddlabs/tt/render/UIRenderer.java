package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.viewer.AmbientAudio;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public interface UIRenderer {
	void render(@NonNull RenderContext context, AmbientAudio ambient, CameraState camera_state, GUIRoot gui_root);

	void pickHover(boolean can_hover_behind, CameraState camera, int x, int y);

	@Nullable ToolTip getToolTip();

	boolean isCheater();

	void startFrame(@NonNull RenderContext context);

	void endFrame(@NonNull RenderContext context, @NonNull Consumer<@NonNull RenderContext> guiRenderCallback);
}
