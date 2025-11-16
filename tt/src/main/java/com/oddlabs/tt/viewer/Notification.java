package com.oddlabs.tt.viewer;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.gui.Arrow;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;

public class Notification implements Updatable {

    private static final float ACTIVE_SECONDS = 5f;

    private final float center_x;
    private final float center_y;
    private final NotificationManager manager;
    private final @NonNull TimerAnimation timer;
    private final @NonNull Arrow arrow;

    public Notification(@NonNull World world, @NonNull GUIRoot gui_root, float x, float y, NotificationManager manager, float r, float g, float b, @NonNull Audio sound, boolean show_always, @NonNull AnimationManager animation_manager) {
        this.center_x = x;
        this.center_y = y;
        this.manager = manager;
        this.timer = new TimerAnimation(animation_manager, this, ACTIVE_SECONDS);
        timer.start();
        this.arrow = new Arrow(world.getHeightMap(), gui_root, center_x, center_y, r, g, b, show_always);
        gui_root.addChild(arrow);
        world.getAudio().newAudio(new AudioParameters<>(sound, 0f, 0f, 0f, AudioPlayer.AUDIO_RANK_NOTIFICATION, AudioPlayer.AUDIO_DISTANCE_NOTIFICATION, .25f, 1f, 1f, false, true));
    }

    public void remove() {
        arrow.remove();
        timer.stop();
    }

    @Override
    public void update(Object anim) {
        remove();
        manager.removeNotification(this);
    }

    protected final @NonNull Arrow getArrow() {
        return arrow;
    }

    protected final @NonNull TimerAnimation getTimer() {
        return timer;
    }

    protected final NotificationManager getManager() {
        return manager;
    }

    public final float getX() {
        return center_x;
    }

    public final float getY() {
        return center_y;
    }
}
