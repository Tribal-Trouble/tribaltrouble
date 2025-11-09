package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;

public abstract class IconSpinner extends GUIObject implements ToolTip {
	private final IconQuad @NonNull [] icon_quad;
	private final String tool_tip;
	private final Quad[] tool_tip_icons;
	private final @NonNull TextField label;
	private final @NonNull GUIObject button_plus;
	private final @NonNull GUIObject button_minus;
	private final WorldViewer viewer;
	private @Nullable IconDisabler icon_disabler = null;

	private int text_count = 0;

	public IconSpinner(WorldViewer viewer, IconQuad @NonNull [] icon_quad, String tool_tip, Quad[] tool_tip_icons, String shortcut_key) {
		this.icon_quad = icon_quad;
		this.tool_tip = tool_tip;
		this.tool_tip_icons = tool_tip_icons;
		this.viewer = viewer;
		ResourceBundle bundle = ResourceBundle.getBundle(IconSpinner.class.getName());
		setCanFocus(true);
		setDim(icon_quad[0].getWidth(), icon_quad[0].getHeight());

		String inc_str = Utils.getBundleString(bundle, "increase", shortcut_key);
		button_plus = new IconSpinnerButton(Skin.getSkin().getPlusButton(), inc_str, this);
		addChild(button_plus);
		button_plus.setPos(0, 0);
		button_plus.addMouseButtonListener(new IncreaseListener());

		String dec_str = Utils.getBundleString(bundle, "decrease", shortcut_key);
		button_minus = new IconSpinnerButton(Skin.getSkin().getMinusButton(), dec_str, this);
		addChild(button_minus);
		button_minus.setPos(button_plus.getWidth(), 0);
		button_minus.addMouseButtonListener(new DecreaseListener());

		label = new Label("", Skin.getSkin().getHeadlineFont(), icon_quad[0].getWidth(), Label.ALIGN_CENTER);
		addChild(label);
		label.setPos(0, (getHeight() - label.getHeight())/2);
	}

	@Override
	public final void setFocus() {
		viewer.getGUIRoot().getDelegate().setFocus();
	}

	public final void setIconDisabler(IconDisabler icon_disabler) {
		this.icon_disabler = icon_disabler;
	}

	public final void doUpdate() {
		setCount();
		if (icon_disabler != null) {
			setDisabled(computeCount() == 0 && getOrderSize() == 0 && icon_disabler.isDisabled());
		}
	}

	public abstract int computeCount();
	protected abstract void increase(int amount);
	protected abstract void decrease(int amount);
	protected abstract void release();
	protected abstract int getOrderSize();
	protected abstract boolean renderInfinite();
	protected abstract float getProgress();

	private void setCount() {
		int count = computeCount();
		if (count != text_count) {
			text_count = count;
			label.clear();
			if (text_count != 0 && !renderInfinite()) {
				label.append(text_count);
			}
		}
	}

	@Override
	public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
		tool_tip_box.append(tool_tip);
		tool_tip_box.append(tool_tip_icons);
	}

	public final void shortcutPressed(boolean shift_down, boolean ctrl_down) {
		if (!isDisabled()) {
				int mouse_button;
				if (ctrl_down)
					mouse_button = LocalInput.RIGHT_BUTTON;
				else
					mouse_button = LocalInput.LEFT_BUTTON;

				if (shift_down)
					button_minus.mousePressedAll(mouse_button, 0, 0);
				else
					button_plus.mousePressedAll(mouse_button, 0, 0);
		}
	}

	public final void shortcutReleased(boolean shift_down, boolean ctrl_down) {
		if (!isDisabled()) {
        		release();
		}
	}

	@Override
	protected final void renderGeometry() {
		int x = (getWidth() - icon_quad[Skin.NORMAL].getWidth())/2;
		int y = (getHeight() - icon_quad[Skin.NORMAL].getHeight())/2;
		if (isDisabled())
			icon_quad[Skin.DISABLED].render(x, y);
		else if (isHovered())
			icon_quad[Skin.ACTIVE].render(x, y);
		else
			icon_quad[Skin.NORMAL].render(x, y);

		if (text_count > 0) {
			IconQuad[] watch = Icons.getIcons().getWatch();
			int index = (int)(getProgress()*(watch.length - 1));
			watch[index].render(getWidth() - watch[index].getWidth(),  getHeight() - watch[index].getHeight());
		}
	}

	@Override
	protected final void mouseReleased(int button, int x, int y) {
	}

	@Override
	protected final void mousePressed(int button, int x, int y) {
	}

	@Override
	protected final void mouseClicked(int button, int x, int y, int clicks) {
	}

	@Override
	protected final void mouseHeld(int button, int x, int y) {
	}

	private final class IncreaseListener implements MouseButtonListener {
		@Override
		public void mouseClicked(int button, int x, int y, int clicks) {
		}

		@Override
		public void mouseHeld(int button, int x, int y) {
			mousePressed(button, x, y);
		}

		@Override
		public void mousePressed(int button, int x, int y) {
			if (button == LocalInput.RIGHT_BUTTON)
					increase(10);
				else
					increase(1);
		}

		@Override
		public void mouseReleased(int button, int x, int y) {
			release();
		}
	}

	private final class DecreaseListener implements MouseButtonListener {
		@Override
		public void mouseClicked(int button, int x, int y, int clicks) {
		}

		@Override
		public void mouseHeld(int button, int x, int y) {
			mousePressed(button, x, y);
		}

		@Override
		public void mousePressed(int button, int x, int y) {
			if (button == LocalInput.RIGHT_BUTTON)
					decrease(10);
				else
					decrease(1);
		}

		@Override
		public void mouseReleased(int button, int x, int y) {
			release();
		}
	}
}
