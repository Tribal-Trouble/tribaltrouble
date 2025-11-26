package com.oddlabs.tt.gui;

import com.oddlabs.tt.delegate.ModalDelegate;
import com.oddlabs.tt.guievent.FocusListener;
import com.oddlabs.tt.guievent.KeyListener;
import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.guievent.MouseWheelListener;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Objects;

public abstract class GUIObject extends Renderable {
    private boolean disabled;
	private boolean hovered;
	private boolean active;
	private boolean focus_cycle;
	private boolean can_focus;
	private int current_taborder = 0;
	private int tab_order;

	private @Nullable GUIObject focused_child = null;
	private @Nullable GUIObject next_hover = null;

	private final List<@NonNull MouseClickListener> mouse_click_listeners = new java.util.ArrayList<>();
	private final List<@NonNull MouseButtonListener> mouse_button_listeners = new java.util.ArrayList<>();
	private final List<@NonNull MouseMotionListener> mouse_motion_listeners = new java.util.ArrayList<>();
	private final List<@NonNull MouseWheelListener> mouse_wheel_listeners = new java.util.ArrayList<>();
	private final List<@NonNull KeyListener> key_listeners = new java.util.ArrayList<>();
	private final List<@NonNull FocusListener> focus_listeners = new java.util.ArrayList<>();

	private boolean placed = false;
	private @Nullable Origin origin;

	public GUIObject() {
		focus_cycle = false;
		can_focus = false;
		disabled = false;
	}

    @Override
    protected @NonNull GUIObject self() {
        return this;
    }

    public int translateXToLocal(int x) {
		GUIObject parent = (GUIObject)getParent();
        return parent == null ? x - getX() : parent.translateXToLocal(x) - getX();
	}

	public int translateYToLocal(int y) {
		GUIObject parent = (GUIObject)getParent();
        return parent == null ? y - getY() : parent.translateYToLocal(y) - getY();
	}

	public void correctPos(int dx, int dy) {
		setPos(getX() + dx, getY() + dy);
	}

	public final void place() {
		place(Origin.AT_START);
	}

	public final void place(@NonNull Origin origin) {
		assert !placed : "Object already placed";
		this.origin = origin;
		setPos(0, 0);
		placed = true;
	}

	public final void place(@NonNull GUIObject neighbor, @NonNull Placement direction) {
		place(neighbor, direction, Skin.getSkin().getFormData().getObjectSpacing());
	}

	public final void place(@NonNull GUIObject neighbor, @NonNull Placement direction, int spacing) {
		assert !placed : "Object already placed";
		int new_x = getXFromDirection(direction, spacing, neighbor.getX(), neighbor.getWidth());
		int new_y = getYFromDirection(direction, spacing, neighbor.getY(), neighbor.getHeight());

		origin = neighbor.origin;
		setPos(new_x, new_y);
		placed = true;
	}

	private int getXFromDirection(@NonNull Placement direction, int spacing, int neightbour_x, int neighbour_width) {
        return switch (direction) {
            case BOTTOM_LEFT, TOP_LEFT -> neightbour_x;
            case BOTTOM_MID, TOP_MID -> neightbour_x + (neighbour_width - getWidth()) / 2;
            case BOTTOM_RIGHT, TOP_RIGHT -> neightbour_x + neighbour_width - getWidth();
            case RIGHT_BOTTOM, RIGHT_MID, RIGHT_TOP -> neightbour_x + neighbour_width + spacing;
            case LEFT_BOTTOM, LEFT_MID, LEFT_TOP -> neightbour_x - getWidth() - spacing;
        };
	}

	private int getYFromDirection(@NonNull Placement direction, int spacing, int neighbour_y, int neighbour_height) {
        return switch (direction) {
            case BOTTOM_LEFT, BOTTOM_MID, BOTTOM_RIGHT -> neighbour_y - getHeight() - spacing;
            case TOP_LEFT, TOP_RIGHT, TOP_MID -> neighbour_y + neighbour_height + spacing;
            case RIGHT_BOTTOM, LEFT_BOTTOM -> neighbour_y;
            case RIGHT_TOP, LEFT_TOP -> neighbour_y + neighbour_height - getHeight();
            case LEFT_MID, RIGHT_MID -> neighbour_y + (neighbour_height - getHeight()) / 2;
        };
	}

