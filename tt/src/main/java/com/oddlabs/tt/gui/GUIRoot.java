package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.delegate.CameraDelegate;
import com.oddlabs.tt.delegate.ModalDelegate;
import com.oddlabs.tt.delegate.NullDelegate;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.Status;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.PointerInput;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.util.Utils;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/** Root of a GUI component tree */
public final class GUIRoot extends GUIObject {
	private static final Logger logger = Logger.getLogger(GUIRoot.class.getName());

	private final ResourceBundle bundle = ResourceBundle.getBundle(GUIRoot.class.getName());

	private static final int CURSOR_OFFSET_Y = 27;
	private final com.oddlabs.tt.resource.Cursor[] cursors = new com.oddlabs.tt.resource.Cursor[]{
		new com.oddlabs.tt.resource.Cursor(Utils.makeURL("/textures/gui/pointer_16_1.image"), 1, 15,
										   Utils.makeURL("/textures/gui/pointer_32_1.image"), 2, 29,
										   Utils.makeURL("/textures/gui/pointer_32_8.image"), 2, 29),

		new com.oddlabs.tt.resource.Cursor(Utils.makeURL("/textures/gui/pointer_target_16_1.image"), 7, 8,
										   Utils.makeURL("/textures/gui/pointer_target_32_1.image"), 14, 17,
										   Utils.makeURL("/textures/gui/pointer_target_32_8.image"), 14, 17),

		new com.oddlabs.tt.resource.Cursor(Utils.makeURL("/textures/gui/pointer_text_16_1.image"), 4, 8,
										   Utils.makeURL("/textures/gui/pointer_text_32_1.image"), 6, 20,
										   Utils.makeURL("/textures/gui/pointer_text_32_8.image"), 6, 20)};

	private final Deque<@NonNull CameraDelegate<?>> delegate_stack = new ArrayDeque<>();
	private final Deque<@NonNull ModalDelegate> modal_delegate_stack = new ArrayDeque<>();
	private final Deque<@NonNull GUIObject> focus_backup_stack = new ArrayDeque<>();

    private final TimerAnimation tool_tip_timer = new TimerAnimation(this::timerUpdate, 0);

    private final @NonNull GUI gui;
	private final @NonNull ToolTipBox tool_tip = new ToolTipBox();
	private final @NonNull InfoPrinter info_printer = new InfoPrinter(this, 4, Skin.getSkin().getEditFont());
	private final Status status = new Status();
	private final InputState input_state = new InputState(this);
	private boolean render_tool_tip = false;

	private @NonNull GUIObject current_gui_object = this;
	private @NonNull GUIObject global_focus = this;

	private @NonNull GUIObject cursor_object = this;

	GUIRoot(@NonNull GUI gui) {
		this.gui = gui;
		setPos(0, 0);
		setCanFocus(true);
		setFocusCycle(true);

		setToolTipTimer();
		addChild(info_printer);
		info_printer.setPos(0, 0);
		pushDelegate(new NullDelegate(this, false));
	}

    public @NonNull GUIRoot self() {
        return this;
    }

    public @NonNull GUIRoot getParentGUIRoot() {
        return self();
    }

    public @NonNull GUI getGUI() {
		return gui;
	}

	@NonNull InputState getInputState() {
		return input_state;
	}

	@NonNull GUIObject getGlobalFocus() {
		return global_focus;
	}

	void setGlobalFocus(@NonNull GUIObject object) {
		global_focus = object;
	}

	public void setToolTipTimer() {
		tool_tip_timer.setTimerInterval(Settings.getSettings().tooltip_delay*ToolTipBox.MAX_DELAY_SECONDS);
	}

	public void timerUpdate(@NonNull TimerAnimation anim) {
		render_tool_tip = true;
		tool_tip_timer.stop();
	}

	public @NonNull InfoPrinter getInfoPrinter() {
		return info_printer;
	}

	public void pushDelegate(@NonNull CameraDelegate<?> delegate) {
		if (!delegate_stack.isEmpty()) {
			getDelegate().remove();
		}
		assert !delegate_stack.contains(delegate);
		delegate_stack.push(delegate);
		addChild(delegate);
		mousePick();
	}

	public void removeDelegate(@NonNull CameraDelegate<?> delegate) {
		boolean top_most = getDelegate() == delegate;
		delegate.remove();

		delegate_stack.remove(delegate);

		if (delegate_stack.isEmpty()) {
			pushDelegate(new NullDelegate(this, false));
		} else if (top_most) {
			addChild(getDelegate());
		}
		mousePick();
	}

