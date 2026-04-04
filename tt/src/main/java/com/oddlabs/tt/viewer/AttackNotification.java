package com.oddlabs.tt.viewer;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

final class AttackNotification extends Notification {
    private static final float RADIUS = 30f;
    private static final float FADE_OUT = 5f;

    private boolean active = true;

    public AttackNotification(@NonNull Player local_player, @NonNull GUIRoot gui_root, @NonNull Selectable<?> center, @NonNull NotificationManager manager, @NonNull AnimationManager animation_manager) {
        super(local_player.getWorld(), gui_root, center.getPositionX(), center.getPositionY(), manager, 1f, 0f, 0f, local_player.getRace().getAttackNotificationAudio(), false, animation_manager);
    }

    public boolean contains(@NonNull Target target) {
        float dx = getX() - target.getPositionX();
        float dy = getY() - target.getPositionY();
        float dist = dx * dx + dy * dy;
        return dist <= RADIUS * RADIUS;
    }

    public void restartTimer() {
        if (!active)
            getTimer().resetTime();
    }

    @Override
    public void update(@NonNull TimerAnimation anim) {
        if (active) {
            active = false;
            getArrow().remove();
            getTimer().setTimerInterval(FADE_OUT);
        } else {
            getTimer().stop();
            getManager().removeAttackNotification(this);
        }
    }
}