	public final @Nullable Origin getOrigin() {
		assert placed : "Object " + this + " compiled before being placed";
		return origin;
	}

	protected final void setFocusCycle(boolean cycle) {
		focus_cycle = cycle;
	}

	protected final void setCanFocus(boolean can_focus) {
		this.can_focus = can_focus;
	}

	public void setDisabled(boolean disabled) {
		if (this.disabled == disabled)
			return;

		this.disabled = disabled;
		GUIObject gui_child = (GUIObject)getFirstChild();
		while (gui_child != null) {
			gui_child.setDisabled(disabled);
			gui_child = (GUIObject)gui_child.getNext();
		}
		if (isFocused())
			focusNext();
	}

	public final boolean isActive() {
		return active;
	}

	private void setGlobalFocus() {
		GUIRoot gui_root = getParentGUIRoot();
		if (gui_root != null)
			gui_root.setGlobalFocus(this);
	}

	private @Nullable GUIObject getGlobalFocus() {
		GUIRoot gui_root = getParentGUIRoot();
        return gui_root != null ? gui_root.getGlobalFocus() : null;
	}

	public final boolean isFocused() {
		return this == getGlobalFocus();
	}

	@Override
	public final boolean isFocusable() {
		return can_focus;
	}

	public final boolean canFocus() {
		return can_focus && !disabled;
	}

	public final boolean isDisabled() {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			return disabled || parent.isDisabled();
		else
			return disabled;
	}

	public final boolean isHovered() {
		return hovered;
	}

	public final @Nullable GUIObject getNextHover() {
		return next_hover;
	}

	protected final void setNextHover(GUIObject next_hover) {
		this.next_hover = next_hover;
	}

	public final @Nullable GUIObject getFocusedChild() {
		return focused_child;
	}

	protected final void setGlobalFocused(@Nullable GUIObject gui_object) {
		if (gui_object != null && gui_object != focused_child) {
			putFirst(gui_object);
		}
		focused_child = gui_object;
	}

	@Override
	public void addChild(@NonNull Renderable child) {
		super.addChild(child);
		GUIObject current;
		current = (GUIObject)child;
		current.setTabOrder(current_taborder);
		current_taborder++;
	}

	protected final void setTabOrder(int tab_order) {
		this.tab_order = tab_order;
	}

	protected final int getTabOrder() {
		return tab_order;
	}

	@Override
	public final void removeChild(@NonNull Renderable child) {
		GUIObject current;
		if (child == focused_child) {
			focused_child = null;
			// If child is in the current focused path, select new current focus
			current = (GUIObject)child;
			while (current.getFocusedChild() != null) {
                current = current.getFocusedChild();
            }
			if (current.isFocused())
				setGlobalFocus();
		}
		super.removeChild(child);
	}

	private void switchFocusToFirstChild(int dir) {
		GUIObject gui_child = (GUIObject)getFirstChild();
		GUIObject min_obj = null;
		while (gui_child != null) {
			if (gui_child.canFocus() && (min_obj == null || dir*gui_child.getTabOrder() < dir*min_obj.getTabOrder()))
				min_obj = gui_child;
			gui_child = (GUIObject)gui_child.getNext();
		}
		if (min_obj != null)
			switchFocusToObject(min_obj, dir);
		else if (!focus_cycle)
			((GUIObject)getParent()).switchFocusToNextChild(dir);
	}

