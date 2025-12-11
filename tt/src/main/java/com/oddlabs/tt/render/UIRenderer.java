package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.viewer.AmbientAudio;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public interface UIRenderer {
	void render(AmbientAudio ambient, CameraState camera_state, GUIRoot gui_root, Matrix4f proj, Matrix4f modelView);
	void pickHover(boolean can_hover_behind, CameraState camera, int x, int y);
	@Nullable ToolTip getToolTip();
    boolean isCheater();
	boolean clearColorBuffer();
}
