package com.oddlabs.tt.delegate;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.viewer.WorldViewer;

public strictfp class CountdownDelegate extends CameraDelegate implements Updatable {

    private final WorldViewer viewer;
    private final TimerAnimation timer_animation;
    private int countdown_time = 3;
    private final Label countdown_label;

    public CountdownDelegate(WorldViewer viewer, Camera camera) {
        super(viewer.getGUIRoot(), camera);
        this.viewer = viewer;
        String label = "3";
        this.countdown_label =
                new Label(label, Skin.getSkin().getHeadlineFont(), 200, Label.ALIGN_CENTER);
        addChild(countdown_label);
        this.timer_animation = new TimerAnimation(viewer.getAnimationManagerLocal(), this, 2f);
        timer_animation.start();
        displayChangedNotify(LocalInput.getViewWidth(), LocalInput.getViewHeight());
    }

    @Override
    public void update(Object anim) {
        if (!allPlayersReady()) {
            timer_animation.start();
            return;
        }
        countdown_time--;
        if (countdown_time < 0) {
            timer_animation.stop();
            this.pop();
            Menu.completeGameSetupHack(viewer);
        } else {
            if (countdown_time == 0) {
                countdown_label.set("Fight !");
            } else {
                countdown_label.set(String.valueOf(countdown_time));
            }
            timer_animation.start();
        }
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        super.displayChangedNotify(width, height);
        countdown_label.setPos(
                (width - countdown_label.getWidth()) / 2,
                (height - countdown_label.getHeight()) / 2);
    }

    @Override
    public void keyPressed(KeyboardEvent event) {}

    @Override
    public void keyReleased(KeyboardEvent event) {}

    @Override
    public void mouseScrolled(int amount) {}

    @Override
    public void mouseMoved(int x, int y) {}

    @Override
    public void mouseDragged(
            int button,
            int x,
            int y,
            int relative_x,
            int relative_y,
            int absolute_x,
            int absolute_y) {}

    @Override
    public void mousePressed(int button, int x, int y) {}

    @Override
    public void mouseReleased(int button, int x, int y) {}

    private boolean allPlayersReady() {
        return viewer.getPeerHub().isSynchronized();
    }
}
