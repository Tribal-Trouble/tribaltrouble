package com.oddlabs.tt.viewer;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

	public void newAttackNotification(@NonNull AnimationManager manager, @NonNull Selectable target, @NonNull Player local_player) {
		for (AttackNotification current : attack_notifies) {
			if (current.contains(target)) {
				current.restartTimer();
				return;
			}
		}
		addNotification(new AttackNotification(local_player, gui_root, target, this, manager), attack_notifies);
	}

	public void newSelectableNotification(@NonNull Selectable s, @NonNull AnimationManager manager, @NonNull Player local_player) {
		newNotification(manager, local_player, s.getPositionX(), s.getPositionY(), 0f, 1f, 0f, false);
	}

	public void newBeacon(@NonNull AnimationManager manager, @NonNull Player local_player, float x, float y) {
		newNotification(manager, local_player, x, y, 0f, 0f, 1f, true);
	}

	private void newNotification(@NonNull AnimationManager manager, @NonNull Player local_player, float x, float y, float r, float g, float b, boolean show_always) {
		addNotification(new Notification(local_player.getWorld(), gui_root, x, y, this, r, g, b, local_player.getRace().getBuildingNotificationAudio(), show_always, manager), notifies);
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
