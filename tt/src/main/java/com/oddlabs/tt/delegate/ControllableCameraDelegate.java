package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.viewer.WorldViewer;
import org.lwjgl.input.Keyboard;

public abstract class ControllableCameraDelegate extends InGameDelegate {
	private final GameCamera game_camera;
	private FirstPersonDelegate first_person_delegate;

	public ControllableCameraDelegate(WorldViewer viewer, GameCamera game_camera) {
		super(viewer, game_camera);
		this.game_camera = game_camera;
	}

        @Override
	public void keyPressed(KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_F:
				pushFirstPersonDelegate(true);
				break;
			case Keyboard.KEY_Z:
				pushZoomDelegate();
				break;
			default:
				super.keyPressed(event);
				break;
		}
	}

        @Override
	public void mousePressed(int button, int x, int y) {
		if (button == LocalInput.MIDDLE_BUTTON) {
			pushFirstPersonDelegate(false);
		}
	}

        @Override
	public void mouseReleased(int button, int x, int y) {
		if (button == LocalInput.MIDDLE_BUTTON && first_person_delegate != null) {
			first_person_delegate.mouseReleased(button, x, y);
		}
	}

        @Override
	public void mouseScrolled(int amount) {
		getCamera().mouseScrolled(amount);
	}

        @Override
	public void mouseMoved(int x, int y) {
		getCamera().mouseMoved(x, y);
	}

        @Override
	public final boolean canScroll() {
		mouseMoved(LocalInput.getMouseX(), LocalInput.getMouseY());
		return getGUIRoot().getModalDelegate() == null;
	}

        @Override
	public void mouseDragged(int button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		if (button == LocalInput.MIDDLE_BUTTON && first_person_delegate != null) {
			first_person_delegate.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
		}
	}

	private void pushFirstPersonDelegate(boolean key_pressed) {
		first_person_delegate = new FirstPersonDelegate(getViewer(), getCamera().getState(), key_pressed);
		getGUIRoot().pushDelegate(first_person_delegate);
	}
	
	private void pushZoomDelegate() {
		getGUIRoot().pushDelegate(new ZoomDelegate(getViewer(), game_camera));
	}
}
