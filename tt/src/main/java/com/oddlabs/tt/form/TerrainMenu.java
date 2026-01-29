package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.registration.RegistrationKey;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.ValueListener;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.DefaultInGameInfo;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.MultiplayerInGameInfo;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.Random;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;
import static com.oddlabs.tt.gui.Placement.TOP_MID;

public final class TerrainMenu extends Group {
	private static final int[] SIZES = new int[]{256, 512, 1024};

	private static final int SLIDER_LENGTH = 250;
	private static final int BUTTON_WIDTH = 100;
	private static final int SLIDER_MAX_VALUE = 10;

	private static final String SEED_CARDINALITY = "40000";
	private static final int SLIDER_CARDINALITY = 11;
	private static final int TERRAIN_TYPE_CARDINALITY = 2;
	private static final int SIZE_CARDINALITY = 3;
	private static final int DIFFICULTY_CARDINALITY = 4;
	private static final int RACE_CARDINALITY = 2;
	private static final int TEAM_CARDINALITY = 6;
	private static final @NonNull BigInteger MAX_VALUE;

	private final @Nullable Menu main_menu;
	private final @Nullable TerrainMenuListener owner;

	private final @NonNull PulldownMenu<Void> pulldown_size;
	private final @NonNull EditLine editline_name;
	private final @NonNull PulldownMenu<Void> pm_terrain_type;
	private final @NonNull Slider slider_hills;
	private final @NonNull Slider slider_vegetation;
	private final @NonNull Slider slider_supplies;
	private final @NonNull Label label_mapcode;
	private final @NonNull HorizButton button_ok;
    private final @NonNull PulldownMenu<Void> @NonNull [] difficulty_pulldown_menus;
	private final @NonNull PulldownMenu<Void> @NonNull [] race_pulldown_menus;
	private final @NonNull PulldownMenu<Void> @NonNull [] team_pulldown_menus;
    private final @NonNull PulldownButton @NonNull [] race_pulldown_buttons;
	private final @NonNull PulldownButton @NonNull [] team_pulldown_buttons;
	private final @NonNull Label @NonNull [] labels_players;
	private final @NonNull CheckBox cb_rated;
	private final boolean multiplayer;
	private final @NonNull PulldownMenu<Void> pm_gamespeed;
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(TerrainMenu.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}
	private final @NonNull GUIRoot gui_root;
	private final @NonNull NetworkSelector network;
	private int seed;

