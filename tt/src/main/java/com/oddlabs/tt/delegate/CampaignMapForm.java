package com.oddlabs.tt.delegate;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MapIslandButton;
import com.oddlabs.tt.gui.MapIslandData;
import com.oddlabs.tt.gui.ModeIconQuads;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.player.campaign.Campaign;
import com.oddlabs.tt.player.campaign.CampaignState;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/** presents campaign map allowing island selection */
public final class CampaignMapForm extends CameraDelegate<StaticCamera> implements Animated {
	private static final float BASE_WIDTH = 800f;
	private static final float BASE_HEIGHT = 600f;
	private static final ResourceBundle bundle = ResourceBundle.getBundle(CampaignMapForm.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

    private final float scale_x;
    private final float scale_y;

	private final @NonNull Campaign campaign;
	private final @NonNull NetworkSelector network;
    private final List<MapIslandButton> islandButtons = new ArrayList<>();
    private boolean initialFocusSet = false;
    
    private float flicker_time;
    private final Vector4f mapColor = new Vector4f(1f, 1f, 1f, 1f);

	public CampaignMapForm(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull Campaign campaign) {
		super(gui_root, new StaticCamera(new CameraState()));
		this.campaign = campaign;
		this.network = network;

		this.scale_x = gui_root.getWidth() / BASE_WIDTH;
		this.scale_y = gui_root.getHeight() / BASE_HEIGHT;

		switch (campaign.getState().getRace()) {
			case CampaignState.RACE_VIKINGS -> {
				if (campaign.getState().getIslandState(10) != CampaignState.ISLAND_HIDDEN) {
					addChild(campaign.getIcons().getHiddenRoutes()[0]);
					addChild(campaign.getIcons().getHiddenRoutes()[1]);
				}

				if (campaign.getState().getCurrentIsland() == 14) {
					final Runnable runnable_menu = () -> closeCampaign(network, gui_root.getGUI());
					final Runnable runnable_next = () -> {
						CampaignDialogForm dialog = new CampaignDialogForm(i18n("native_campaign_opened_header"),
								i18n("native_campaign_opened"),
								null,
								Origin.AT_START,
								runnable_menu);
						gui_root.addModalForm(dialog);
					};
					CampaignDialogForm dialog = new CampaignDialogForm(i18n("viking_header"),
							i18n("viking_campaign_completed"),
							campaign.getIcons().getFaces()[0],
							Origin.AT_START,
							runnable_next);
					gui_root.addModalForm(dialog);
					Settings.getSettings().has_native_campaign = true;
				}
			}

			case CampaignState.RACE_NATIVES -> {
				if (campaign.getState().getIslandState(7) != CampaignState.ISLAND_HIDDEN) {
				addChild(campaign.getIcons().getHiddenRoutes()[0]);
				}

				if (campaign.getState().getCurrentIsland() == 7) {
					Runnable runnable = () -> closeCampaign(network, gui_root.getGUI());
					CampaignDialogForm dialog = new CampaignDialogForm(i18n("native_header"),
							i18n("native_campaign_completed"),
							campaign.getIcons().getFaces()[0],
							Origin.AT_START,
							runnable);
					gui_root.addModalForm(dialog);
				}
			}
		}

		// Islands
		for (int i = 0; i < campaign.getIcons().getNumIslands(); i++) {
			MapIslandData data = campaign.getIcons().getMapIslandData(i);
			int state = campaign.getState().getIslandState(i);
			GUIObject island = switch (state) {
                case CampaignState.ISLAND_AVAILABLE -> {
                    final int index = i;
                    MapIslandButton button = new MapIslandButton(data.button(), "", index);
                    button.addMouseClickListener((_, _, _, _) -> campaign.islandChosen(network, getGUIRoot(), index));
                    addChild(button);
                    islandButtons.add(button);
                    if (campaign.getState().getCurrentIsland() == i) {
                        button.setFocus();
                    }
					yield button;
                }
                case CampaignState.ISLAND_SEMI_AVAILABLE, CampaignState.ISLAND_UNAVAILABLE -> {
                    GUIObject icon = new GUIIcon(data.button().quad(ModeIconQuads.Mode.DISABLED));
                    addChild(icon);
					yield icon;
                }
                case CampaignState.ISLAND_COMPLETED -> {
                    GUIObject icon = new GUIIcon(data.button().quad(ModeIconQuads.Mode.NORMAL));
                    addChild(icon);
                    if (campaign.getState().getCurrentIsland() != i) {
                        GUIIcon flag = new GUIIcon(data.flag());
                        flag.setPos(data.pinX(), data.pinY());
                        addChild(flag);
                    } else {
                        GUIIcon boat = new GUIIcon(data.boat());
                        boat.setPos(data.pinX(), data.pinY());
                        addChild(boat);
                    }
					yield icon;
                }
                case CampaignState.ISLAND_HIDDEN -> null;
                default -> throw new IllegalArgumentException("Unexpected island state: " + state);
            };
			if (island != null)
				island.setPos(data.x(), data.y());
		}
        
        setFocus();
	}

	@Override
	public void setFocus(@NonNull FocusDirection direction) {
		if (direction == FocusDirection.BACKWARD) {
			super.setFocus(direction);
		} else {
			setFocus();
		}
	}

    @Override
    public void setFocus() {
        if (islandButtons.isEmpty()) {
            super.setFocus();
            return;
        }

        // If we already have a focused button among our islands, keep it.
        if (getFocusedChild() instanceof MapIslandButton) {
            return;
        }

        MapIslandButton toFocus = null;
        if (!initialFocusSet) {
            int currentIsland = campaign.getState().getCurrentIsland();
            for (MapIslandButton button : islandButtons) {
                if (button.getIslandIndex() == currentIsland) {
                    toFocus = button;
                    break;
                }
            }
            initialFocusSet = true;
        }

        if (toFocus == null) {
            toFocus = islandButtons.getLast();
        }

        toFocus.setFocus();
    }

	@Override
	public void handleInput(@NonNull InputEvent event) {
        if (!event.isConsumed() && event.getPhase() == InputPhase.PRESSED) {
            int dx = 0;
            int dy = 0;
            if (event.consumeAction(GameAction.UI_NAV_UP)) dy = -1;
            else if (event.consumeAction(GameAction.UI_NAV_DOWN)) dy = 1;
            else if (event.consumeAction(GameAction.UI_NAV_LEFT)) dx = -1;
            else if (event.consumeAction(GameAction.UI_NAV_RIGHT)) dx = 1;
            else if (event.consumeAction(GameAction.UI_FOCUS_NEXT)) {
                focusNext();
                event.consume();
                return;
            } else if (event.consumeAction(GameAction.UI_FOCUS_PREV)) {
                focusPrior();
                event.consume();
                return;
            } else if (event.consumeAction(GameAction.UI_ACTIVATE)) {
                if (getFocusedChild() instanceof MapIslandButton button) {
                    campaign.islandChosen(network, getGUIRoot(), button.getIslandIndex());
                    event.consume();
                    return;
                }
            }

            if (dx != 0 || dy != 0) {
                navigate(dx, dy);
                event.consume();
                return;
            }
        }

		if (event.getPhase() == InputPhase.PRESSED) {
			if (event.consumeAction(GameAction.GLOBAL_MENU) || event.consumeAction(GameAction.UI_CANCEL)) {
				getGUIRoot().addModalForm(new CampaignMapMenu(network, getGUIRoot()));
				event.consume();
				return;
			}
		}
		super.handleInput(event);
	}

    private void navigate(int dx, int dy) {
        if (islandButtons.isEmpty()) return;

        MapIslandButton current = null;
        if (getFocusedChild() instanceof MapIslandButton b && islandButtons.contains(b)) {
            current = b;
        }

        if (current == null) {
            islandButtons.getLast().setFocus();
            return;
        }

        MapIslandButton best = null;
        float bestScore = Float.MAX_VALUE;

        for (MapIslandButton candidate : islandButtons) {
            if (candidate == current) continue;

            float cdx = candidate.getX() - current.getX();
            float cdy = candidate.getY() - current.getY();

            boolean inDir = false;
            if (dy < 0) inDir = cdy < -1 && Math.abs(cdx) < Math.abs(cdy) * 2;
            else if (dy > 0) inDir = cdy > 1 && Math.abs(cdx) < Math.abs(cdy) * 2;
            else if (dx < 0) inDir = cdx < -1 && Math.abs(cdy) < Math.abs(cdx) * 2;
            else if (dx > 0) inDir = cdx > 1 && Math.abs(cdy) < Math.abs(cdx) * 2;

            if (inDir) {
                float distSq = cdx*cdx + cdy*cdy;
                if (distSq < bestScore) {
                    bestScore = distSq;
                    best = candidate;
                }
            }
        }

        if (best != null) {
            best.setFocus();
        }
    }

	public static void closeCampaign(@NonNull NetworkSelector network, @NonNull GUI gui) {
		Renderer.startMenu(network, gui);
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		renderer.drawIcon(campaign.getIcons().getMap(), 0f, 0f, mapColor);
	}

    @Override
    protected void doAdd() {
        super.doAdd();
        LocalEventQueue.getQueue().getManager().registerAnimation(this);
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        LocalEventQueue.getQueue().getManager().removeAnimation(this);
    }

    @Override
    public void animate(float t) {
        flicker_time += t;
        float flicker = 0.9f + (float) (0.0375 * Math.sin(flicker_time * 4.5) + 0.0375 * Math.sin(flicker_time * 10.35));
        mapColor.set(flicker, flicker, flicker, 1f);
    }

    @Override
    protected void render(@NonNull GUIRenderer renderer, float clip_left, float clip_right, float clip_bottom, float clip_top) {
        renderer.getMatrixStack().push();
        renderer.getMatrixStack().scale(scale_x, scale_y, 1f);
        super.render(renderer, clip_left, clip_right, clip_bottom, clip_top);
        renderer.getMatrixStack().pop();
    }

    @Override
    protected GUIObject pick(float x, float y) {
        return super.pick(x / scale_x, y / scale_y);
    }
}