	public @NonNull CameraDelegate<?> getDelegate() {
		return delegate_stack.element();
	}

	private void pushModalDelegate(@NonNull ModalDelegate delegate) {
		if (!modal_delegate_stack.isEmpty()) {
			getModalDelegate().remove();
		}
		modal_delegate_stack.push(delegate);
		super.addChild(delegate);
		mousePick();
	}

	private void popModalDelegate(@NonNull ModalDelegate delegate) {
		if (!modal_delegate_stack.contains(delegate)) {
			return;
		}
		boolean top_most = getModalDelegate() == delegate;
		modal_delegate_stack.remove(delegate);
		delegate.remove();

		ModalDelegate modal_delegate = getModalDelegate();
		if (top_most && modal_delegate != null)
			super.addChild(modal_delegate);

		GUIObject object = focus_backup_stack.pop();
		if (!delegate_stack.isEmpty())
			getDelegate().setFocus();
		if (top_most && object != null)
			object.setFocus();
		mousePick();
	}

	public @Nullable ModalDelegate getModalDelegate() {
		return modal_delegate_stack.peek();
	}

	public void addModalForm(@NonNull Form form) {
		focus_backup_stack.push(global_focus);
		ModalDelegate delegate = new ModalDelegate();
		delegate.addChild(form);
		form.addCloseListener(() -> popModalDelegate(delegate));
		pushModalDelegate(delegate);
		form.setFocus();
	}

	void swapFocusBackup(@NonNull GUIObject o) {
		focus_backup_stack.pop();
		focus_backup_stack.push(o);
	}

	@Override
	protected void displayChangedNotify(int width, int height) {
		setDim(width, height);
		getDelegate().displayChanged(width, height);
	}

	@Override
	protected void keyPressed(@NonNull KeyboardEvent event) {
		switch (event.keyCode()) {
			case S:
				if (event.controlDown()) {
					String filename = GLUtils.takeScreenshot("");
					info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "screenshot_message", filename));
				}
				break;