	private void switchFocusToNextChild(int dir) {
		int tab_order = focused_child.getTabOrder();
		GUIObject gui_child = (GUIObject)getFirstChild();
		GUIObject greater_obj = null;
		GUIObject min_obj = null;
		while (gui_child != null) {
			if (gui_child.canFocus()) {
				if (min_obj == null || dir*gui_child.getTabOrder() < dir*min_obj.getTabOrder())
					min_obj = gui_child;
				if (dir*gui_child.getTabOrder() > dir*tab_order && (greater_obj == null || dir*gui_child.getTabOrder() < dir*greater_obj.getTabOrder()))
					greater_obj = gui_child;
			}
			gui_child = (GUIObject)gui_child.getNext();
		}
		if (greater_obj != null) {
			switchFocusToObject(greater_obj, dir);
		} else if (focus_cycle) {
			if (min_obj != null) {
				switchFocusToObject(min_obj, dir);
			}
		} else if (canFocus()) {
			switchFocusToObject(this, dir);
		} else {
			((GUIObject)getParent()).switchFocusToNextChild(dir);
		}
	}

	private void switchFocusToObject(GUIObject obj, int dir) {
		if (obj instanceof Group group) {
			group.setGroupFocus(dir);
		} else {
			obj.setFocus();
		}
	}

	public final void switchFocus(int direction) {
		// find any GUIObject to focus
		if (focused_child == null) {
			switchFocusToFirstChild(direction);
			return;
		}
		// Find next GUIObject in tab_order
		switchFocusToNextChild(direction);
	}

	public final void focusNext() {
		switchFocus(1);
	}

	public final void focusPrior() {
		switchFocus(-1);
	}

	public final void defocusBranch() {
		if (getFocusedChild() != null)
			getFocusedChild().defocusBranch();
		focusNotifyAll(false);
		focused_child = null;
	}

