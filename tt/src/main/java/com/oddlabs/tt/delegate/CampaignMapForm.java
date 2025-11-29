package com.oddlabs.tt.delegate;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MapIslandData;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.NonFocusIconButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.player.campaign.Campaign;
import com.oddlabs.tt.player.campaign.CampaignState;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

import java.util.ResourceBundle;

public final class CampaignMapForm extends CameraDelegate {
	private static final int base_width = 800;
	private static final int base_height = 600;

	private final @NonNull Campaign campaign;
	private final @NonNull NetworkSelector network;

	public CampaignMapForm(final @NonNull NetworkSelector network, final @NonNull GUIRoot gui_root, @NonNull Campaign campaign) {
		super(gui_root, new StaticCamera(new CameraState()));
		this.campaign = campaign;
		this.network = network;
		final ResourceBundle bundle = ResourceBundle.getBundle(CampaignMapForm.class.getName());

		float scale_x = LocalInput.getViewWidth()/(float)base_width;
		float scale_y = LocalInput.getViewHeight()/(float)base_height;
		setScale(scale_x, scale_y);

		if (campaign.getState().getRace() == CampaignState.RACE_VIKINGS) {
			if (campaign.getState().getIslandState(10) != CampaignState.ISLAND_HIDDEN) {
				addChild(campaign.getIcons().getHiddenRoutes()[0]);
				addChild(campaign.getIcons().getHiddenRoutes()[1]);
			}

			if (campaign.getState().getCurrentIsland() == 14) {
				final Runnable runnable_menu = () -> closeCampaign(network, gui_root.getGUI());
				final Runnable runnable_next = () -> {
                                    CampaignDialogForm dialog = new CampaignDialogForm(Utils.getBundleString(bundle, "native_campaign_opened_header"),
                                            Utils.getBundleString(bundle, "native_campaign_opened"),
                                            null,
                                            Origin.AT_START,
                                            runnable_menu);
                                    gui_root.addModalForm(dialog);
                                };
				CampaignDialogForm dialog = new CampaignDialogForm(Utils.getBundleString(bundle, "viking_header"),
						Utils.getBundleString(bundle, "viking_campaign_completed"),
						campaign.getIcons().getFaces()[0],
						Origin.AT_START,
						runnable_next);
				gui_root.addModalForm(dialog);
				Settings.getSettings().has_native_campaign = true;
			}
		}
		if (campaign.getState().getRace() == CampaignState.RACE_NATIVES) {
			if (campaign.getState().getIslandState(7) != CampaignState.ISLAND_HIDDEN) {
				addChild(campaign.getIcons().getHiddenRoutes()[0]);
			}

			if (campaign.getState().getCurrentIsland() == 7) {
				Runnable runnable = () -> closeCampaign(network, gui_root.getGUI());
				CampaignDialogForm dialog = new CampaignDialogForm(Utils.getBundleString(bundle, "native_header"),
						Utils.getBundleString(bundle, "native_campaign_completed"),
						campaign.getIcons().getFaces()[0],
						Origin.AT_START,
						runnable);
				gui_root.addModalForm(dialog);
			}
		}
		// Islands
		for (int i = 0; i < campaign.getIcons().getNumIslands(); i++) {
			MapIslandData data = campaign.getIcons().getMapIslandData(i);
			int state = campaign.getState().getIslandState(i);
			GUIObject island;
			switch (state) {
				case CampaignState.ISLAND_AVAILABLE:
					island = new NonFocusIconButton(data.getButton(), "");
					island.addMouseClickListener(new IslandClickListener(i));
					addChild(island);
					break;
				case CampaignState.ISLAND_SEMI_AVAILABLE:
				case CampaignState.ISLAND_UNAVAILABLE:
					island = new GUIIcon(data.getButton()[Skin.DISABLED]);
					addChild(island);
					break;
				case CampaignState.ISLAND_COMPLETED:
					island = new GUIIcon(data.getButton()[Skin.NORMAL]);
					addChild(island);
					if (campaign.getState().getCurrentIsland() != i) {
						GUIIcon flag = new GUIIcon(data.getFlag());
						flag.setPos(data.getPinX(), data.getPinY());
						addChild(flag);
					} else {
						GUIIcon boat = new GUIIcon(data.getBoat());
						boat.setPos(data.getPinX(), data.getPinY());
						addChild(boat);
					}
					break;
				case CampaignState.ISLAND_HIDDEN:
					island = null;
					break;
				default:
					throw new RuntimeException();
			}
			if (island != null)
				island.setPos(data.getX(), data.getY());
		}
	}

	@Override
	protected void keyPressed(@NonNull KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_ESCAPE:
				getGUIRoot().pushDelegate(new CampaignMapMenu(network, getGUIRoot(), new StaticCamera(getCamera().getState())));
				break;
			default:
				super.keyPressed(event);
				break;
		}
	}

	public static void closeCampaign(@NonNull NetworkSelector network, @NonNull GUI gui) {
		Renderer.startMenu(network, gui);
	}

	@Override
	public boolean forceRender() {
		return true;
	}

	@Override
	protected void renderGeometry() {
		campaign.getIcons().getMap().render(0, 0);
//		campaign.extraRender();
	}

	/*
	protected final void keyPressed(KeyboardEvent event) {
		if (event.getKeyCode() == Keyboard.KEY_ESCAPE) {
		} else {
			super.keyPressed(event);
		}
	}
	*/
	private final class IslandClickListener implements MouseClickListener {
		private final int number;

		public IslandClickListener(int number) {
			this.number = number;
		}

		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			campaign.islandChosen(network, getGUIRoot(), number);
		}
	}
}
