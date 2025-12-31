package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;

public abstract class IconSpinner extends GUIObject implements ToolTip {
	private final @NonNull ModeIconQuads icon_quad;
	private final @NonNull String tool_tip;
	private final @NonNull IconQuad @Nullable [] tool_tip_icons;
	private final @NonNull TextField label;
	private final @NonNull GUIObject button_plus;
	private final @NonNull GUIObject button_minus;
	private final @NonNull WorldViewer viewer;
	private @Nullable IconDisabler icon_disabler = null;

	private int text_count = 0;

	public IconSpinner(@NonNull WorldViewer viewer, @NonNull ModeIconQuads icon_quad, @NonNull String tool_tip, @NonNull IconQuad @Nullable [] tool_tip_icons, @NonNull String shortcut_key) {
		this.icon_quad = icon_quad;
		this.tool_tip = tool_tip;
		this.tool_tip_icons = tool_tip_icons;
		this.viewer = viewer;
		ResourceBundle bundle = ResourceBundle.getBundle(IconSpinner.class.getName());
		setCanFocus(true);
		setDim(icon_quad.quad(ModeIconQuads.Mode.NORMAL).getWidth(), icon_quad.quad(ModeIconQuads.Mode.NORMAL).getHeight());

		String inc_str = Utils.getBundleString(bundle, "increase", shortcut_key);
		button_plus = new IconSpinnerButton(Skin.getSkin().getPlusButton(), inc_str, this);
		button_plus.setPos(0, 0);
		button_plus.addMouseButtonListener(new IncreaseListener());
        addChild(button_plus);

		String dec_str = Utils.getBundleString(bundle, "decrease", shortcut_key);
		button_minus = new IconSpinnerButton(Skin.getSkin().getMinusButton(), dec_str, this);
		button_minus.setPos(button_plus.getWidth(), 0);
		button_minus.addMouseButtonListener(new DecreaseListener());
        addChild(button_minus);

		label = new Label("", Skin.getSkin().getHeadlineFont(), icon_quad.quad(ModeIconQuads.Mode.NORMAL).getWidth(), Origin.AT_MIDDLE);
		label.setPos(0, (getHeight() - label.getHeight())/2);
        addChild(label);
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
	public abstract boolean renderInfinite();
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
			MouseButton mouse_button = ctrl_down ? MouseButton.RIGHT : MouseButton.LEFT;

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
	protected final void renderGeometry(@NonNull GUIRenderer renderer) {
		int x = (getWidth() - icon_quad.quad(ModeIconQuads.Mode.NORMAL).getWidth())/2;
		int y = (getHeight() - icon_quad.quad(ModeIconQuads.Mode.NORMAL).getHeight())/2;

		ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isHovered()
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;

		renderer.drawIcon(icon_quad.quad(skinMode), x, y);

		if (text_count > 0) {
			IconQuad[] watch = GUIIcons.getIcons().getWatch();
			int index = (int)(getProgress()*(watch.length - 1));
			IconQuad watchQuad = watch[index];
			renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
		}
	}

	@Override
	protected final void mouseReleased (@NonNull MouseButton button, int x, int y) {
	}

	@Override
	protected final void mousePressed (@NonNull MouseButton button, int x, int y) {
	}

	@Override
	protected final void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
	}

	@Override
	protected final void mouseHeld (@NonNull MouseButton button, int x, int y) {
	}

	private final class IncreaseListener implements MouseButtonListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
		}

		@Override
		public void mouseHeld(@NonNull MouseButton button, int x, int y) {
			mousePressed(button, x, y);
		}

		@Override
		public void mousePressed(@NonNull MouseButton button, int x, int y) {
            increase(button == MouseButton.RIGHT ? 10 : 1);
		}

		@Override
		public void mouseReleased(@NonNull MouseButton button, int x, int y) {
			release();
		}
	}

	private final class DecreaseListener implements MouseButtonListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
		}

		@Override
		public void mouseHeld(@NonNull MouseButton button, int x, int y) {
			mousePressed(button, x, y);
		}

		@Override
		public void mousePressed(@NonNull MouseButton button, int x, int y) {
            decrease(button == MouseButton.RIGHT ? 10 : 1);
		}

		@Override
		public void mouseReleased(@NonNull MouseButton button, int x, int y) {
			release();
		}
	}
}
