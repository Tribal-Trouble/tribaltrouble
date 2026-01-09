package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.Network;
import com.oddlabs.util.Color;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class InfoPrinter extends GUIObject implements Animated, ChatListener {
	private static final float SECONDS_PER_TIMEOUT = 8f;
	private static final Vector4fc PRIVATE_COLOR = Color.argb4v(0xFF_33_66_FF);
	private static final Vector4fc TEAM_COLOR = Color.argb4v(0xFF_4C_7F_FF);

	private final @NonNull Font font;
	private final List<@NonNull LabelBox> history = new ArrayList<>();
	private final List<Float> timers = new ArrayList<>();
	private final int lines;
	private final @NonNull GUIRoot gui_root;

	private float time;

	public InfoPrinter(@NonNull GUIRoot gui_root, int lines, @NonNull Font font) {
		this.gui_root = gui_root;
		this.lines = lines;
		this.font = font;
		displayChangedNotify(gui_root.getWidth(), gui_root.getHeight());
		LocalEventQueue.getQueue().getManager().registerAnimation(this);
		time = 0;
	}

	public @NonNull GUIRoot getGUIRoot() {
		return gui_root;
	}

	@Override
	protected void doAdd() {
		super.doAdd();
		Network.getChatHub().addListener(this);
	}

	@Override
	protected void doRemove() {
		super.doRemove();
		Network.getChatHub().removeListener(this);
		LocalEventQueue.getQueue().getManager().removeAnimation(this);
	}

	@Override
	protected void displayChangedNotify(int width, int height) {
		setDim(width, height);
	}

	@Override
	public void chat(@NonNull ChatMessage message) {
		chat(message.formatShort(), message.type);
	}

	public void chat(@NonNull String text, ChatMessage.@NonNull Type type) {
		switch (type) {
			case NORMAL:
				print(text);
				break;
			case TEAM:
				print(text, TEAM_COLOR);
				break;
			case PRIVATE:
				print(text, PRIVATE_COLOR);
				break;
			default:
				break;
		}
	}

	public void print(@NonNull String text) {
		print(text, Color.TRANSPARENT);
	}

	public void print(@NonNull String text, @NonNull Vector4fc color) {
		int width = Math.min(font.getWidth(text), getWidth());
		LabelBox label_box = new BackgroundLabelBox(text, font, width);
		if (color.w() > .2f)
			label_box.setColor(color);
		addChild(label_box);
		history.add(label_box);
		timers.add(time + SECONDS_PER_TIMEOUT);

		while (history.size() > lines) {
			removeLine(0);
		}
		setLabelsPos();
	}

	private void removeLine(int index) {
		LabelBox label_box = history.get(index);
		label_box.remove();
		history.remove(index);
		timers.remove(index);
		setLabelsPos();
	}

	@Override
	public void animate(float t) {
		time += t;
		for (int i = timers.size() - 1; i >= 0; i--) {
			float remove_time = timers.get(i);
			if (time > remove_time) {
				removeLine(i);
			}
		}
	}

    private void setLabelsPos() {
		int y = getHeight();
        for (LabelBox label_box : history) {
            y -= label_box.getHeight();
            label_box.setPos(0, y);
        }
	}
}
