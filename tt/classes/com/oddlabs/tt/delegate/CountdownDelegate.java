package com.oddlabs.tt.delegate;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.resource.FontFile;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;

import org.lwjgl.opengl.GL11;

import java.util.ResourceBundle;

public strictfp class CountdownDelegate extends CameraDelegate implements Updatable {

    private static final Font COUNTDOWN_FONT =
            (Font) Resources.findResource(new FontFile("/font/impact_72.font"));
    private static final ResourceBundle bundle =
            ResourceBundle.getBundle(CountdownDelegate.class.getName());

    private final WorldViewer viewer;
    private final Label countdown_label;
    private final Label waiting_label;
    private TimerAnimation timer_animation;
    private int countdown_time = 3;
    private boolean counting_down;

    public CountdownDelegate(WorldViewer viewer, Camera camera) {
        super(viewer.getGUIRoot(), camera);
        this.viewer = viewer;
        Font form_font = Skin.getSkin().getEditFont();
        String waiting_text = Utils.getBundleString(bundle, "waiting");
        this.waiting_label =
                new Label(
                        waiting_text,
                        form_font,
                        form_font.getWidth(waiting_text),
                        Label.ALIGN_CENTER);
        addChild(waiting_label);
        this.countdown_label =
                new Label(
                        "",
                        COUNTDOWN_FONT,
                        COUNTDOWN_FONT.getWidth(Utils.getBundleString(bundle, "fight")),
                        Label.ALIGN_CENTER);
        addChild(countdown_label);
        this.timer_animation = new TimerAnimation(viewer.getAnimationManagerLocal(), this, 1f);
        timer_animation.start();
        displayChangedNotify(LocalInput.getViewWidth(), LocalInput.getViewHeight());
    }

    @Override
    public void update(Object anim) {
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
            this.pop();
            Menu.completeGameSetupHack(viewer);
        } else {
            if (countdown_time == 0) {
                countdown_label.set(Utils.getBundleString(bundle, "fight"));
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
        waiting_label.setPos(
                (width - waiting_label.getWidth()) / 2, (height - waiting_label.getHeight()) / 2);
    }

    @Override
    protected void renderGeometry() {
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0f, 0f, 0f, 0.5f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(getWidth(), 0);
        GL11.glVertex2f(getWidth(), getHeight());
        GL11.glVertex2f(0, getHeight());
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
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
