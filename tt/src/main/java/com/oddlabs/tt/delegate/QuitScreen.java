package com.oddlabs.tt.delegate;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

public final class QuitScreen extends CameraDelegate<Camera> implements Updatable<TimerAnimation> {
    private static final float DELAY = 5f;

    private static final int overlay_texture_width = 1024;
    private static final int overlay_texture_height = 1024;
    private static final int overlay_image_width = 800;
    private static final int overlay_image_height = 600;
    private static final String overlay_texture_name = "/textures/gui/quitscreen";

    private final @NonNull GUIImage overlay;
    private final TimerAnimation delay_timer = new TimerAnimation(this, DELAY);
    private boolean key_pressed = false;
    private boolean time_out = false;

    public QuitScreen(@NonNull GUIRoot gui_root, Camera camera) {
        super(gui_root, camera);
        setCanFocus(true);
        setFocusCycle(true);

        overlay = new GUIImage(gui_root.getWidth(), gui_root.getHeight(), 0f, 0f, (float) overlay_image_width / overlay_texture_width, (float) overlay_image_height / overlay_texture_height, overlay_texture_name);
        overlay.setPos(0, 0);
        addChild(overlay);

        GUIRoot quit_root = gui_root.getGUI().newFade();

        delay_timer.start();

        quit_root.pushDelegate(this);
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);
        overlay.setDim(width, height);
    }

    @Override
    public void update(@NonNull TimerAnimation anim) {
        delay_timer.stop();
        time_out = true;
        quit();
    }

    private void quit() {
        if (key_pressed && time_out)
            Renderer.shutdown();
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            key_pressed = true;
            quit();
            event.consume();
        }
        super.handleInput(event);
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        key_pressed = true;
        quit();
    }
}
