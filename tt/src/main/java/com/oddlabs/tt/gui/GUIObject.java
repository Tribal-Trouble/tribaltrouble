package com.oddlabs.tt.gui;

import com.oddlabs.tt.delegate.ModalDelegate;
import com.oddlabs.tt.guievent.EventListener;
import com.oddlabs.tt.guievent.FocusListener;
import com.oddlabs.tt.guievent.KeyListener;
import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.guievent.MouseWheelListener;
import com.oddlabs.tt.input.Key;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class GUIObject extends Renderable<GUIObject> {
    private boolean disabled;
	private boolean hovered;
	private boolean active;
	private boolean focus_cycle;
	private boolean can_focus;
	private boolean tab_stop = true;
	private int current_taborder = 0;
	private int tab_order;

	private @Nullable GUIObject focused_child = null;

	protected final void setTabStop(boolean tab_stop) {
		this.tab_stop = tab_stop;
	}

	public final boolean isTabStop() {
		return tab_stop;
	}

	private @Nullable GUIObject next_hover = null;

	private final Set<@NonNull EventListener> listeners = new CopyOnWriteArraySet<>();

	private boolean placed = false;
	private @NonNull Origin origin = Origin.AT_START;

	public GUIObject() {
	}

    @Override
    protected @NonNull GUIObject self() {
        return this;
    }

    public @Nullable GUIRoot getParentGUIRoot() {
        return null != parent ? parent.getParentGUIRoot() : null;
    }

    void addTree() {
        if (getParentGUIRoot() != null) {
            super.addTree();
        }
    }

    public void remove() {
        boolean notify_remove = getParentGUIRoot() != null && parent != null;
        super.remove();
        if (notify_remove) {
            removeTree();
        }
    }

    public int translateXToLocal(int x) {
		GUIObject parent = getParent();
        return parent == null ? x - getX() : parent.translateXToLocal(x) - getX();
	}

	int translateYToLocal(int y) {
		GUIObject parent = getParent();
        return parent == null ? y - getY() : parent.translateYToLocal(y) - getY();
	}

	void correctPos(int dx, int dy) {
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

	public void place(GUIObject neighbor, @NonNull Placement direction) {
		place(neighbor, direction, Skin.getSkin().getFormData().objectSpacing());
	}

	public final void place(@NonNull GUIObject neighbor, @NonNull Placement direction, int spacing) {
		assert !placed : "Object already placed";
		int new_x = getXFromDirection(direction, spacing, neighbor.getX(), neighbor.getWidth());
		int new_y = getYFromDirection(direction, spacing, neighbor.getY(), neighbor.getHeight());

		origin = neighbor.origin;
		setPos(new_x, new_y);
		placed = true;
	}

	private int getXFromDirection(@NonNull Placement direction, int spacing, int neighbour_x, int neighbour_width) {
        return switch (direction) {
            case BOTTOM_LEFT, TOP_LEFT -> neighbour_x;
            case BOTTOM_MID, TOP_MID -> neighbour_x + (neighbour_width - getWidth()) / 2;
            case BOTTOM_RIGHT, TOP_RIGHT -> neighbour_x + neighbour_width - getWidth();
            case RIGHT_BOTTOM, RIGHT_MID, RIGHT_TOP -> neighbour_x + neighbour_width + spacing;
            case LEFT_BOTTOM, LEFT_MID, LEFT_TOP -> neighbour_x - getWidth() - spacing;
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

	public final @NonNull Origin getOrigin() {
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
		GUIObject gui_child = getFirstChild();
		while (gui_child != null) {
			gui_child.setDisabled(disabled);
			gui_child = gui_child.getNext();
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

	private boolean isFocused() {
		return this == getGlobalFocus();
	}

	@Override
	public final boolean isFocusable() {
		return can_focus;
	}

	private boolean canFocus() {
		return can_focus && !disabled;
	}

	public final boolean isDisabled() {
		GUIObject parent = getParent();
        return disabled || (parent != null && parent.isDisabled());
	}

	public final boolean isHovered() {
		return hovered;
	}

	public final @Nullable GUIObject getNextHover() {
		return next_hover;
	}

	protected final void setNextHover(@Nullable GUIObject next_hover) {
		this.next_hover = next_hover;
	}

	protected final @Nullable GUIObject getFocusedChild() {
		return focused_child;
	}

	protected final void setGlobalFocused(@Nullable GUIObject gui_object) {
		if (gui_object != null && gui_object != focused_child) {
			putFirst(gui_object);
		}
		focused_child = gui_object;
	}

	@Override
	public void addChild(@NonNull GUIObject child) {
		super.addChild(child);
        child.setTabOrder(current_taborder);
		current_taborder++;
	}

	private void setTabOrder(int tab_order) {
		this.tab_order = tab_order;
	}

	private int getTabOrder() {
		return tab_order;
	}

	@Override
	public final void removeChild(@NonNull GUIObject child) {
		GUIObject current;
		if (child == focused_child) {
			focused_child = null;
			// If child is in the current focused path, select new current focus
			current = child;
			while (current.getFocusedChild() != null) {
                current = current.getFocusedChild();
            }
			if (current.isFocused())
				setGlobalFocus();
		}
		super.removeChild(child);
	}

	private void switchFocusToFirstChild(int dir) {
		GUIObject gui_child = getFirstChild();
		GUIObject min_obj = null;
		while (gui_child != null) {
			if (gui_child.canFocus() && gui_child.isTabStop() && (min_obj == null || dir*gui_child.getTabOrder() < dir*min_obj.getTabOrder()))
				min_obj = gui_child;
			gui_child = gui_child.getNext();
		}
		if (min_obj != null)
			switchFocusToObject(min_obj, dir);
		else if (!focus_cycle)
			getParent().switchFocusToNextChild(dir);
	}

	private void switchFocusToNextChild(int dir) {
		int tab_order = focused_child.getTabOrder();
		GUIObject gui_child = getFirstChild();
		GUIObject greater_obj = null;
		GUIObject min_obj = null;
		while (gui_child != null) {
			if (gui_child.canFocus() && gui_child.isTabStop()) {
				if (min_obj == null || dir*gui_child.getTabOrder() < dir*min_obj.getTabOrder())
					min_obj = gui_child;
				if (dir*gui_child.getTabOrder() > dir*tab_order && (greater_obj == null || dir*gui_child.getTabOrder() < dir*greater_obj.getTabOrder()))
					greater_obj = gui_child;
			}
			gui_child = gui_child.getNext();
		}
		if (greater_obj != null) {
			switchFocusToObject(greater_obj, dir);
		} else if (focus_cycle) {
			if (min_obj != null) {
				switchFocusToObject(min_obj, dir);
			}
		} else {
			GUIObject parent = getParent();
			if (parent != null) {
				parent.switchFocusToNextChild(dir);
			} else {
				// We are at the root (or detached) and not a cycle, but we should wrap if global cycle is desired
				// or just stay put. GUIRoot usually has focus_cycle=true so this else-block is for detached items.
				if (min_obj != null) {
					switchFocusToObject(min_obj, dir);
				}
			}
		}
	}

	private void switchFocusToObject(GUIObject obj, int dir) {
		if (obj instanceof Group group) {
			group.setGroupFocus(dir);
		} else {
			obj.setFocus();
		}
	}

	protected final void switchFocus(int direction) {
		// find any GUIObject to focus
		if (focused_child == null) {
			switchFocusToFirstChild(direction);
			return;
		}
		// Find next GUIObject in tab_order
		switchFocusToNextChild(direction);
	}

	protected final void focusNext() {
		switchFocus(1);
	}

	protected final void focusPrior() {
		switchFocus(-1);
	}

	private void defocusBranch() {
		if (getFocusedChild() != null)
			getFocusedChild().defocusBranch();
		focusNotifyAll(false);
		focused_child = null;
	}

	private void refocusTree(GUIObject caller_child) {
		GUIObject parent = getParent();
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

	final void focusNotifyAll(boolean focus) {
		active = focus;
		focusNotify(focus);
		for (var listener : listeners) {
			if (listener instanceof FocusListener l) {
				l.activated(focus);
			}
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
                getParent() != null && (getParent() == modal_delegate || getParent().modalRelative(modal_delegate));
	}

	protected @NonNull CursorType getCursorType() {
		return CursorType.NORMAL;
	}

	public boolean canHoverBehind() {
		return false;
	}

	final void mouseScrolledAll(int amount) {
		if (disabled)
			return;
		mouseScrolled(amount);
		for (var listener : listeners) {
			if (listener instanceof MouseWheelListener l) {
				l.mouseScrolled(amount);
			}
		}
	}

	protected void mouseScrolled(int amount) {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mouseScrolledAll(amount);
	}

	final void mouseDraggedAll (@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		if (disabled)
			return;
		mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
		for (var listener : listeners) {
			if (listener instanceof MouseMotionListener l) {
				l.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
			}
		}
	}

	protected void mouseDragged (@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		// do not send this to parents, because it would move the form if unstopped
	}

	final void mouseMovedAll(int x, int y) {
		if (disabled)
			return;
		mouseMoved(x, y);
		for (var listener : listeners) {
			if (listener instanceof MouseMotionListener l) {
				l.mouseMoved(x, y);
			}
		}
	}

	protected void mouseMoved(int x, int y) {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mouseMovedAll(x, y);
	}

	final void mouseExitedAll() {
		hovered = false;
		mouseExited();
		for (var listener : listeners) {
			if (listener instanceof MouseMotionListener l) {
				l.mouseExited();
			}
		}
	}

	protected void mouseExited() {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mouseExitedAll();
	}

	final void mouseEnteredAll() {
		hovered = true;
		mouseEntered();
		for (var listener : listeners) {
			if (listener instanceof MouseMotionListener l) {
				l.mouseEntered();
			}
        }
	}

	protected void mouseEntered() {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mouseEnteredAll();
	}

	final void mouseClickedAll (@NonNull MouseButton button, int x, int y, int clicks) {
		if (disabled)
			return;
		mouseClicked(button, x, y, clicks);
		for (var listener : listeners) {
			if (listener instanceof MouseClickListener l) {
    			l.mouseClicked(button, x, y, clicks);
			}
        }
	}

	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mouseClickedAll(button, x, y, clicks);
	}

	final void mouseReleasedAll (@NonNull MouseButton button, int x, int y) {
		if (disabled)
			return;
		mouseReleased(button, x, y);
		for (var listener : listeners) {
			if (listener instanceof MouseButtonListener l) {
				l.mouseReleased(button, x, y);
			}
        }
	}

	protected void mouseReleased (@NonNull MouseButton button, int x, int y) {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mouseReleasedAll(button, x, y);
	}

	final void mousePressedAll (@NonNull MouseButton button, int x, int y) {
		if (disabled)
			return;
		mousePressed(button, x, y);
		for (var listener : listeners) {
			if (listener instanceof MouseButtonListener l) {
				l.mousePressed(button, x, y);
			}
		}
	}

	protected void mousePressed (@NonNull MouseButton button, int x, int y) {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mousePressedAll(button, x, y);
	}

	final void mouseHeldAll (@NonNull MouseButton button, int x, int y) {
		if (disabled)
			return;
		mouseHeld(button, x, y);
		for (var listener : listeners) {
			if (listener instanceof MouseButtonListener l) {
    			l.mouseHeld(button, x, y);
			}
		}
	}

	protected void mouseHeld (@NonNull MouseButton button, int x, int y) {
		GUIObject parent = getParent();
		if (parent != null)
			parent.mouseHeldAll(button, x, y);
	}

	final boolean keyPressedAll(@NonNull KeyboardEvent event) {
		if (keyPressed(event)) return true;
        for (var listener : listeners) {
            if (listener instanceof KeyListener l) {
                if (l.keyPressed(event)) return true;
            }
        }
		GUIObject parent = getParent();
		return parent != null && parent.keyPressedAll(event);
	}

	protected boolean keyPressed(@NonNull KeyboardEvent event) {
		if (event.keyCode() == Key.SPACE || event.keyCode() == Key.RETURN) {
			mousePressedAll(MouseButton.LEFT, 0, 0);
			return true;
		}
		return false;
	}

	final boolean keyReleasedAll(@NonNull KeyboardEvent event) {
		if (keyReleased(event)) return true;
		for (var listener : listeners) {
			if (listener instanceof KeyListener l) {
				if (l.keyReleased(event)) return true;
			}
        }
		GUIObject parent = getParent();
		return parent != null && parent.keyReleasedAll(event);
	}

	protected boolean keyReleased(@NonNull KeyboardEvent event) {
		if (event.keyCode() == Key.SPACE || event.keyCode() == Key.RETURN) {
			mouseReleasedAll(MouseButton.LEFT, 0, 0);
			mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
			return true;
		}
		return false;
	}

	final boolean keyRepeatAll(@NonNull KeyboardEvent event) {
		if (keyRepeat(event)) return true;
		for (var listener : listeners) {
			if (listener instanceof KeyListener l) {
				if (l.keyRepeat(event)) return true;
			}
		}
		GUIObject parent = getParent();
		return parent != null && parent.keyRepeatAll(event);
	}

	protected boolean keyRepeat(@NonNull KeyboardEvent event) {
		return false;
	}

	public final void addMouseClickListener(@NonNull MouseClickListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	/** @apiNote Your listener will also receive {@link MouseClickListener#mouseClicked} events */
	final void addMouseButtonListener(@NonNull MouseButtonListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	final void addMouseMotionListener(@NonNull MouseMotionListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addMouseWheelListener(@NonNull MouseWheelListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	final void addKeyListener(@NonNull KeyListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addFocusListener(@NonNull FocusListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}
}
