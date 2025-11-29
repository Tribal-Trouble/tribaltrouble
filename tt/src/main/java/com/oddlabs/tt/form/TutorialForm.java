package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.MainMenu;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.net.WorldInitAction;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.tutorial.BuildingChieftainTrigger;
import com.oddlabs.tt.tutorial.PlacingDelegateTrigger;
import com.oddlabs.tt.tutorial.ScrollTrigger;
import com.oddlabs.tt.tutorial.SelectArmoryTrigger;
import com.oddlabs.tt.tutorial.SelectTowerTrigger;
import com.oddlabs.tt.tutorial.Tutorial;
import com.oddlabs.tt.tutorial.TutorialInGameInfo;
import com.oddlabs.tt.tutorial.TutorialOverTrigger;
import com.oddlabs.tt.tutorial.TutorialTrigger;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public final class TutorialForm extends Form {
	public static final int TUTORIAL_CAMERA = 1;
	public static final int TUTORIAL_QUARTERS = 2;
	public static final int TUTORIAL_ARMORY = 3;
	public static final int TUTORIAL_TOWER = 4;
	public static final int TUTORIAL_CHIEFTAIN = 5;
	public static final int TUTORIAL_BATTLE = 6;

	public static final int NUM_TUTORIALS = 6;

	private static final ResourceBundle bundle = ResourceBundle.getBundle(TutorialForm.class.getName());

	private final GUIRoot gui_root;
	private final NetworkSelector network;

	private static @NonNull String formatTutorial(int tutorial_number) {
		return Utils.getBundleString(bundle, "tutorial", Integer.toString(tutorial_number));
	}

	public TutorialForm(NetworkSelector network, GUIRoot gui_root) {
		this.gui_root = gui_root;
		this.network = network;
		Label headline = new Label(Utils.getBundleString(bundle, "tutorial_caption"), Skin.getSkin().getHeadlineFont());
		addChild(headline);

		HorizButton button_tutorial1 = new HorizButton(formatTutorial(TUTORIAL_CAMERA), 120);
		button_tutorial1.addMouseClickListener(new TutorialListener(TUTORIAL_CAMERA));
		addChild(button_tutorial1);
		Label label_tutorial1 = new Label(Utils.getBundleString(bundle, "tutorial1_tip"), Skin.getSkin().getEditFont());
		addChild(label_tutorial1);

		HorizButton button_tutorial2 = new HorizButton(formatTutorial(TUTORIAL_QUARTERS), 120);
		button_tutorial2.addMouseClickListener(new TutorialListener(TUTORIAL_QUARTERS));
		addChild(button_tutorial2);
		Label label_tutorial2 = new Label(Utils.getBundleString(bundle, "tutorial2_tip"), Skin.getSkin().getEditFont());
		addChild(label_tutorial2);

		HorizButton button_tutorial3 = new HorizButton(formatTutorial(TUTORIAL_ARMORY), 120);
		button_tutorial3.addMouseClickListener(new TutorialListener(TUTORIAL_ARMORY));
		addChild(button_tutorial3);
		Label label_tutorial3 = new Label(Utils.getBundleString(bundle, "tutorial3_tip"), Skin.getSkin().getEditFont());
		addChild(label_tutorial3);

		HorizButton button_tutorial4 = new HorizButton(formatTutorial(TUTORIAL_TOWER), 120);
		button_tutorial4.addMouseClickListener(new TutorialListener(TUTORIAL_TOWER));
		addChild(button_tutorial4);
		Label label_tutorial4 = new Label(Utils.getBundleString(bundle, "tutorial4_tip"), Skin.getSkin().getEditFont());
		addChild(label_tutorial4);

		HorizButton button_tutorial5 = new HorizButton(formatTutorial(TUTORIAL_CHIEFTAIN), 120);
		button_tutorial5.addMouseClickListener(new TutorialListener(TUTORIAL_CHIEFTAIN));
		addChild(button_tutorial5);
		Label label_tutorial5 = new Label(Utils.getBundleString(bundle, "tutorial5_tip"), Skin.getSkin().getEditFont());
		addChild(label_tutorial5);

		HorizButton button_tutorial6 = new HorizButton(formatTutorial(TUTORIAL_BATTLE), 120);
		button_tutorial6.addMouseClickListener(new TutorialListener(TUTORIAL_BATTLE));
		addChild(button_tutorial6);
		Label label_tutorial6 = new Label(Utils.getBundleString(bundle, "tutorial6_tip"), Skin.getSkin().getEditFont());
		addChild(label_tutorial6);

		HorizButton cancel_button = new CancelButton(120);
		addChild(cancel_button);
		cancel_button.addMouseClickListener( (_, _, _, _) -> this.cancel());

		// Place objects
		headline.place();
		button_tutorial1.place(headline, BOTTOM_LEFT);
		label_tutorial1.place(button_tutorial1, RIGHT_MID);
		button_tutorial2.place(button_tutorial1, BOTTOM_LEFT);
		label_tutorial2.place(button_tutorial2, RIGHT_MID);
		button_tutorial3.place(button_tutorial2, BOTTOM_LEFT);
		label_tutorial3.place(button_tutorial3, RIGHT_MID);
		button_tutorial4.place(button_tutorial3, BOTTOM_LEFT);
		label_tutorial4.place(button_tutorial4, RIGHT_MID);
		button_tutorial5.place(button_tutorial4, BOTTOM_LEFT);
		label_tutorial5.place(button_tutorial5, RIGHT_MID);
		button_tutorial6.place(button_tutorial5, BOTTOM_LEFT);
		label_tutorial6.place(button_tutorial6, RIGHT_MID);
		cancel_button.place(Origin.AT_END);

		// headline
		compileCanvas();
		centerPos();
	}

	private static final class TutorialAction implements WorldInitAction {
		private final TriggerFactory factory;
		private final TutorialInGameInfo ingame_info;

		public TutorialAction(TriggerFactory factory, TutorialInGameInfo ingame_info) {
			this.factory = factory;
			this.ingame_info = ingame_info;
		}

		@Override
		public void run(WorldViewer viewer) {
			new Tutorial(viewer, ingame_info, factory.create(viewer));
		}
	}

	private interface TriggerFactory {
		TutorialTrigger create(WorldViewer viewer);
	}

	private static void startNewGame(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, TriggerFactory factory, int tutorial_num) {
		TutorialInGameInfo ingame_info = new TutorialInGameInfo();
		GameNetwork game_network = doStartNewGame(network, gui_root, ingame_info, new TutorialAction(factory, ingame_info), Player.INITIAL_UNIT_COUNT, tutorial_num);
		game_network.getClient().getServerInterface().setPlayerSlot(0, PlayerSlot.HUMAN, RacesResources.RACE_NATIVES, 0, true, PlayerSlot.AI_NONE);
		game_network.getClient().getServerInterface().startServer();
	}

	private static @NonNull GameNetwork doStartNewGame(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull TutorialInGameInfo ingame_info, final @Nullable WorldInitAction initial_action, int initial_unit_count, int tutorial_num) {
		int size = 256;
		float hills = 1f;
		float trees = 1f;
		float resources = 1f;
		int seed = 2;
		String ai_string = Utils.getBundleString(bundle, "ai");
		WorldInitAction compound_action = (WorldViewer world_viewer) -> {
                    if (initial_action != null)
                        initial_action.run(world_viewer);
                    MainMenu.completeGameSetupHack(world_viewer);
                };
		return MainMenu.startNewGame(network, gui_root, null, new WorldParameters(Game.GAMESPEED_NORMAL, "Tutorial" + tutorial_num, initial_unit_count, Player.DEFAULT_MAX_UNIT_COUNT), ingame_info, compound_action, null, size, Landscape.TerrainType.NATIVE, hills, trees, 0f, seed, new String[]{ai_string + "0", ai_string + "1", ai_string + "2", ai_string + "3", ai_string + "4", ai_string + "5"});
	}

	public static boolean checkTutorial(GUIRoot gui_root, int tutorial_number) {
		return true;
	}

	public static void startTutorial(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, int tutorial_number) {
		final TutorialInGameInfo ingame_info;
		GameNetwork game_network;
		switch (tutorial_number) {
			case TUTORIAL_CAMERA:
				startNewGame(network, gui_root, (WorldViewer viewer) -> new ScrollTrigger(viewer.getLocalPlayer()), 1);
				break;
			case TUTORIAL_QUARTERS:
				startNewGame(network, gui_root, (WorldViewer viewer) -> new PlacingDelegateTrigger(viewer.getLocalPlayer()), 2);
				break;
			case TUTORIAL_ARMORY:
				startNewGame(network, gui_root, (WorldViewer viewer) -> new SelectArmoryTrigger(viewer.getLocalPlayer()), 3);
				break;
			case TUTORIAL_TOWER:
				ingame_info = new TutorialInGameInfo();
				WorldInitAction action = (WorldViewer viewer) -> {
                                    Player player = viewer.getLocalPlayer();
                                    new Unit(player, player.getStartX(), player.getStartY(), null, player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
                                    new Tutorial(viewer, ingame_info, new SelectTowerTrigger(viewer.getLocalPlayer()));
                };
				game_network = doStartNewGame(network, gui_root, ingame_info, action, 10, 4);
				game_network.getClient().getServerInterface().setPlayerSlot(0, PlayerSlot.HUMAN, RacesResources.RACE_NATIVES, 0, true, PlayerSlot.AI_TOWER_TUTORIAL);
				game_network.getClient().setUnitInfo(0, new UnitInfo(false, false, 0, false, 10, 0, 0, 0));
				game_network.getClient().getServerInterface().setPlayerSlot(1, PlayerSlot.AI, RacesResources.RACE_VIKINGS, 1, true, PlayerSlot.AI_TOWER_TUTORIAL);
				game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
				game_network.getClient().getServerInterface().startServer();
				break;
			case TUTORIAL_CHIEFTAIN:
				ingame_info = new TutorialInGameInfo();
				game_network = doStartNewGame(network, gui_root, ingame_info, new TutorialAction((WorldViewer viewer) -> new BuildingChieftainTrigger(viewer.getLocalPlayer()), ingame_info), Player.INITIAL_UNIT_COUNT, 5);
				game_network.getClient().getServerInterface().setPlayerSlot(0, PlayerSlot.HUMAN, RacesResources.RACE_NATIVES, 0, true, PlayerSlot.AI_NONE);
				game_network.getClient().getServerInterface().setPlayerSlot(1, PlayerSlot.AI, RacesResources.RACE_VIKINGS, 1, true, PlayerSlot.AI_CHIEFTAIN_TUTORIAL);
				game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
				game_network.getClient().getServerInterface().startServer();
				break;
			case TUTORIAL_BATTLE:
				ingame_info = new TutorialInGameInfo();
				game_network = doStartNewGame(network, gui_root, ingame_info, new TutorialAction((WorldViewer _) -> new TutorialOverTrigger(), ingame_info), Player.INITIAL_UNIT_COUNT, 6);
				game_network.getClient().getServerInterface().setPlayerSlot(0, PlayerSlot.HUMAN, RacesResources.RACE_NATIVES, 0, true, PlayerSlot.AI_NONE);
				game_network.getClient().getServerInterface().setPlayerSlot(1, PlayerSlot.AI, RacesResources.RACE_VIKINGS, 1, true, PlayerSlot.AI_BATTLE_TUTORIAL);
				game_network.getClient().setUnitInfo(1, new UnitInfo(true, true, 0, false, 0, 15, 0, 0));
				game_network.getClient().getServerInterface().startServer();
				break;
			default:
				throw new RuntimeException();
		}
	}

	private final class TutorialListener implements MouseClickListener {
		private final int number;

		public TutorialListener(int number) {
			this.number = number;
		}

		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			if (checkTutorial(gui_root, number)) {
				startTutorial(network, gui_root, number);
				TutorialForm.this.remove();
			}
		}
	}
}
