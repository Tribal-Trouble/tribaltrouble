package com.oddlabs.tt.delegate;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * Displays a countdown before a multiplayer game starts.
 * Waits for all players to be synchronized, then counts down 3-2-1-Fight.
 */
public final class CountdownDelegate extends CameraDelegate<Camera> {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(CountdownDelegate.class.getName());
    private static final Vector4fc OVERLAY_COLOR = new Vector4f(0f, 0f, 0f, 0.5f);

    private final WorldViewer viewer;
    private final Label countdown_label;
    private final Label waiting_label;
    private final TimerAnimation timer_animation;
    private int countdown_time = 3;
    private boolean counting_down;

    public CountdownDelegate(@NonNull WorldViewer viewer, @NonNull Camera camera) {
        super(viewer.getGUIRoot(), camera);
        this.viewer = viewer;

        String waitingText = Utils.getBundleString(bundle, "waiting");
        waiting_label = new Label(waitingText, Skin.getSkin().getHeadlineFont(), Skin.getSkin().getHeadlineFont().getWidth(waitingText), Origin.AT_MIDDLE);
        addChild(waiting_label);

        String fightText = Utils.getBundleString(bundle, "fight");
        countdown_label = new Label("", Skin.getSkin().getHeadlineFont(), Skin.getSkin().getHeadlineFont().getWidth(fightText), Origin.AT_MIDDLE);
        addChild(countdown_label);

        timer_animation = new TimerAnimation(this::tick, 1f);
        timer_animation.start();
    }

    private void tick(@NonNull TimerAnimation anim) {
        if (!allPlayersReady()) {
            timer_animation.start();
            return;
        }
        if (!counting_down) {
            counting_down = true;
            waiting_label.remove();
            countdown_label.set("3");
            timer_animation.start();
            return;
        }
        countdown_time--;
        if (countdown_time < 0) {
            timer_animation.stop();
            pop();
            Menu.completeGameSetupHack(viewer);
        } else if (countdown_time == 0) {
            countdown_label.set(Utils.getBundleString(bundle, "fight"));
            timer_animation.start();
        } else {
            countdown_label.set(String.valueOf(countdown_time));
            timer_animation.start();
        }
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        super.displayChangedNotify(width, height);
        countdown_label.setPos((width - countdown_label.getWidth()) / 2, (height - countdown_label.getHeight()) / 2);
        waiting_label.setPos((width - waiting_label.getWidth()) / 2, (height - waiting_label.getHeight()) / 2);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), OVERLAY_COLOR);
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        // Consume all input during countdown
        event.consume();
    }

    private boolean allPlayersReady() {
        return viewer.getPeerHub() != null && viewer.getPeerHub().isSynchronized();
    }
}
