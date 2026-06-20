package com.oddlabs.tt.delegate;


import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * Base class for all top-level UI states (delegates) in the game. Delegates handle
 * specific interaction modes (e.g., menus, in-game selection, targeting) and participate
 * in both 2D and 3D rendering passes.
 */
public abstract class Delegate extends GUIObject {
    private static final Color.Linear BACKGROUND_ALPHA = Color.Linear.BLACK.alpha(0.3f);

    @Override
    protected boolean shouldHandleActivate() {
        return false;
    }

    Delegate() {
        setPos(0, 0);
        setCanFocus(true);
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        super.setFocus(direction);
        switchFocus(direction, false);
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        GUIRoot root = getParentGUIRoot();
        if (root != null) {
            displayChanged(root.getWidth(), root.getHeight());
            if (root.getModalDelegate() == null || root.getModalDelegate() == this) {
                restoreFocus();
            }
        } else {
            restoreFocus();
        }
    }

    public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues render_queues,
            @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
    }

    public void render2D(@NonNull GUIRenderer renderer) {
    }

    public boolean keyboardBlocked() {
        return false;
    }

    final void renderBackgroundAlpha(@NonNull GUIRenderer renderer) {
        renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), BACKGROUND_ALPHA);
    }
}
