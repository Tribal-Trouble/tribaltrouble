package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.MapCamera;
import com.oddlabs.tt.form.InGameChatForm;
import com.oddlabs.tt.gui.ActionButtonPanel;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Army;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.IdleController;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.Notification;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public final class SelectionDelegate extends ControllableCameraDelegate {
	private static final int SELECTION_COLOR = 0xFF_4C_FF_00;

	private final @NonNull InGameChatForm chat_form;
	private final @NonNull Label observer_label;
	private final @NonNull GameCamera game_camera;

	private boolean close_chat_override = false;
	private boolean chat_visible;
	private boolean selection = false;
	private int selection_x1;
	private int selection_y1;
	private int selection_x2;
	private int selection_y2;
	private boolean pick_done = false;
	private boolean map_mode = false;
	private boolean observer = false;
	private int last_idle_peon_name = -1;

	public SelectionDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera camera) {
		super(viewer, camera);
		String observer_mode = Utils.getBundleString(ResourceBundle.getBundle(SelectionDelegate.class.getName()), "observer_mode");
		this.observer_label = new Label(observer_mode, Skin.getSkin().getHeadlineFont());
		this.game_camera = (GameCamera)getCamera();
		displayChangedNotify(LocalInput.getViewWidth(), LocalInput.getViewHeight());
		addChild(getViewer().getPanel());
		chat_form = new InGameChatForm(getViewer().getGUIRoot().getInfoPrinter(), getViewer());
		chat_form.addCloseListener(() -> {
			if (LocalInput.isKeyDown(Keyboard.KEY_RETURN)) {
				close_chat_override = true;
			}
			chat_visible = false;
		});
		chat_visible = false;
		((GameCamera)getCamera()).setOwner(this);
	}

	public @NonNull InGameChatForm getChatForm() {
		return chat_form;
	}

    private @NonNull ActionButtonPanel getActionButtonPanel() {
		return getViewer().getPanel();
	}

	public void setObserverMode() {
		observer = true;
		getViewer().getSelection().clearSelection();
		if (!map_mode)
			addChild(observer_label);
	}

	@Override
	public void keyPressed(@NonNull KeyboardEvent event) {
		getCamera().keyPressed(event);
		int army_number = 0;
		switch (event.getKeyCode()) {
			case Keyboard.KEY_SPACE:
			case Keyboard.KEY_NUMPAD5:
				if (!map_mode) {
					selection = false;
					getViewer().getPicker().pickRotate((GameCamera)getCamera());
					map_mode = true;
					if (observer)
						observer_label.remove();
					else
						getActionButtonPanel().remove();
					getCamera().disable();
					setCamera(new MapCamera(this, game_camera));
					getCamera().enable();
				}
				break;
			case Keyboard.KEY_TAB:
				if (!observer) {
					Notification n = getViewer().getNotificationManager().getLatestNotification();
					if (n != null) {
						if (getCamera() instanceof GameCamera)
							getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), (GameCamera)getCamera(), n.getX(), n.getY()));
						else if (getCamera() instanceof MapCamera)
							((MapCamera)getCamera()).mapGoto(n.getX(), n.getY(), true);
					}
				}
				break;
			case Keyboard.KEY_9: army_number++;
			case Keyboard.KEY_8: army_number++;
			case Keyboard.KEY_7: army_number++;
			case Keyboard.KEY_6: army_number++;
			case Keyboard.KEY_5: army_number++;
			case Keyboard.KEY_4: army_number++;
			case Keyboard.KEY_3: army_number++;
			case Keyboard.KEY_2: army_number++;
			case Keyboard.KEY_1: army_number++;
			case Keyboard.KEY_0:
				if (!map_mode && !observer) {
					if (event.isControlDown()) {
						getViewer().getSelection().setShortcutArmy(army_number);
					} else {
						boolean selected = getViewer().getSelection().enableShortcutArmy(army_number);
						if (selected && event.getNumClicks() > 1) {
							Set<Selectable> set = getViewer().getSelection().getCurrentSelection().getSet();
							if (!set.isEmpty()) {
								Selectable s = set.iterator().next();
								getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), (GameCamera)getCamera(), s.getPositionX(), s.getPositionY()));
							}
						}
					}
				}
				break;
			case Keyboard.KEY_RETURN:
					if (!chat_visible)
						chat_form.setReceivers(!event.isShiftDown());
				break;
			case Keyboard.KEY_B:
				if (event.isControlDown() && !map_mode && !observer) {
					getGUIRoot().pushDelegate(new BeaconDelegate(getViewer(), (GameCamera)getCamera()));
				}
				break;
			case Keyboard.KEY_N:
				nextIdlePeon();
				break;
			case Keyboard.KEY_F:
			case Keyboard.KEY_Z:
				if (!map_mode)
					super.keyPressed(event);
				break;
			default:
				if (map_mode || observer) {
					super.keyPressed(event);
				} else {
					if (!getActionButtonPanel().doKeyPressed(event))
						super.keyPressed(event);
				}
				break;
		}
	}

	private void changeGamespeed(int delta) {
		getViewer().getPeerHub().getPlayerInterface().changePreferredGamespeed(delta);
	}

	private void nextIdlePeon() {
		Set<@NonNull Selectable> set = getViewer().getLocalPlayer().getUnits().getSet();

		boolean has_idle_peon = false;
		int lowest_name = Integer.MAX_VALUE;
		Selectable lowest_peon = null;

		boolean has_greater_name = false;
		int lowest_greater_name = Integer.MAX_VALUE;
		Selectable lowest_greater_peon = null;
		for (Selectable s : set) {
			if (s.getOwner() != getViewer().getLocalPlayer())
				continue;
			Abilities abilities = s.getAbilities();
			if ((abilities.hasAbilities(Abilities.BUILD)) && (s.getPrimaryController() instanceof IdleController)) {
				int name = getViewer().getDistributableTable().getName(s);
				if (name < lowest_name) {
					has_idle_peon = true;
					lowest_name = name;
					lowest_peon = s;
				}
				if (name > last_idle_peon_name && name < lowest_greater_name) {
					has_greater_name = true;
					lowest_greater_name = name;
					lowest_greater_peon = s;
				}
			}
		}

		Selectable target = null;
		if (has_greater_name) {
			last_idle_peon_name = lowest_greater_name;
			target = lowest_greater_peon;
		} else if (has_idle_peon) {
			last_idle_peon_name = lowest_name;
			target = lowest_peon;
		}

		if (target != null && getCamera() instanceof GameCamera) {
			getViewer().getSelection().clearSelection();
			getViewer().getSelection().getCurrentSelection().add(target);
			getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), (GameCamera)getCamera(), target.getPositionX(), target.getPositionY()));
		}
	}

	@Override
	public void keyRepeat(@NonNull KeyboardEvent event) {
//		getCamera().keyRepeat(event);
		switch (event.getKeyChar()) {
			case '+':
				changeGamespeed(1);
				break;
			case '-':
				changeGamespeed(-1);
				break;
			default:
				if (!map_mode && !observer && !getActionButtonPanel().doKeyRepeat(event))
					super.keyRepeat(event);
				break;
		}
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
		getCamera().keyReleased(event);
		switch (event.getKeyCode()) {
			case Keyboard.KEY_RETURN:
				if (!close_chat_override) {
					if (!chat_visible) {
						addChild(chat_form);
						chat_form.setPos(GameCamera.SCROLL_BUFFER, GameCamera.SCROLL_BUFFER);
						chat_form.setFocus();
						chat_visible = true;
					}
				} else {
					close_chat_override = false;
				}
				break;
			default:
				if (!map_mode && !observer && !getActionButtonPanel().doKeyReleased(event))
					super.keyReleased(event);
				break;
		}
	}

	@Override
	protected @NonNull CursorType getCursorType() {
        return map_mode ? CursorType.TARGET : CursorType.NORMAL;
	}

	public void exitMapMode() {
		map_mode = false;
		getCamera().disable();
		setCamera(game_camera);
		getCamera().enable();
		if (observer)
			addChild(observer_label);
		else
			addChild(getActionButtonPanel());

		if (chat_visible) {
			chat_form.remove();
			addChild(chat_form);
			chat_visible = true;
		}
	}

	private void updateSelection(@NonNull List<@NonNull Selectable> friendly_units, Selectable friendly_building, Selectable enemy) {
		Army current_selection = getViewer().getSelection().getCurrentSelection();
		Selectable first = current_selection.getSet().iterator().next();
		if (first instanceof Building || first.getOwner() != getViewer().getLocalPlayer()) {
			if (first == friendly_building || first == enemy) {
				current_selection.clear();
			}
			return;
		}

		boolean add = false;
        for (Selectable selectable : friendly_units) {
            if (!current_selection.contains(selectable)) {
                add = true;
                break;
            }
        }
        for (Selectable selectable : friendly_units) {
            if (add) {
                if (!current_selection.contains(selectable))
                    current_selection.add(selectable);
            } else {
                current_selection.remove(selectable);
            }
        }
	}

	private void replaceSelection(@NonNull List<Selectable> friendly_units, @Nullable Selectable friendly_building, @Nullable Selectable enemy) {
		Army current_selection = getViewer().getSelection().getCurrentSelection();
		current_selection.clear();
		if (!friendly_units.isEmpty()) {
            for (Selectable friendlyUnit : friendly_units) {
                current_selection.add(friendlyUnit);
            }
		} else if (friendly_building != null) {
			current_selection.add(friendly_building);
		} else if (enemy != null) {
			current_selection.add(enemy);
		}
	}

	@Override
	public void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
		if (button == MouseButton.LEFT && !map_mode && !observer) {
			if (selection) {
				selection = false;
				Selectable[] picked = getViewer().getPicker().pickBoxed(getViewer().getGUIRoot().getDelegate().getCamera().getState(), selection_x1, selection_y1, selection_x2, selection_y2, clicks);
				List<Selectable> friendly_units = new ArrayList<>();
				Selectable friendly_building = null;
				Selectable enemy = null;
                for (Selectable selectable : picked) {
                    if (selectable != null) {
                        if (selectable.getOwner() == getViewer().getLocalPlayer()) {
                            if (selectable instanceof Building)
                                friendly_building = selectable;
                            else if (selectable instanceof Unit)
                                friendly_units.add(selectable);
                            else
                                throw new RuntimeException();
                        } else {
                            enemy = selectable;
                        }
                    }
                }
				if (LocalInput.isShiftDownCurrently() && getViewer().getSelection().getCurrentSelection().size() > 0)
					updateSelection(friendly_units, friendly_building, enemy);
				else
					replaceSelection(friendly_units, friendly_building, enemy);
				pick_done = true;
			}
		}
	}

	@Override
	public void mouseReleased (@NonNull MouseButton button, int x, int y) {
		if (map_mode) {
			if (button == MouseButton.LEFT) {
				getViewer().getPicker().pickMapGoto(x, y, (MapCamera)getCamera());
			}
		} else if (!observer) {
			if (!pick_done)
				mouseClicked(button, x, y, 1);
			pick_done = false;
			super.mouseReleased(button, x, y);
		} else {
			super.mouseReleased(button, x, y);
		}
	}

	@Override
	public boolean canHoverBehind() {
		return true;
	}

	@Override
	public void mouseDragged (@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		if (!map_mode) {
			if (!observer) {
				if (button == MouseButton.LEFT) {
					selection_x2 += relative_x;
					selection_y2 += relative_y;
				} else {
					super.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
				}
			} else {
				super.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
			}
		}
	}

	@Override
	public void mousePressed (@NonNull MouseButton button, int x, int y) {
		if (!map_mode) {
			if (!observer) {
                switch (button) {
                    case LEFT:
                        if (!LocalInput.isKeyDown(Keyboard.KEY_SPACE)) {
                            selection = true;
                        }
                        selection_x1 = x;
                        selection_y1 = y;
                        selection_x2 = x;
                        selection_y2 = y;
                        break;
                    case RIGHT: {
                        Army selection = getViewer().getSelection().getCurrentSelection();
                        if (selection.size() > 0 && selection.containsAbility(Abilities.TARGET)) {
                            getViewer().getPicker().pickTarget(selection, getViewer().getGUIRoot().getDelegate().getCamera().getState(), getViewer().getPeerHub().getPlayerInterface(), x, y, Action.DEFAULT);
                        }
                        break;
                    }
                    default:
                        super.mousePressed(button, x, y);
                        break;
                }
            } else {
				super.mousePressed(button, x, y);
			}
		}

	}

	public boolean isSelecting() {
		return selection;
	}

	@Override
	public boolean keyboardBlocked() {
		return chat_visible && chat_form.isActive();
	}

	@Override
	public void render2D(@NonNull GUIRenderer renderer) {
		if (selection) {
			float minX = Math.min(selection_x1, selection_x2);
			float minY = Math.min(selection_y1, selection_y2);
			float maxX = Math.max(selection_x1, selection_x2);
			float maxY = Math.max(selection_y1, selection_y2);
			float w = maxX - minX;
			float h = maxY - minY;

			renderer.drawColoredQuad(minX, minY, w, 1, SELECTION_COLOR);
			renderer.drawColoredQuad(minX, maxY - 1, w, 1, SELECTION_COLOR);
			renderer.drawColoredQuad(minX, minY + 1, 1, h - 2, SELECTION_COLOR);
			renderer.drawColoredQuad(maxX - 1, minY + 1, 1, h - 2, SELECTION_COLOR);
		}
	}

	@Override
	public void displayChangedNotify(int width, int height) {
		super.displayChangedNotify(width, height);
		observer_label.setPos((width - observer_label.getWidth())/2, height - observer_label.getHeight());
	}
}
