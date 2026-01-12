package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.MapCamera;
import com.oddlabs.tt.form.InGameChatForm;
import com.oddlabs.tt.gui.ActionButtonPanel;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Army;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitTemplate;
import com.oddlabs.tt.model.behaviour.IdleController;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.Notification;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Color;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public final class SelectionDelegate extends ControllableCameraDelegate {
	private static final Vector4fc SELECTION_COLOR = Color.argb4v(0xFF_4C_FF_00);

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
		displayChangedNotify(getGUIRoot().getWidth(), getGUIRoot().getHeight());
		addChild(getViewer().getPanel());
		chat_form = new InGameChatForm(getViewer().getGUIRoot().getInfoPrinter(), getViewer());
		chat_form.addCloseListener(() -> {
			if (Renderer.getLocalInput().isKeyDown(Key.RETURN)) {
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
	public boolean keyPressed(@NonNull KeyboardEvent event) {
		if (getCamera().keyPressed(event)) return true;
		int army_number = 0;
		switch (event.keyCode()) {
			case SPACE:
			case NUMPAD5:
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
				return true;
			case TAB:
				if (!observer) {
					Notification n = getViewer().getNotificationManager().getLatestNotification();
					if (n != null) {
						if (getCamera() instanceof GameCamera)
							getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), (GameCamera)getCamera(), n.getX(), n.getY()));
						else if (getCamera() instanceof MapCamera)
							((MapCamera)getCamera()).mapGoto(n.getX(), n.getY(), true);
					}
				}
				return true;
			case KEY_9: army_number++;
			case KEY_8: army_number++;
			case KEY_7: army_number++;
			case KEY_6: army_number++;
			case KEY_5: army_number++;
			case KEY_4: army_number++;
			case KEY_3: army_number++;
			case KEY_2: army_number++;
			case KEY_1: army_number++;
			case KEY_0:
				if (!map_mode && !observer) {
					if (event.controlDown()) {
						getViewer().getSelection().setShortcutArmy(army_number);
					} else {
						boolean selected = getViewer().getSelection().enableShortcutArmy(army_number);
						if (selected && event.clicks() > 1) {
							var set = getViewer().getSelection().getCurrentSelection().getSet();
							if (!set.isEmpty()) {
								var s = set.iterator().next();
								getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), (GameCamera)getCamera(), s.getPositionX(), s.getPositionY()));
							}
						}
					}
				}
				return true;
			case RETURN:
					if (!chat_visible)
						chat_form.setReceivers(!event.shiftDown());
				return true;
			case B:
				if (event.controlDown() && !map_mode && !observer) {
					getGUIRoot().pushDelegate(new BeaconDelegate(getViewer(), (GameCamera)getCamera()));
				}
				return true;
			case N:
				nextIdlePeon();
				return true;
			case F:
			case Z:
				if (!map_mode)
					return super.keyPressed(event);
				return true;
			default:
				if (map_mode || observer) {
					return super.keyPressed(event);
				} else {
					if (getActionButtonPanel().doKeyPressed(event)) {
						return true;
					}
					return super.keyPressed(event);
				}
		}
	}

	private void changeGamespeed(int delta) {
		getViewer().getPeerHub().getPlayerInterface().changePreferredGamespeed(delta);
	}

	private void nextIdlePeon() {
		var set = getViewer().getLocalPlayer().getUnits().getSet();

		boolean has_idle_peon = false;
		int lowest_name = Integer.MAX_VALUE;
		Selectable<?> lowest_peon = null;

		boolean has_greater_name = false;
		int lowest_greater_name = Integer.MAX_VALUE;
		Selectable<?> lowest_greater_peon = null;
		for (var s : set) {
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

		Selectable<?> target = null;
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
	public boolean keyRepeat(@NonNull KeyboardEvent event) {
//		getCamera().keyRepeat(event);
		switch (event.keyChar()) {
			case '+':
				changeGamespeed(1);
				return true;
			case '-':
				changeGamespeed(-1);
				return true;
			default:
				if (!map_mode && !observer && getActionButtonPanel().doKeyRepeat(event)) {
					return true;
				}
				return super.keyRepeat(event);
		}
	}

	@Override
	public boolean keyReleased(@NonNull KeyboardEvent event) {
		if (getCamera().keyReleased(event)) return true;
        switch (event.keyCode()) {
            case RETURN -> {
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
				return true;
            }
            default -> {
                if (!map_mode && !observer && getActionButtonPanel().doKeyReleased(event)) {
					return true;
				}
                return super.keyReleased(event);
            }
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

	private void updateSelection(@NonNull List<@NonNull Selectable<UnitTemplate>> friendly_units, Selectable<BuildingTemplate> friendly_building, Selectable<?> enemy) {
		Army current_selection = getViewer().getSelection().getCurrentSelection();
		Selectable<?> first = current_selection.getSet().iterator().next();
		if (first instanceof Building || first.getOwner() != getViewer().getLocalPlayer()) {
			if (first == friendly_building || first == enemy) {
				current_selection.clear();
			}
			return;
		}

		boolean add = false;
        for (Selectable<?> selectable : friendly_units) {
            if (!current_selection.contains(selectable)) {
                add = true;
                break;
            }
        }
        for (Selectable<?> selectable : friendly_units) {
            if (add) {
                if (!current_selection.contains(selectable))
                    current_selection.add(selectable);
            } else {
                current_selection.remove(selectable);
            }
        }
	}

	private void replaceSelection(@NonNull List<Selectable<UnitTemplate>> friendly_units, @Nullable Selectable<BuildingTemplate> friendly_building, @Nullable Selectable<?> enemy) {
		Army current_selection = getViewer().getSelection().getCurrentSelection();
		current_selection.clear();
		if (!friendly_units.isEmpty()) {
            for (Selectable<?> friendlyUnit : friendly_units) {
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
				Selectable<?>[] picked = getViewer().getPicker().pickBoxed(getViewer().getGUIRoot().getDelegate().getCamera().getState(), selection_x1, selection_y1, selection_x2, selection_y2, clicks);
				List<Selectable<UnitTemplate>> friendly_units = new ArrayList<>();
				Selectable<BuildingTemplate> friendly_building = null;
				Selectable<?> enemy = null;
                for (Selectable<?> selectable : picked) {
                    if (selectable != null) {
                        if (selectable.getOwner() == getViewer().getLocalPlayer()) {
                            if (selectable instanceof Building building)
                                friendly_building = building;
                            else if (selectable instanceof Unit unit)
                                friendly_units.add(unit);
                            else
                                throw new RuntimeException();
                        } else {
                            enemy = selectable;
                        }
                    }
                }
				if (Renderer.getLocalInput().isShiftDownCurrently() && getViewer().getSelection().getCurrentSelection().size() > 0)
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
				var localInput = Renderer.getLocalInput();
                switch (button) {
                    case LEFT:
                        if (!localInput.isKeyDown(Key.SPACE)) {
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
            
            float thickness = com.oddlabs.tt.global.Settings.getSettings().high_contrast ? 3.0f : 1.0f;

            // Ensure thickness doesn't exceed half dimensions
            if (thickness > w / 2) thickness = w / 2;
            if (thickness > h / 2) thickness = h / 2;

			renderer.drawColoredQuad(minX, minY, w, thickness, SELECTION_COLOR);
			renderer.drawColoredQuad(minX, maxY - thickness, w, thickness, SELECTION_COLOR);
			renderer.drawColoredQuad(minX, minY + thickness, thickness, h - 2 * thickness, SELECTION_COLOR);
			renderer.drawColoredQuad(maxX - thickness, minY + thickness, thickness, h - 2 * thickness, SELECTION_COLOR);
		}
	}

	@Override
	public void displayChangedNotify(int width, int height) {
		super.displayChangedNotify(width, height);
		observer_label.setPos((width - observer_label.getWidth())/2, height - observer_label.getHeight());
	}
}
