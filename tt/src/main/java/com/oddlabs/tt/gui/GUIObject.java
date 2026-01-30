package com.oddlabs.tt.gui;

import com.oddlabs.tt.delegate.ModalDelegate;
import com.oddlabs.tt.guievent.EventListener;
import com.oddlabs.tt.guievent.FocusListener;
import com.oddlabs.tt.guievent.InputListener;
import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.guievent.MouseWheelListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public abstract class GUIObject extends Renderable<GUIObject> {
	private static final Logger logger = Logger.getLogger(GUIObject.class.getName());
    private static final int ROW_TOLERANCE = 5;

	/** If true then control is disabled otherwise false */
    private boolean disabled;
	/** if true then mouse is currently over control */
	private boolean hovered;
	/** If true then focus is this control or a child */
	private boolean active;

	private boolean focus_cycle;
	/** if true then control can be focused */
	private boolean can_focus;
	/** if true then control is in tab order */
	private boolean tab_stop = true;

	/** The child which is currently in focus chain or null if no child is focused */
	private @Nullable GUIObject focused_child = null;

	protected final void setTabStop(boolean tab_stop) {
		this.tab_stop = tab_stop;
	}

	public final boolean isTabStop() {
		return tab_stop;
	}

	private @Nullable GUIObject next_hover = null;

	protected final Set<@NonNull EventListener> listeners = new CopyOnWriteArraySet<>();

	/** If true then object has been placed in the layout*/
	private boolean placed = false;
	/** placement "gravity"" for this control */
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
		place(origin);
	}

	public final void place(@NonNull Origin origin) {
		assert !placed : "Object already placed";
		this.origin = origin;
		setPos(0, 0);
		placed = true;
	}

	public void place(@NonNull GUIObject neighbor, @NonNull Placement direction) {
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
		if (this.disabled != disabled) {
			this.disabled = disabled;
			if (disabled && isActive()) {
				// we have to move focus out of the disabled control
				var parent = getParent();
				if (null != parent) {
					parent.focusNext();
				}
			}
		}
	}

	public final boolean isActive() {
		return active;
	}

	/** Set the GUI focus to this control */
	private void setGlobalFocus() {
		GUIRoot gui_root = getParentGUIRoot();
		if (gui_root != null)
			gui_root.setGlobalFocus(this);
	}

	/** {@return the currently focused control in the hierarchy or null if not inserted} */
	private @Nullable GUIObject getGlobalFocus() {
		GUIRoot gui_root = getParentGUIRoot();
        return gui_root != null ? gui_root.getGlobalFocus() : null;
	}

	private boolean isFocused() {
		return this == getGlobalFocus();
	}

	/** {@return true if control is focusable} */
	@Override
	public final boolean isFocusable() {
		return can_focus;
	}

	/** {@return true if control is focusable and not disabled} */
	private boolean canFocus() {
		return can_focus && !isDisabled();
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

	private void switchFocusToFirstChild(@NonNull FocusDirection dirEnum, boolean bubble) {
        logger.info("switchFocusToFirstChild: " + this + " dir=" + dirEnum + " bubble=" + bubble);
        GUIObject bestCandidate = findNextFocusable(null, dirEnum);

		if (bestCandidate != null) {
            logger.info("  Found best candidate: " + bestCandidate);
			switchFocusToObject(bestCandidate, dirEnum);
        } else if (bubble && !focus_cycle) {
            logger.info("  No candidate found, bubbling to parent");
            GUIObject parent = getParent();
            if (parent != null)
			    parent.switchFocusToNextChild(dirEnum);
        } else {
            logger.info("  No candidate found, no bubble (cycle=" + focus_cycle + ")");
        }
	}

	private void switchFocusToNextChild(@NonNull FocusDirection dirEnum) {
        logger.info("switchFocusToNextChild: " + this + " dir=" + dirEnum + " current=" + focused_child);
        GUIObject bestCandidate = findNextFocusable(focused_child, dirEnum);

		if (bestCandidate != null) {
            logger.info("  Found next candidate: " + bestCandidate);
			switchFocusToObject(bestCandidate, dirEnum);
		} else if (focus_cycle) {
            logger.info("  No next candidate, cycling...");
            // Wrap around: find the "first" element again
            GUIObject first = findNextFocusable(null, dirEnum);
			if (first != null) {
                logger.info("  Cycled to: " + first);
				switchFocusToObject(first, dirEnum);
			} else {
                logger.info("  Cycle failed, no focusable children?");
            }
		} else {
            logger.info("  No next candidate, bubbling...");
			GUIObject parent = getParent();
			if (parent != null) {
				parent.switchFocusToNextChild(dirEnum);
			} else {
				// We are at the root (or detached) and not a cycle, but we should wrap if global cycle is desired
				// or just stay put. GUIRoot usually has focus_cycle=true so this else-block is for detached items.
                logger.info("  At root/detached, trying to wrap locally");
                GUIObject first = findNextFocusable(null, dirEnum);
				if (first != null) {
					switchFocusToObject(first, dirEnum);
				}
			}
		}
	}

    private @Nullable GUIObject findNextFocusable(@Nullable GUIObject current, @NonNull FocusDirection dir) {
        GUIObject bestCandidate = null;
        GUIObject child = getFirstChild();
        while (child != null) {
            if (child.canFocus() && child.isTabStop()) {
                if (isBetterCandidate(current, child, bestCandidate, dir)) {
                    bestCandidate = child;
                }
            } else {
                // logger.info("  Skipping child: " + child + " canFocus=" + child.canFocus() + " tabStop=" + child.isTabStop());
            }
            child = child.getNext();
        }
        return bestCandidate;
    }

    private boolean isBetterCandidate(@Nullable GUIObject current, @NonNull GUIObject candidate, @Nullable GUIObject bestSoFar, @NonNull FocusDirection dir) {
        if (current == candidate) return false;

        return switch (dir) {
            case FORWARD -> {
                // Candidate must be "after" current (if current exists)
                if (current != null && compareSpatial(candidate, current) <= 0) {
					yield false;
                }
                // Candidate must be "before" bestSoFar (if bestSoFar exists)
                yield bestSoFar == null || compareSpatial(candidate, bestSoFar) < 0;
            }
            case BACKWARD -> {
                // Candidate must be "before" current (if current exists)
                if (current != null && compareSpatial(candidate, current) >= 0) {
					yield false;
                }
                // Candidate must be "after" bestSoFar (if bestSoFar exists)
                yield bestSoFar == null || compareSpatial(candidate, bestSoFar) > 0;
            }
            default -> isBetterDirectionalCandidate(current, candidate, bestSoFar, dir);
        };
    }

    private boolean isBetterDirectionalCandidate(@Nullable GUIObject current, @NonNull GUIObject candidate, @Nullable GUIObject bestSoFar, @NonNull FocusDirection dir) {
        if (current == null) {
            //  default to reading order first element.
            return isBetterCandidate(null, candidate, bestSoFar, FocusDirection.FORWARD);
        }

        int dx = candidate.getX() - current.getX();
        int dy = candidate.getY() - current.getY();

        boolean validDirection = switch (dir) {
            case RIGHT -> dx > 0;
            case LEFT -> dx < 0;
            case UP -> dy > 0; // Y grows UP (0 is bottom)
            case DOWN -> dy < 0;
            default -> false;
        };

        if (!validDirection) return false;

        int distSq = dx * dx + dy * dy;
        int bestDistSq = Integer.MAX_VALUE;

        if (bestSoFar != null) {
            int bdx = bestSoFar.getX() - current.getX();
            int bdy = bestSoFar.getY() - current.getY();
            bestDistSq = bdx * bdx + bdy * bdy;
        }

        return distSq < bestDistSq;
    }

    /**
     * Compares two GUIObjects spatially based on reading order (Top-Left to Bottom-Right).
     * @return negative if a comes before b, positive if a comes after b, 0 if equal.
     */
    private int compareSpatial(@NonNull GUIObject a, @NonNull GUIObject b) {
        // Primary Sort: Y (Descending) - because Y grows UP in this system (0 is bottom).
        // Higher Y means "above", which comes first in reading order.
        int yDiff = b.getY() - a.getY();

        if (Math.abs(yDiff) > ROW_TOLERANCE) {
            return yDiff;
        }

        // Secondary Sort: X (Ascending)
        return a.getX() - b.getX();
    }

	private static void switchFocusToObject(@NonNull GUIObject obj, @NonNull FocusDirection dir) {
        logger.info("switchFocusToObject: " + obj);
		obj.setFocus(dir);
	}

	protected final void switchFocus(@NonNull FocusDirection direction) {
        switchFocus(direction, true);
	}

    protected final void switchFocus(@NonNull FocusDirection direction, boolean bubble) {
		// find any GUIObject to focus
		if (focused_child == null) {
			switchFocusToFirstChild(direction, bubble);
			return;
		}
		// Find next GUIObject in tab_order
		switchFocusToNextChild(direction);
    }

	protected final void focusNext() {
		switchFocus(FocusDirection.FORWARD);
	}

	protected final void focusPrior() {
		switchFocus(FocusDirection.BACKWARD);
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

	public void restoreFocus() {
		if (focused_child != null) {
			focused_child.restoreFocus();
		} else {
			setFocus();
		}
	}

	public void setFocus() {
        setFocus(FocusDirection.FORWARD);
    }

    public void setFocus(@NonNull FocusDirection direction) {
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

	final void handleInputAll(@NonNull InputEvent event) {
		if (event.isConsumed()) return;
		handleInput(event);
		if (event.isConsumed()) return;
		
		for (var listener : listeners) {
			if (listener instanceof InputListener l) {
				l.handleInput(event);
				if (event.isConsumed()) return;
			}
		}
		
		GUIObject parent = getParent();
		if (parent != null) {
			parent.handleInputAll(event);
		}
	}

	protected void handleInput(@NonNull InputEvent event) {
		if (event.consumeAction(GameAction.UI_ACTIVATE)) {
			if (event.getPhase() == InputPhase.PRESSED) {
				mousePressedAll(MouseButton.LEFT, 0, 0);
			} else if (event.getPhase() == InputPhase.RELEASED) {
				mouseReleasedAll(MouseButton.LEFT, 0, 0);
				mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
			}
		}
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

	public final void addInputListener(@NonNull InputListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void addFocusListener(@NonNull FocusListener listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}
}
