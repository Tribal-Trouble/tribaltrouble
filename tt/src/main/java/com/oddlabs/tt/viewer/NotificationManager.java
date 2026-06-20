package com.oddlabs.tt.viewer;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the creation and lifecycle of all in-game notifications, such as attack warnings and beacon alerts.
 */
public final class NotificationManager {
    private final List<@NonNull AttackNotification> attack_notifies = new ArrayList<>();
    private final List<@NonNull Notification> notifies = new ArrayList<>();
    private final GUIRoot gui_root;
    private @Nullable Notification latest_notification = null;

    public NotificationManager(GUIRoot gui_root) {
        this.gui_root = gui_root;
    }

    public @Nullable Notification getLatestNotification() {
        return latest_notification;
    }

    public void newAttackNotification(@NonNull AnimationManager manager, @NonNull Selectable<?> target,
            @NonNull Player local_player) {
        for (AttackNotification current : attack_notifies) {
            if (current.contains(target)) {
                current.restartTimer();
                return;
            }
        }
        addNotification(new AttackNotification(local_player, gui_root, target, this, manager), attack_notifies);
    }

    public void newSelectableNotification(@NonNull Selectable<?> s, @NonNull AnimationManager manager,
            @NonNull Player local_player) {
        newNotification(manager, local_player, s.getPositionX(), s.getPositionY(), Color.Standard.GREEN, false);
    }

    public void newBeacon(@NonNull AnimationManager manager, @NonNull Player local_player, float x, float y) {
        newNotification(manager, local_player, x, y, Color.Standard.BLUE, true);
    }

    private void newNotification(@NonNull AnimationManager manager, @NonNull Player local_player, float x, float y,
            @NonNull Color color, boolean show_always) {
        var audio = local_player.getRaceInfo().getBuildingNotificationAudio();
        var notification = new Notification(local_player.getWorld(), gui_root, x, y, this, color, audio, show_always,
                manager);
        addNotification(notification, notifies);
    }

    private <N extends @NonNull Notification> void addNotification(N notification, @NonNull List<N> list) {
        list.add(notification);
        latest_notification = notification;
    }

    void removeAttackNotification(@NonNull AttackNotification current) {
        attack_notifies.remove(current);
    }

    public void removeNotification(@NonNull Notification current) {
        notifies.remove(current);
    }
}