			case H:
				if (event.controlDown() && (Renderer.getLocalInput().getNativeCursorCaps() & LocalInput.CURSOR_ONE_BIT_TRANSPARENCY) != 0) {
					Settings.getSettings().use_native_cursor = !Settings.getSettings().use_native_cursor;
					if (Settings.getSettings().use_native_cursor)
						info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "hardware_cursor_on"));
					else
						info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "hardware_cursor_off"));
				}
				break;

			case A:
				if (event.controlDown()) {
					Settings.getSettings().aggressive_units = !Settings.getSettings().aggressive_units;
					if (Settings.getSettings().aggressive_units)
						info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "aggressive_unites_on"));
					else
						info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "aggressive_unites_off"));
				}
				break;

			 case I:
				if (event.controlDown()) {
					Globals.draw_status = !Globals.draw_status;
				}
				break;
			default:
				break;
		}

		if (!Settings.getSettings().inDeveloperMode())
			return;

		switch (event.keyCode()) {
			case U:
				Renderer.getRenderer().startMovieRecording();
				break;
			case W:
				if (event.controlDown())
					Globals.draw_water = !Globals.draw_water;
				break;
			case R:
				if (event.controlDown()) {
					Globals.run_ai = !Globals.run_ai;
					IO.println("Globals.run_ai = " + Globals.run_ai);
				}
				break;

			case O:
				Globals.draw_light = !Globals.draw_light;
				break;
			case P:
				if (event.controlDown())
					GLUtils.takeScreenshot("");
				else
					Globals.draw_plants = !Globals.draw_plants;
				break;
			case E:
				Globals.draw_particles = !Globals.draw_particles;
				break;
			case A:
				if (!event.controlDown()) {
					Globals.draw_axes = !Globals.draw_axes;
				}
				break;
			case M:
				if (event.controlDown())
					Globals.draw_misc = !Globals.draw_misc;
				else {
					IO.println("WARNING: KEY_M pressed!");
					Globals.process_misc = !Globals.process_misc;
				}
				break;
			case J:
				Renderer.getLocalInput().getInputProvider().setCursorPosition(10, 10);
				break;
			case S:
				if (!event.controlDown()) {
					Globals.draw_detail = !Globals.draw_detail;
				}
				break;
			case C:
				if (event.controlDown()) {
					IO.println("crash!");
					throw new RuntimeException("Ctrl+C pressed -> throwing a runtime exception.");
				} else {
					Globals.clear_frame_buffer = !Globals.clear_frame_buffer;
				}
				break;
			case D:
				Globals.switchBoundingMode();
				break;
			case V:
				Globals.frustum_freeze = !Globals.frustum_freeze;
				IO.println("Globals.frustum_freeze = " + Globals.frustum_freeze);
				break;
			case F1:
				IO.println("*********************************************************");
				LocalEventQueue.getQueue().debugPrintAnimations();
				IO.println("Texture.globalSize() = " + Texture.globalSize());
				break;
			case F11:
				Renderer.getRenderer().toggleFullscreen();
				break;
			case F12:
				IO.println("GC Forced");
                System.gc();
				break;
			default:
				break;
		}
	}

	void mousePick() {
		mousePick(Renderer.getLocalInput().getMouseX(), Renderer.getLocalInput().getMouseY());
	}

	private void mousePick(int x, int y) {
		GUIObject target = pick(x ,y);
		if (target != null && target != current_gui_object) {
			current_gui_object.mouseExitedAll();
			tool_tip_timer.resetTime();
			boolean old_tip = current_gui_object instanceof ToolTip;
			boolean new_tip = target instanceof ToolTip;
			if (!old_tip && new_tip) {
				tool_tip_timer.start();
				render_tool_tip = false;
			}
			if (old_tip && !new_tip) {
				if (!render_tool_tip)
					tool_tip_timer.stop();
				else
					render_tool_tip = false;
			}
			current_gui_object = target;
			current_gui_object.mouseEnteredAll();
			if (!current_gui_object.isDisabled())
				cursor_object = current_gui_object;
		}
	}

	@NonNull GUIObject getCurrentGUIObject() {
		return current_gui_object;
	}

	@Override
	public void addChild(@NonNull GUIObject child) {
		super.addChild(child);
		ModalDelegate modal_delegate = getModalDelegate();
		putFirst(info_printer);
		if (modal_delegate != null) {
			super.addChild(modal_delegate); // move to front
		}
	}

	private boolean showToolTip() {
		return getModalDelegate() != null || getDelegate().renderCursor();
	}

	void renderTopmost(@NonNull GUIRenderer renderer, @Nullable ToolTip hovered, boolean cheater) {
        if (cheater) {
            renderer.drawIcon(GUIIcons.getIcons().getCheatIcon(),
                    getWidth() - GUIIcons.getIcons().getCheatIcon().getWidth() - 10,
                    5);
        }

        getDelegate().render2D(renderer);

		// render forced delegates
        boolean initial = true; // Skip the first element which is the current delegate
        for (CameraDelegate<?> delegate : delegate_stack) {
            if (initial) {
                initial = false;
            } else if (delegate.forceRender()) {
                delegate.render(renderer);
            }
        }

		if (Globals.draw_status) {
			status.render(renderer);
		}

		if (gui.getFade() != null) {
			gui.getFade().render(renderer);
		}

		if (cursor_object.getCursorType() != CursorType.NULL) {
			cursors[cursor_object.getCursorType().ordinal()].setActive();
			if (getModalDelegate() != null || getDelegate().renderCursor()) {
				float mouse_x = Renderer.getLocalInput().getMouseX();
				float mouse_y = Renderer.getLocalInput().getMouseY();
				cursors[cursor_object.getCursorType().ordinal()].render(renderer, mouse_x, mouse_y);
			}
		} else
			PointerInput.setActiveCursor(MemoryUtil.NULL);

        if (showToolTip()) {
            ToolTip tooltip = getToolTip();
            if (tooltip == null)
                tooltip = hovered;
            if (tooltip != null)
                renderToolTip(renderer, tooltip);
        }
    }

	public Matrix4f multProjection(@NonNull Matrix4f matrix) {
		float fovy = Globals.FOV;
		float zNear = Globals.VIEW_MIN;
		float zFar = Globals.VIEW_MAX;

		Matrix4f perspectiveMatrix = new Matrix4f().perspective((float)Math.toRadians(fovy), (float) getWidth() / getHeight(), zNear, zFar);
		return matrix.mul(perspectiveMatrix);
	}

	private @Nullable ToolTip getToolTip() {
        return render_tool_tip && getCurrentGUIObject() instanceof ToolTip tip ? tip : null;
	}

	private void renderToolTip(@NonNull GUIRenderer renderer, @NonNull ToolTip hovered) {
		tool_tip.clear();
		hovered.appendToolTip(tool_tip);
		tool_tip.render(renderer, Renderer.getLocalInput().getMouseX(), Renderer.getLocalInput().getMouseY() - CURSOR_OFFSET_Y, getWidth(), getHeight());
	}
}
