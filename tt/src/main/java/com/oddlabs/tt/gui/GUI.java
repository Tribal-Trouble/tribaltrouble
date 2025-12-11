package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.viewer.AmbientAudio;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class GUI implements Animated {
    private final @NonNull GUIRenderer guiRenderer;
    private @NonNull GUIRoot current_root;
    private @Nullable Fade fade;
    private @Nullable UIRenderer renderer;

    public GUI() {
        this.guiRenderer = new GUIRenderer();
        this.current_root = createRoot();
    }

    public @NonNull GUIRoot newFade() {
        return newFade(null, null);
    }

    public @NonNull GUIRoot newFade(@Nullable Fadable fadable, @Nullable UIRenderer renderer) {
        GUIRoot gui_root = createRoot();
        newFade(fadable, gui_root, renderer);
        return gui_root;
    }

    public void newFade(@Nullable Fadable fadable, @NonNull GUIRoot gui_root, @Nullable UIRenderer renderer) {
        fade = new Fade(fadable, gui_root, renderer);
        LocalEventQueue.getQueue().getManager().registerAnimation(this);
    }

    public @NonNull GUIRoot createRoot() {
        GUIRoot gui_root = new GUIRoot(this);
        gui_root.displayChanged();
        return gui_root;
    }

    @Override
    public void animate(float t) {
        if (fade != null) {
            fade.animate(this, t);
        }
    }

    void stopFade() {
        LocalEventQueue.getQueue().getManager().removeAnimation(this);
        fade = null;
    }

    @Override
    public void updateChecksum(@NonNull StateChecksum checksum) {
    }

    void switchRoot(@NonNull GUIRoot gui_root, @Nullable UIRenderer renderer) {
        current_root.removeTree();
        current_root = gui_root;
        this.renderer = renderer;
    }

    public @NonNull GUIRoot getGUIRoot() {
        return current_root;
    }

    @Nullable Fade getFade() {
        return fade;
    }

    public void render(@NonNull AmbientAudio ambient, @NonNull CameraState frustum_state, @NonNull Matrix4f proj, @NonNull Matrix4f modelView) {
        boolean clear_color = renderer == null || renderer.clearColorBuffer();
        GL11.glClear(clear_color ? GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT : GL11.GL_DEPTH_BUFFER_BIT);
        if (renderer != null)
            renderer.render(ambient, frustum_state, current_root, proj, modelView);
        renderGUI();
    }

    public void pickHover() {
        CameraState camera = getGUIRoot().getDelegate().getCamera().getState();
        GUIObject gui_hit = getGUIRoot().getCurrentGUIObject();
        if (renderer != null)
            renderer.pickHover(gui_hit.canHoverBehind(), camera, LocalInput.getMouseX(), LocalInput.getMouseY());
    }

    private void renderGUI() {
        GUIRoot guiRoot = getGUIRoot();
        guiRenderer.renderFrame(guiRoot.getWidth(), guiRoot.getHeight(), () -> {
            guiRoot.render(guiRenderer);
            guiRoot.renderTopmost(guiRenderer, renderer != null ? renderer.getToolTip() : null, renderer != null && renderer.isCheater());
        });
    }
}
