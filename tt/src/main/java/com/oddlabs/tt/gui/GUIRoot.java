package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.delegate.CameraDelegate;
import com.oddlabs.tt.delegate.ModalDelegate;
import com.oddlabs.tt.delegate.NullDelegate;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.QuitForm;
import com.oddlabs.tt.form.Status;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.util.ToolTip;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/** Root of a GUI component tree */
public final class GUIRoot extends GUIObject {
	private static final Logger logger = Logger.getLogger(GUIRoot.class.getName());

	private static final ResourceBundle bundle = ResourceBundle.getBundle(GUIRoot.class.getName());

	private static final int CURSOR_OFFSET_Y = 27;

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

    private float effective_scale = 1.0f;

	GUIRoot(@NonNull GUI gui) {
		this.gui = gui;
		setPos(0, 0);
		setCanFocus(true);
		setFocusCycle(true);

		setToolTipTimer();
		addChild(info_printer);
		info_printer.setPos(0, 0);
		pushDelegate(new NullDelegate(this, false));
		Renderer.getLocalInput().getPointerInput().setActiveCursor(current_gui_object.getCursorType());
	}

    public @NonNull GUIRoot self() {
        return this;
    }

    public float getGlobalScale() {
        return effective_scale;
    }

	public @NonNull GUIRoot getParentGUIRoot() {
        return self();
    }

    public @NonNull GUI getGUI() {
		return gui;
	}

	public @NonNull InputState getInputState() {
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
	}

	public void stopToolTip() {
		render_tool_tip = false;
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

    public boolean isShowingQuitForm() {
        ModalDelegate modal = getModalDelegate();
        if (modal == null) return false;
        GUIObject child = modal.getFirstChild();
        while (child != null) {
            if (child instanceof QuitForm) return true;
            child = child.getNext();
        }
        return false;
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

    public static float calculateMaxScale(int width, int height) {
        if (width <= 0 || height <= 0) return 1.0f;
        float maxScaleX = width / 800f;
        float maxScaleY = height / 600f;
        return Math.max(1.0f, Math.min(maxScaleX, maxScaleY));
    }

    public static float calculateMinScale(int width, int height) {
        if (width <= 0 || height <= 0) return 1.0f;
        float autoScale = Math.min(width / 1280f, height / 1024f);
        return Math.max(1.0f, autoScale);
    }

    public static float calculateEffectiveScale(int width, int height) {
        if (width <= 0 || height <= 0) return 1.0f;

        float minScale = calculateMinScale(width, height);
		float maxAllowedScale = Math.max(minScale, calculateMaxScale(width, height));

        float current = Math.clamp(Settings.getSettings().ui_scale, 0f, 1f);
        
        // Interpolate
        float rawTarget = minScale + (current * (maxAllowedScale - minScale));
        
        // Snap to nearest 0.25 increment
        float snappedTarget = Math.round(rawTarget * 4f) / 4f;
        
        return Math.clamp(snappedTarget, minScale, maxAllowedScale);
    }

	@Override
	protected void displayChangedNotify(int width, int height) {
        if (width <= 0 || height <= 0) return;

        effective_scale = calculateEffectiveScale(width, height);

        int virtualWidth = (int)(width / effective_scale);
        int virtualHeight = (int)(height / effective_scale);

		setDim(virtualWidth, virtualHeight);
	}

	@Override
	public void handleInput(@NonNull InputEvent event) {
		if (event.getPhase() == InputPhase.PRESSED) {
			boolean consumed = false;

			if (event.hasActions()) {
				if (event.consumeAction(GameAction.GLOBAL_SCREENSHOT)) {
					String filename = GLUtils.takeScreenshot("");
					info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "screenshot_message", filename));
					consumed = true;
				}
				if (event.consumeAction(GameAction.GLOBAL_AGGRESSIVE_UNITS)) {
					Settings.getSettings().aggressive_units = !Settings.getSettings().aggressive_units;
					if (Settings.getSettings().aggressive_units)
						info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "aggressive_unites_on"));
					else
						info_printer.print(com.oddlabs.tt.util.Utils.getBundleString(bundle, "aggressive_unites_off"));
					consumed = true;
				}
				if (event.consumeAction(GameAction.GLOBAL_TOGGLE_STATUS)) {
					Globals.draw_status = !Globals.draw_status;
					consumed = true;
				}
				// GLOBAL_MENU removed because it requires viewer which GUIRoot doesn't have.
                
				if (event.consumeAction(GameAction.GLOBAL_TOGGLE_FULLSCREEN)) {
					Renderer.getRenderer().toggleFullscreen();
					consumed = true;
				}

				// Debug Actions (Only those that don't need Viewer)
				if (Settings.getSettings().inDeveloperMode()) {
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_LIGHT)) {
						Globals.draw_light = !Globals.draw_light;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_PLANTS)) {
						Globals.draw_plants = !Globals.draw_plants;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_PARTICLES)) {
						Globals.draw_particles = !Globals.draw_particles;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_AXES)) {
						Globals.draw_axes = !Globals.draw_axes;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_MISC)) {
						Globals.draw_misc = !Globals.draw_misc;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_PROCESS_MISC)) {
						logger.info("WARNING: DEBUG_PROCESS_MISC triggered!");
						Globals.process_misc = !Globals.process_misc;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_RESET_CURSOR)) {
						Renderer.getLocalInput().getInputProvider().setCursorPosition(10, 10);
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_DETAIL)) {
						Globals.draw_detail = !Globals.draw_detail;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_CRASH)) {
						logger.info("crash!");
						throw new RuntimeException("Debug crash action triggered.");
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_FRAME_BUFFER)) {
						Globals.clear_frame_buffer = !Globals.clear_frame_buffer;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_BOUNDING)) {
						Globals.switchBoundingMode();
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_FRUSTUM_FREEZE)) {
						Globals.frustum_freeze = !Globals.frustum_freeze;
						logger.info("Globals.frustum_freeze = " + Globals.frustum_freeze);
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_FORCE_GC)) {
						logger.info("GC Forced");
						System.gc();
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_START_RECORDING)) {
						Renderer.getRenderer().startMovieRecording();
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_WATER)) {
						Globals.draw_water = !Globals.draw_water;
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_TOGGLE_AI)) {
						Globals.run_ai = !Globals.run_ai;
						logger.info("Globals.run_ai = " + Globals.run_ai);
						consumed = true;
					}
					if (event.consumeAction(GameAction.DEBUG_DUMP_ANIMATIONS)) {
						logger.info("*********************************************************");
						LocalEventQueue.getQueue().debugPrintAnimations();
						logger.info("Texture.globalSize() = " + Texture.globalSize());
						consumed = true;
					}
				}
			}
			
			if (consumed) {
				event.consume();
				return;
			}
		}
		super.handleInput(event);
	}

	void mousePick() {
		mousePick(Math.round(Renderer.getLocalInput().getMouseX() / effective_scale),
                  Math.round(Renderer.getLocalInput().getMouseY() / effective_scale));
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
				Renderer.getLocalInput().getPointerInput().setActiveCursor(current_gui_object.getCursorType());
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
		tool_tip.render(renderer,
                Math.round(Renderer.getLocalInput().getMouseX() / effective_scale),
                Math.round(Renderer.getLocalInput().getMouseY() / effective_scale) - CURSOR_OFFSET_Y,
                getWidth(), getHeight());
	}
}