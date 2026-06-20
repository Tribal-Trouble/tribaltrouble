package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.viewer.AmbientAudio;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Container for the 2D user interface
 */
public final class GUI implements Animated {
    private final GUIRenderer guiRenderer = new GUIRenderer();
    private @NonNull GUIRoot current_root = createRoot();
    private @Nullable Fade fade;
    private @Nullable UIRenderer renderer;
    private final CameraState frustum_state = new CameraState();

    public GUI() {
    }

    public @NonNull GUIRoot newFade() {
        return newFade(null, null);
    }

    public @NonNull GUIRoot newFade(@Nullable Fadable fadable, @Nullable UIRenderer renderer) {
        GUIRoot gui_root = createRoot();
        newFade(fadable, gui_root, renderer);
        return gui_root;
    }

    public @NonNull GUIRoot newFade(@Nullable Fadable fadable, @NonNull GUIRoot gui_root,
            @Nullable UIRenderer renderer) {
        fade = new Fade(fadable, gui_root, renderer);
        Renderer.getRenderer().getEventQueue().getManager().registerAnimation(this);
        return gui_root;
    }

    public @NonNull GUIRoot createRoot() {
        GUIRoot gui_root = new GUIRoot(this);
        // This happens early before the viewport is fully initialized
        var window = Renderer.getRenderer().getWindow();
        gui_root.displayChanged(window.getWidth(), window.getHeight());
        return gui_root;
    }

    @Override
    public void animate(float t) {
        if (fade != null) {
            fade.animate(this, t);
        }
    }

    void stopFade() {
        Renderer.getRenderer().getEventQueue().getManager().removeAnimation(this);
        fade = null;
    }

    void switchRoot(@NonNull GUIRoot gui_root, @Nullable UIRenderer renderer) {
        current_root.removeTree();
        current_root = gui_root;
        this.renderer = renderer;
    }

    public @NonNull GUIRoot getGUIRoot() {
        return current_root;
    }

    @Nullable
    Fade getFade() {
        return fade;
    }

    public @Nullable UIRenderer getRenderer() {
        return renderer;
    }

    public void render(@NonNull AmbientAudio ambient) {
        Matrix4f proj = new Matrix4f();
        Matrix4f modelView = new Matrix4f();
        var guiRoot = getGUIRoot();
        CameraState camera = guiRoot.getDelegate().getCamera().getState();

        RenderContext context = Renderer.getRenderer().getRenderContext();

        camera.setView(guiRoot.multProjection(proj.identity()), context.getViewportWidth(), context
                .getViewportHeight());
        modelView.set(camera.getModelView());

        if (!Globals.frustum_freeze) {
            frustum_state.set(camera);
        }

        if (renderer != null && !renderer.isClosed()) {
            renderer.startFrame(context);
        } else {
            context.clear(true, true);
        }

        if (renderer != null && !renderer.isClosed())
            renderer.render(context, ambient, frustum_state, current_root);

        if (renderer != null && !renderer.isClosed()) {
            renderer.endFrame(context, this::renderGUI);
        } else {
            renderGUI(context);
        }
    }

    public void pickHover() {
        var guiRoot = getGUIRoot();
        CameraState camera = guiRoot.getDelegate().getCamera().getState();
        GUIObject gui_hit = guiRoot.getCurrentGUIObject();
        if (renderer != null) {
            var localInput = Renderer.getLocalInput();
            renderer.pickHover(gui_hit.canHoverBehind(), camera, localInput.getMouseX(), localInput.getMouseY());
        }
    }

    private void renderGUI(@NonNull RenderContext context) {
        GUIRoot guiRoot = getGUIRoot();

        // If we are rendering directly to the back buffer (e.g. loading screen),
        // we must set the correct blend mode here.
        // During gameplay, PostProcessor.renderComposite sets per-buffer blend modes.
        try (var _ = (renderer == null || renderer.isClosed()) ? context.withBlendMode(BlendMode.PREMULTIPLIED)
                : (ScopedState) () -> {
                }) {
            guiRenderer.renderFrame(context, guiRoot.getWidth(), guiRoot.getHeight(), () -> {
                guiRoot.render(guiRenderer);
                guiRoot.renderTopmost(guiRenderer, renderer != null ? renderer.getToolTip() : null, renderer != null
                        && renderer.isCheater());
            });
        }
    }
}