	private void refocusTree(GUIObject caller_child) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null) {
			parent.refocusTree(this);
		}
		if (getFocusedChild() != null && getFocusedChild() != caller_child) {
			getFocusedChild().defocusBranch();
		}

		if (!active)
			focusNotifyAll(true);
		focused_child = caller_child;
	}

	public void setFocus() {
		GUIRoot gui_root = getParentGUIRoot();
		// Make sure we are linked to the root
		if (gui_root == null)
			return;
		if (isFocused() || !canFocus() || modalBlocked(gui_root)) {
			if (modalBlocked(gui_root)) {
				gui_root.swapFocusBackup(this);
			}
			return;
		}
		refocusTree(null);
		setGlobalFocus();
	}

	protected final void focusNotifyAll(boolean focus) {
		active = focus;
		focusNotify(focus);
		for (FocusListener listener : focus_listeners) {
			listener.activated(focus);
		}
	}

	protected void focusNotify(boolean focus) {
	}

	private boolean modalBlocked(@NonNull GUIRoot gui_root) {
		ModalDelegate modal_delegate = gui_root.getModalDelegate();
		return modal_delegate != null && !modalRelative(modal_delegate);
	}

	private boolean modalRelative(ModalDelegate modal_delegate) {
        return this == modal_delegate ||
                getParent() != null && (getParent() == modal_delegate ||
                        ((GUIObject) getParent()).modalRelative(modal_delegate));
	}

	protected @NonNull CursorType getCursorType() {
		return CursorType.NORMAL;
	}

	public boolean canHoverBehind() {
		return false;
	}

	public final void mouseScrolledAll(int amount) {
		if (disabled)
			return;
		mouseScrolled(amount);
		for (MouseWheelListener listener : mouse_wheel_listeners) {
			listener.mouseScrolled(amount);
		}
	}

	protected void mouseScrolled(int amount) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mouseScrolledAll(amount);
	}

	public final void mouseDraggedAll(int button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		if (disabled)
			return;
		mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
		for (MouseMotionListener listener : mouse_motion_listeners) {
			listener.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
		}
	}

	protected void mouseDragged(int button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		// do not send this to parents, because it would move the form if unstopped
	}

	public final void mouseMovedAll(int x, int y) {
		if (disabled)
			return;
		mouseMoved(x, y);
		for (MouseMotionListener listener : mouse_motion_listeners) {
			listener.mouseMoved(x, y);
		}
	}

	protected void mouseMoved(int x, int y) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mouseMovedAll(x, y);
	}

	public final void mouseExitedAll() {
		hovered = false;
		mouseExited();
		for (MouseMotionListener listener : mouse_motion_listeners) {
			listener.mouseExited();
		}
	}

	protected void mouseExited() {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mouseExitedAll();
	}

	public final void mouseEnteredAll() {
		hovered = true;
		mouseEntered();
		for (MouseMotionListener listener : mouse_motion_listeners) {
			listener.mouseEntered();
        }
	}

	protected void mouseEntered() {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mouseEnteredAll();
	}

	public final void mouseClickedAll(int button, int x, int y, int clicks) {
		if (disabled)
			return;
		mouseClicked(button, x, y, clicks);
		for (MouseClickListener listener : mouse_click_listeners) {
    		listener.mouseClicked(button, x, y, clicks);
        }
	}

	protected void mouseClicked(int button, int x, int y, int clicks) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mouseClickedAll(button, x, y, clicks);
	}

	public final void mouseReleasedAll(int button, int x, int y) {
		if (disabled)
			return;
		mouseReleased(button, x, y);
		for (MouseButtonListener listener : mouse_button_listeners) {
			listener.mouseReleased(button, x, y);
        }
	}

	protected void mouseReleased(int button, int x, int y) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mouseReleasedAll(button, x, y);
	}

	public final void mousePressedAll(int button, int x, int y) {
		if (disabled)
			return;
		mousePressed(button, x, y);
		for (MouseButtonListener listener : mouse_button_listeners) {
			listener.mousePressed(button, x, y);
		}
	}

	protected void mousePressed(int button, int x, int y) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mousePressedAll(button, x, y);
	}

	public final void mouseHeldAll(int button, int x, int y) {
		if (disabled)
			return;
		mouseHeld(button, x, y);
		for (MouseButtonListener listener : mouse_button_listeners) {
    		listener.mouseHeld(button, x, y);
		}
	}

	protected void mouseHeld(int button, int x, int y) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.mouseHeldAll(button, x, y);
	}

	public final void keyPressedAll(@NonNull KeyboardEvent event) {
		keyPressed(event);
        for (KeyListener listener : key_listeners) {
            listener.keyPressed(event);
        }
	}

	protected void keyPressed(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() == Keyboard.KEY_SPACE || event.getKeyCode() == Keyboard.KEY_RETURN) {
			mousePressedAll(LocalInput.LEFT_BUTTON, 0, 0);
		} else {
			GUIObject parent = (GUIObject)getParent();
			if (parent != null)
				parent.keyPressedAll(event);
		}
	}

	public final void keyReleasedAll(@NonNull KeyboardEvent event) {
		keyReleased(event);
		for (KeyListener listener : key_listeners) {
			listener.keyReleased(event);
        }
	}

	protected void keyReleased(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() == Keyboard.KEY_SPACE || event.getKeyCode() == Keyboard.KEY_RETURN) {
			mouseReleasedAll(LocalInput.LEFT_BUTTON, 0, 0);
			mouseClickedAll(LocalInput.LEFT_BUTTON, 0, 0, 1);
		} else {
			GUIObject parent = (GUIObject)getParent();
			if (parent != null)
				parent.keyReleasedAll(event);
		}
	}

	public final void keyRepeatAll(@NonNull KeyboardEvent event) {
		keyRepeat(event);
		for (KeyListener listener : key_listeners) {
			listener.keyRepeat(event);
		}
	}

	protected void keyRepeat(@NonNull KeyboardEvent event) {
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.keyRepeatAll(event);
	}

	public final void addMouseClickListener(@NonNull MouseClickListener listener) {
		mouse_click_listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addMouseButtonListener(@NonNull MouseButtonListener listener) {
		mouse_button_listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addMouseMotionListener(@NonNull MouseMotionListener listener) {
		mouse_motion_listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addMouseWheelListener(@NonNull MouseWheelListener listener) {
		mouse_wheel_listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addKeyListener(@NonNull KeyListener listener) {
		key_listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addFocusListener(@NonNull FocusListener listener) {
		focus_listeners.add(Objects.requireNonNull(listener, "listener"));
	}
}
