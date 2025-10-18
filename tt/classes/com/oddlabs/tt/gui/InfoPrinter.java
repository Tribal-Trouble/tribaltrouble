package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.StateChecksum;
import java.util.ArrayList;

public final class InfoPrinter extends GUIObject implements Animated, ChatListener {
	private final static float SECONDS_PER_TIMEOUT = 8f;
	private final static float[] PRIVATE_COLOR = new float[]{1f, .2f, .4f, 1f};
	private final static float[] TEAM_COLOR = new float[]{.3f, .5f, 1f, 1f};

	private final Font font;
	private final ArrayList history = new ArrayList();
	private final ArrayList timers = new ArrayList();
	private final int lines;
	private final GUIRoot gui_root;

	private float time;

	public InfoPrinter(GUIRoot gui_root, int lines, Font font) {
		this.gui_root = gui_root;
		this.lines = lines;
		this.font = font;
		displayChangedNotify(LocalInput.getViewWidth(), LocalInput.getViewHeight());
		LocalEventQueue.getQueue().getManager().registerAnimation(this);
		time = 0;
	}

	public GUIRoot getGUIRoot() {
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
	public void chat(ChatMessage message) {
		chat(message.formatShort(), message.type);
	}

	public void chat(String text, ChatMessage.Type type) {
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

	public void print(String text) {
		print(text, null);
	}

	public void print(String text, float[] color) {
		int width = StrictMath.min(font.getWidth(text), getWidth());
		LabelBox label_box = new BackgroundLabelBox(text, font, width);
		if (color != null)
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
		LabelBox label_box = (LabelBox)history.get(index);
		label_box.remove();
		history.remove(index);
		timers.remove(index);
		setLabelsPos();
	}

        @Override
	public void animate(float t) {
		time += t;
		for (int i = timers.size() - 1; i >= 0; i--) {
			float remove_time = ((Float)timers.get(i));
			if (time > remove_time) {
				removeLine(i);
			}
		}
	}

        @Override
	public void updateChecksum(StateChecksum checksum) {
	}

        @Override
	protected void renderGeometry() {
	}

	private void setLabelsPos() {
		int y = getHeight();
		for (int i = 0; i < history.size(); i++) {
			LabelBox label_box = (LabelBox)history.get(i);
			y -= label_box.getHeight();
			label_box.setPos(0, y);
		}
	}
}