	static {
		BigInteger max = BigInteger.ONE;
		max = max.multiply(new BigInteger(SEED_CARDINALITY));
		max = max.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		max = max.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		max = max.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		max = max.multiply(new BigInteger(new byte[]{TERRAIN_TYPE_CARDINALITY}));
		max = max.multiply(new BigInteger(new byte[]{SIZE_CARDINALITY}));
		max = max.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));
		max = max.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));
		for (int i = 1; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
			max = max.multiply(new BigInteger(new byte[]{DIFFICULTY_CARDINALITY}));
			max = max.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));
			max = max.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));
		}
		MAX_VALUE = max;
	}

	@SuppressWarnings("unchecked")
    public TerrainMenu(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @Nullable Menu main_menu, boolean multiplayer, @Nullable TerrainMenuListener owner) {
		this.network = network;
		this.main_menu = main_menu;
		this.multiplayer = multiplayer;
		this.owner = owner;
		this.gui_root = gui_root;

		// headline
		Label label_headline = new Label(i18n(multiplayer ? "new_game" : "skirmish"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);
		Panel standard = new Panel(i18n("standard_options"));
		Panel advanced = new Panel(i18n("advanced_options"));
		Group group_map_options = new Group();

		// game name
		Label label_name = new Label(i18n("game_name"), Skin.getSkin().getEditFont());
		Label label_default_name = null;
		editline_name = new EditLine(180, Game.MAX_LENGTH);
		if (multiplayer) {
			standard.addChild(label_name);
			String default_name = i18n("default_name", Network.getMatchmakingClient().getProfile().getNick());
			label_default_name = new Label(default_name, Skin.getSkin().getEditFont());
			editline_name.append(default_name);
			if (Renderer.isRegistered())
				standard.addChild(editline_name);
			else
				standard.addChild(label_default_name);
		}
		String rated_tip = i18n("rated_game_tip", GameSession.MIN_WINS_FOR_RANKING);
		cb_rated = new CheckBox(false, i18n("rated_game"), rated_tip);
		if (multiplayer ) {
			standard.addChild(cb_rated);
			cb_rated.setDisabled(Network.getMatchmakingClient().getProfile() == null || Network.getMatchmakingClient().getProfile().getWins() < GameSession.MIN_WINS_FOR_RANKING);
		}

		// gamespeed
		Group group_gamespeed = new Group();
		Label label_gamespeed = new Label(i18n("gamespeed"), Skin.getSkin().getEditFont());
		group_gamespeed.addChild(label_gamespeed);
		pm_gamespeed = new PulldownMenu<>();
		pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_SLOW)));
		pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_NORMAL)));
		pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_FAST)));
		pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_LUDICROUS)));
		PulldownButton pb_gamespeed = new PulldownButton(gui_root, pm_gamespeed, 1, 150);
		group_gamespeed.addChild(pb_gamespeed);
		label_gamespeed.place();
		pb_gamespeed.place(label_gamespeed, RIGHT_MID);
		group_gamespeed.compileCanvas();

		if (multiplayer) {
			group_map_options.addChild(group_gamespeed);
		}
		// size
		Group group_size = new Group();

		Label label_size = new Label(i18n("island_size"), Skin.getSkin().getEditFont());
		group_size.addChild(label_size);

		pulldown_size = new PulldownMenu<>();
		pulldown_size.addItem(new PulldownItem<>(ServerMessageBundler.getSizeString(Game.SIZE_SMALL)));
		pulldown_size.addItem(new PulldownItem<>(ServerMessageBundler.getSizeString(Game.SIZE_MEDIUM)));
		pulldown_size.addItem(new PulldownItem<>(ServerMessageBundler.getSizeString(Game.SIZE_LARGE)));

		PulldownButton pb_size = new PulldownButton(gui_root, pulldown_size, 1, 150);
		group_size.addChild(pb_size);
		label_size.place();
		pb_size.place(label_size, RIGHT_MID);
		group_size.compileCanvas();
		group_map_options.addChild(group_size);
		pulldown_size.addItemChosenListener(new PulldownUpdateMapcodeListener());

		// seed
		Label label_seed = new Label(i18n("map_code"), Skin.getSkin().getEditFont());
		label_mapcode = new Label("", Skin.getSkin().getHeadlineFont(), 250);

		Group group_seed = new Group();
		group_seed.addChild(label_seed);
		group_seed.addChild(label_mapcode);
		label_seed.place();
		label_mapcode.place(label_seed, RIGHT_MID);
		group_seed.compileCanvas();
		advanced.addChild(group_seed);

		// terrain_type
		Group group_terrain_type = new Group();
		Label label_terrain_type = new Label(i18n("terrain_type"), Skin.getSkin().getEditFont());
		group_terrain_type.addChild(label_terrain_type);
		pm_terrain_type = new PulldownMenu<>();
		pm_terrain_type.addItem(new PulldownItem<>(ServerMessageBundler.getTerrainTypeString(Game.TERRAIN_TYPE_NATIVE)));
		pm_terrain_type.addItem(new PulldownItem<>(ServerMessageBundler.getTerrainTypeString(Game.TERRAIN_TYPE_VIKING)));
		PulldownButton pb_terrain_type = new PulldownButton(gui_root, pm_terrain_type, 0, 150);
		group_terrain_type.addChild(pb_terrain_type);
		label_terrain_type.place();
		pb_terrain_type.place(label_terrain_type, RIGHT_MID);
		group_terrain_type.compileCanvas();
		pm_terrain_type.addItemChosenListener(new PulldownUpdateMapcodeListener());
		group_map_options.addChild(group_terrain_type);

		Group group_sliders = new Group();
		// hills
		Label label_hills_low = new Label(i18n("min"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_hills_low);
		Label label_hills_high = new Label(i18n("max"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_hills_high);
		Label label_hills = new Label(i18n("hills"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_hills);
		slider_hills = new Slider(SLIDER_LENGTH, 0, SLIDER_MAX_VALUE, SLIDER_MAX_VALUE/2);
		slider_hills.addValueListener(new SliderUpdateMapcodeListener());
		group_sliders.addChild(slider_hills);

		// vegetation
		Label label_vegetation_low = new Label(i18n("min"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_vegetation_low);
		Label label_vegetation_high = new Label(i18n("max"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_vegetation_high);
		Label label_vegetation = new Label(i18n("trees"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_vegetation);
		slider_vegetation = new Slider(SLIDER_LENGTH, 0, SLIDER_MAX_VALUE, SLIDER_MAX_VALUE/2);
		slider_vegetation.addValueListener(new SliderUpdateMapcodeListener());
		group_sliders.addChild(slider_vegetation);

		// supplies
		Label label_supplies_low = new Label(i18n("min"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_supplies_low);
		Label label_supplies_high = new Label(i18n("max"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_supplies_high);
		Label label_supplies = new Label(i18n("resources"), Skin.getSkin().getEditFont());
		group_sliders.addChild(label_supplies);
		slider_supplies = new Slider(SLIDER_LENGTH, 0, SLIDER_MAX_VALUE, SLIDER_MAX_VALUE/2);
		slider_supplies.addValueListener(new SliderUpdateMapcodeListener());
		group_sliders.addChild(slider_supplies);

		// supplies
		label_supplies.place();
		label_supplies_low.place(label_supplies, RIGHT_MID);
		slider_supplies.place(label_supplies_low, RIGHT_MID);
		label_supplies_high.place(slider_supplies, RIGHT_MID);
		// vegetation
		label_vegetation.place(label_supplies, TOP_LEFT, Skin.getSkin().getFormData().sectionSpacing());
		slider_vegetation.place(slider_supplies, TOP_MID, Skin.getSkin().getFormData().sectionSpacing());
		label_vegetation_low.place(slider_vegetation, LEFT_MID);
		label_vegetation_high.place(slider_vegetation, RIGHT_MID);
		// hills
		label_hills.place(label_vegetation, TOP_LEFT, Skin.getSkin().getFormData().sectionSpacing());
		slider_hills.place(slider_vegetation, TOP_MID, Skin.getSkin().getFormData().sectionSpacing());
		label_hills_low.place(slider_hills, LEFT_MID);
		label_hills_high.place(slider_hills, RIGHT_MID);
		// sliders
		group_sliders.compileCanvas();
		advanced.addChild(group_sliders);

		// races and teams
		Group group_race_team = new Group();
		labels_players = new Label[MatchmakingServerInterface.MAX_PLAYERS];
		difficulty_pulldown_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
		race_pulldown_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
		team_pulldown_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        PulldownButton [] difficulty_pulldown_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
		race_pulldown_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
		team_pulldown_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
		Random random = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick()*LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
		random.nextFloat();
		for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
			difficulty_pulldown_menus[i] = new PulldownMenu<>();
			race_pulldown_menus[i] = new PulldownMenu<>();
			team_pulldown_menus[i] = new PulldownMenu<>();

			if (i == 0) {
				difficulty_pulldown_menus[i].addItem(new PulldownItem<>(i18n("human")));
			} else {
				difficulty_pulldown_menus[i].addItem(new PulldownItem<>(i18n("closed")));
				difficulty_pulldown_menus[i].addItem(new PulldownItem<>(i18n("easy_ai")));
				difficulty_pulldown_menus[i].addItem(new PulldownItem<>(i18n("normal_ai")));
				PulldownItem<Void> hard = new PulldownItem<>(i18n("hard_ai"));
				difficulty_pulldown_menus[i].addItem(hard);
			}

			difficulty_pulldown_buttons[i] = new PulldownButton(gui_root, difficulty_pulldown_menus[i], 0, 115);
			group_race_team.addChild(difficulty_pulldown_buttons[i]);

			for (int j = 0; j < RacesResources.getNumRaces(); j++) {
				PulldownItem<Void> pulldown_item_race = new PulldownItem<>(RacesResources.getRaceName(j));
				race_pulldown_menus[i].addItem(pulldown_item_race);
			}

			race_pulldown_buttons[i] = new PulldownButton(gui_root, race_pulldown_menus[i], 0, 115);
			group_race_team.addChild(race_pulldown_buttons[i]);
			for (int j = 0; j < MatchmakingServerInterface.MAX_PLAYERS; j++) {
				String team_str = i18n("team", Integer.toString(j + 1));
				PulldownItem<Void> pulldown_item_team = new PulldownItem<>(team_str);
				team_pulldown_menus[i].addItem(pulldown_item_team);
			}
			team_pulldown_buttons[i] = new PulldownButton(gui_root, team_pulldown_menus[i], i, 115);
			group_race_team.addChild(team_pulldown_buttons[i]);
			if (i == 0) {
				String player_str = i18n("player", Integer.toString(1));
				labels_players[0] = new Label(player_str, Skin.getSkin().getEditFont())
						.setColor(Settings.getSettings().team_colours[0]);
				group_race_team.addChild(labels_players[0]);
				labels_players[0].place();
				difficulty_pulldown_buttons[0].place(labels_players[0], RIGHT_MID);
				race_pulldown_buttons[0].place(difficulty_pulldown_buttons[0], RIGHT_MID);
				team_pulldown_buttons[0].place(race_pulldown_buttons[0], RIGHT_MID);
			} else {
				String player_str = i18n("player", Integer.toString(i + 1));
				labels_players[i] = new Label(player_str, Skin.getSkin().getEditFont())
						.setColor(Settings.getSettings().team_colours[i]);
				group_race_team.addChild(labels_players[i]);
				labels_players[i].place(labels_players[i-1], BOTTOM_RIGHT);
				difficulty_pulldown_buttons[i].place(labels_players[i], RIGHT_MID);
				race_pulldown_buttons[i].place(difficulty_pulldown_buttons[i], RIGHT_MID);
				team_pulldown_buttons[i].place(race_pulldown_buttons[i], RIGHT_MID);
				difficulty_pulldown_menus[i].addItemChosenListener(new DisableListener(i));
			}
			difficulty_pulldown_menus[i].addItemChosenListener(new PulldownUpdateMapcodeListener());
			race_pulldown_menus[i].addItemChosenListener(new PulldownUpdateMapcodeListener());
			team_pulldown_menus[i].addItemChosenListener(new PulldownUpdateMapcodeListener());
		}
		if (!multiplayer) {
			group_race_team.compileCanvas();
			standard.addChild(group_race_team);
		}

		// buttons
		Group group_buttons = new Group();

		button_ok = new OKButton(BUTTON_WIDTH);
		button_ok.addMouseClickListener(new OKListener());
        HorizButton button_cancel = new CancelButton(BUTTON_WIDTH);
		button_cancel.addMouseClickListener(new CancelButtonListener());
        HorizButton button_mapcode = new HorizButton(i18n("enter_map_code"), 170);
		button_mapcode.addMouseClickListener(new MapcodeListener());

		group_buttons.addChild(button_mapcode);
		group_buttons.addChild(button_ok);
		group_buttons.addChild(button_cancel);

		button_cancel.place();
		button_ok.place(button_cancel, LEFT_MID);
		button_mapcode.place(button_ok, LEFT_MID);

		group_buttons.compileCanvas();
		addChild(group_buttons);

		// map options
		if (multiplayer) {
			group_gamespeed.place();
			group_size.place(group_gamespeed, BOTTOM_RIGHT);
		} else {
			group_size.place();
		}
		group_terrain_type.place(group_size, BOTTOM_RIGHT);
		group_map_options.compileCanvas();
		standard.addChild(group_map_options);

		// standard
		if (multiplayer) {
			label_name.place();
			if (Renderer.isRegistered())
				editline_name.place(label_name, RIGHT_MID);
			else
				label_default_name.place(label_name, RIGHT_MID);
			cb_rated.place(label_name, BOTTOM_LEFT, Skin.getSkin().getFormData().sectionSpacing());
			group_map_options.place(cb_rated, BOTTOM_LEFT);
		} else {
			group_map_options.place();
			group_race_team.place(group_map_options, BOTTOM_LEFT, Skin.getSkin().getFormData().sectionSpacing());
		}
		standard.compileCanvas();

		// advanced
		group_sliders.place();
		group_seed.place(group_sliders, BOTTOM_LEFT, Skin.getSkin().getFormData().sectionSpacing());
		advanced.compileCanvas();

		PanelGroup panel_group = new PanelGroup(standard, advanced);
		addChild(panel_group);

		// Place objects
		label_headline.place();
		panel_group.place(label_headline, BOTTOM_LEFT);

		// buttons
		group_buttons.place(Origin.AT_END);

		compileCanvas();
		randomize();

		// set standard game
		pulldown_size.addItemChosenListener(new PulldownUpdateSizeListener());
		pm_terrain_type.addItemChosenListener(new PulldownUpdateTerrainListener());
		for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
			difficulty_pulldown_menus[i].addItemChosenListener(new PulldownUpdateHardListener());
			if (i == 0) {
				team_pulldown_menus[i].chooseItem(0);
			} else {
				team_pulldown_menus[i].chooseItem(1);
			}
			if (i == 1) {
				difficulty_pulldown_menus[i].chooseItem(PlayerSlot.AI_EASY);
				race_pulldown_menus[i].chooseItem((race_pulldown_menus[0].getChosenItemIndex()+1)%2);
			} else {
				difficulty_pulldown_menus[i].chooseItem(0);
			}
		}
		pulldown_size.chooseItem(1);
		if (!Renderer.isRegistered())
			pm_terrain_type.chooseItem(0);
	}

	private void setMapcode() {
		BigInteger max_val = BigInteger.ONE;
		BigInteger result = BigInteger.ZERO;
		result = result.add((new BigInteger(""+seed)).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(SEED_CARDINALITY));
		int hills = slider_hills.getValue();
		result = result.add((new BigInteger(new byte[]{(byte)hills})).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		int vegetation_amount = slider_vegetation.getValue();
		result = result.add((new BigInteger(new byte[]{(byte)vegetation_amount})).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		int supplies_amount = slider_supplies.getValue();
		result = result.add((new BigInteger(new byte[]{(byte)supplies_amount})).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		int terrain_type = pm_terrain_type.getChosenItemIndex();
		result = result.add((new BigInteger(new byte[]{(byte)terrain_type})).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(new byte[]{TERRAIN_TYPE_CARDINALITY}));
		int size = pulldown_size.getChosenItemIndex();
		result = result.add((new BigInteger(new byte[]{(byte)size})).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(new byte[]{SIZE_CARDINALITY}));
		int player_race = race_pulldown_menus[0].getChosenItemIndex();
		result = result.add((new BigInteger(new byte[]{(byte)player_race})).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));
		int player_team = team_pulldown_menus[0].getChosenItemIndex();
		result = result.add((new BigInteger(new byte[]{(byte)player_team})).multiply(max_val));
		max_val = max_val.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));
		for (int i = 1; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
			int difficulty = difficulty_pulldown_menus[i].getChosenItemIndex();
			result = result.add((new BigInteger(new byte[]{(byte)difficulty})).multiply(max_val));
			max_val = max_val.multiply(new BigInteger(new byte[]{DIFFICULTY_CARDINALITY}));
			int race = race_pulldown_menus[i].getChosenItemIndex();
			result = result.add((new BigInteger(new byte[]{(byte)race})).multiply(max_val));
			max_val = max_val.multiply(new BigInteger(new byte[]{RACE_CARDINALITY}));
			int team = team_pulldown_menus[i].getChosenItemIndex();
			result = result.add((new BigInteger(new byte[]{(byte)team})).multiply(max_val));
			max_val = max_val.multiply(new BigInteger(new byte[]{TEAM_CARDINALITY}));
		}

		String code = RegistrationKey.createString(result);
		label_mapcode.clear();
		label_mapcode.append(code);
	}

	public void parseMapcode(@NonNull String text) {
		String code = text.toUpperCase();
		BigInteger result = RegistrationKey.parseBits(code);
		parseBigInteger(result);
		label_mapcode.clear();
		label_mapcode.append(code);
	}

	private void parseBigInteger(BigInteger result) {
		BigInteger max_val = MAX_VALUE;
		for (int i = MatchmakingServerInterface.MAX_PLAYERS - 1; i >= 1; i--) {
			result = result.mod(max_val);
			max_val = max_val.divide(new BigInteger(new byte[]{TEAM_CARDINALITY}));
			int team = result.divide(max_val).intValue();
			team_pulldown_menus[i].chooseItem(team);
			result = result.mod(max_val);
			max_val = max_val.divide(new BigInteger(new byte[]{RACE_CARDINALITY}));
			int race = result.divide(max_val).intValue();
			race_pulldown_menus[i].chooseItem(race);
			result = result.mod(max_val);
			max_val = max_val.divide(new BigInteger(new byte[]{DIFFICULTY_CARDINALITY}));
			int difficulty = result.divide(max_val).intValue();
			difficulty_pulldown_menus[i].chooseItem(difficulty);
			if (difficulty == 0) {
				labels_players[i].setDisabled(true);
				race_pulldown_buttons[i].setDisabled(true);
				team_pulldown_buttons[i].setDisabled(true);
			} else {
				labels_players[i].setDisabled(false);
				race_pulldown_buttons[i].setDisabled(false);
				team_pulldown_buttons[i].setDisabled(false);
			}
		}
		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(new byte[]{TEAM_CARDINALITY}));
		int player_team = result.divide(max_val).intValue();
		team_pulldown_menus[0].chooseItem(player_team);
		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(new byte[]{RACE_CARDINALITY}));
		int player_race = result.divide(max_val).intValue();
		race_pulldown_menus[0].chooseItem(player_race);
		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(new byte[]{SIZE_CARDINALITY}));
		int size = result.divide(max_val).intValue();
		pulldown_size.chooseItem(size);
		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(new byte[]{TERRAIN_TYPE_CARDINALITY}));
		int terrain_type = result.divide(max_val).intValue();
		pm_terrain_type.chooseItem(terrain_type);
		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		int supplies_amount = result.divide(max_val).intValue();
		slider_supplies.setValue(supplies_amount);
		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		int vegetation_amount = result.divide(max_val).intValue();
		slider_vegetation.setValue(vegetation_amount);

		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(new byte[]{SLIDER_CARDINALITY}));
		int hills = result.divide(max_val).intValue();
		slider_hills.setValue(hills);

		result = result.mod(max_val);
		max_val = max_val.divide(new BigInteger(SEED_CARDINALITY));
		seed = result.divide(max_val).intValue();
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	@Override
	public void setFocus(@NonNull FocusDirection direction) {
		super.setFocus(direction);
		switchFocus(direction);
	}

	public @NonNull GUIObject getButtonOK() {
		return button_ok;
	}

	private final class CancelButtonListener implements MouseClickListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			owner.terrainMenuCancel();
		}
	}

	private void randomize() {
		Random random = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick()*LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
		random.nextInt();
		BigInteger rand_int = new BigInteger(100, random);
		parseBigInteger(rand_int);
		setMapcode();
	}

	void doCancel() {
		if (multiplayer)
			new SelectGameMenu(network, gui_root, main_menu);
	}

	private boolean isChosen(@NonNull PulldownMenu<Void> menu) {
		return menu.getChosenItemIndex() != 0;
	}

	public boolean startGame() {
		int hills = slider_hills.getValue();
		int vegetation_amount = slider_vegetation.getValue();
		int supplies_amount = slider_supplies.getValue();
		Landscape.TerrainType terrain_type = Landscape.TerrainType.values()[pm_terrain_type.getChosenItemIndex()];
		Game game;
		boolean rated = cb_rated.isMarked();
		if (rated)
			team_pulldown_menus[0].chooseItem(team_pulldown_menus[0].getChosenItemIndex()%2);
		if (multiplayer) {
			String game_name = editline_name.getContents();
			if (game_name.length() < Game.MIN_LENGTH) {
				String min_name = i18n("min_name_length", Game.MIN_LENGTH);
				gui_root.addModalForm(new MessageForm(min_name));
				return false;
			}
			float random_start_pos = LocalEventQueue.getQueue().getTime()%1f;
			game = new Game(game_name, (byte)pulldown_size.getChosenItemIndex(), (byte)terrain_type.ordinal(), (byte)hills, (byte)vegetation_amount, (byte)supplies_amount, rated, (byte)(pm_gamespeed.getChosenItemIndex() + 1), label_mapcode.getContents(), random_start_pos, Player.DEFAULT_MAX_UNIT_COUNT);
		} else {
			boolean has_enemy = false;
			for (int i = 1; i < race_pulldown_menus.length; i++) {
                if (isChosen(difficulty_pulldown_menus[i]) && team_pulldown_menus[i].getChosenItemIndex() != team_pulldown_menus[0].getChosenItemIndex()) {
					has_enemy = true;
					break;
				}
            }
			if (!has_enemy) {
				String min_name = i18n("min_num_teams", 2);
				gui_root.addModalForm(new MessageForm(min_name));
				return false;
			}
			game = null;
		}
		if (owner != null)
			owner.terrainMenuOK();
		SelectGameMenu menu = null;
		if (multiplayer)
			menu = (SelectGameMenu)owner;
		int gametype;
		IO.println("hills = " + hills / (float)SLIDER_MAX_VALUE + " | vegetation_amount = " + vegetation_amount / (float)SLIDER_MAX_VALUE + " | supplies_amount = " + supplies_amount / (float)SLIDER_MAX_VALUE + " | seed = " + seed * seed);
		String ai_string = i18n("ai");
		InGameInfo ingame_info = multiplayer ? new MultiplayerInGameInfo(game.getRandomStartPos(), game.isRated()) : new DefaultInGameInfo();
		GameNetwork game_network = Menu.startNewGame(network, gui_root,
				menu,
				new WorldParameters(multiplayer ? game.getGamespeed() : Globals.gamespeed,
					label_mapcode.getContents(), Player.INITIAL_UNIT_COUNT,
					multiplayer ? game.getMaxUnitCount() : Player.DEFAULT_MAX_UNIT_COUNT),
				ingame_info,
				new Menu.DefaultWorldInitAction(),
				game,
				SIZES[pulldown_size.getChosenItemIndex()],
				terrain_type,
				hills/(float)SLIDER_MAX_VALUE,
				vegetation_amount/(float)SLIDER_MAX_VALUE,
				supplies_amount/(float)SLIDER_MAX_VALUE,
				seed*seed,
				new String[]{ai_string + "0", ai_string + "1", ai_string + "2", ai_string + "3", ai_string + "4", ai_string + "5"});
		game_network.getClient().getServerInterface().setPlayerSlot(0, PlayerSlot.HUMAN, race_pulldown_menus[0].getChosenItemIndex(), team_pulldown_menus[0].getChosenItemIndex(), !multiplayer, PlayerSlot.AI_NONE);
		if (!multiplayer) {
			for (int i = 1; i < race_pulldown_menus.length; i++) {
				if (isChosen(difficulty_pulldown_menus[i]))
					game_network.getClient().getServerInterface().setPlayerSlot(i, PlayerSlot.AI, race_pulldown_menus[i].getChosenItemIndex(), team_pulldown_menus[i].getChosenItemIndex(), true, difficulty_pulldown_menus[i].getChosenItemIndex());
			}
			game_network.getClient().getServerInterface().startServer();
			IO.println("Start server");
		}
		IO.println("Map code: " + label_mapcode.getContents());
		return true;
	}

	private final class MapcodeListener implements MouseClickListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			gui_root.addModalForm(new MapcodeForm(TerrainMenu.this));
		}
	}

	private final class OKListener implements MouseClickListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			boolean started = startGame();
			if (started)
				button_ok.setDisabled(true);
		}
	}

	private final class DisableListener implements ItemChosenListener<Void> {
		final int i;

		public DisableListener(int i) {
			this.i = i;
		}

		@Override
		public void itemChosen(@NonNull PulldownMenu<Void> menu, int item_index) {
			if (item_index == 0) {
				labels_players[i].setDisabled(true);
				race_pulldown_buttons[i].setDisabled(true);
				team_pulldown_buttons[i].setDisabled(true);
			} else {
				labels_players[i].setDisabled(false);
				race_pulldown_buttons[i].setDisabled(false);
				team_pulldown_buttons[i].setDisabled(false);
			}
		}
	}

	private final class PulldownUpdateMapcodeListener implements ItemChosenListener<Void> {
		@Override
		public void itemChosen(@NonNull PulldownMenu<Void> menu, int item_index) {
			setMapcode();
		}
	}

	private static final class PulldownUpdateSizeListener implements ItemChosenListener<Void> {
		@Override
		public void itemChosen(@NonNull PulldownMenu<Void> menu, int item_index) {
		}
	}

	private static final class PulldownUpdateHardListener implements ItemChosenListener<Void> {
		@Override
		public void itemChosen(@NonNull PulldownMenu<Void> menu, int item_index) {
		}
	}

	private static final class PulldownUpdateTerrainListener implements ItemChosenListener<Void> {
		@Override
		public void itemChosen(@NonNull PulldownMenu<Void> menu, int item_index) {
		}
	}

	private final class SliderUpdateMapcodeListener implements ValueListener {
		@Override
		public void valueSet(long value) {
			setMapcode();
		}
	}
}
