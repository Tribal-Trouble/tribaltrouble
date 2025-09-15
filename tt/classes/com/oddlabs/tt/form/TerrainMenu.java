package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.registration.RegistrationKey;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;
// (no Pulldown class; use PulldownMenu/PulldownButton)
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
// import removals: MultiColumnComboBox/ColumnInfo/Row/DateLabel no longer used in Single Player
import com.oddlabs.tt.gui.ScrollableGroup;
import com.oddlabs.tt.gui.ScrollablePulldownMenu;
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
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.util.WordsEncoding;
import com.oddlabs.tt.viewer.DefaultInGameInfo;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.MultiplayerInGameInfo;

import java.math.BigInteger;
import java.util.Random;
import java.util.ResourceBundle;

public final class TerrainMenu extends Group {
    public static final byte SMALL = 0;
    public static final byte MEDIUM = 1;
    public static final byte LARGE = 2;

    private static final int NORMAL = 2;
    private static final int HARD = 3;
    private static final int[] SIZES = new int[] {256, 512, 1024, 2048, 2048};
    private static final boolean[] ARCHIPELAGO = new boolean[] {false, false, false, false, true};

    private static final int SLIDER_LENGTH = 250;
    private static final int BUTTON_WIDTH = 100;
    private static final int SLIDER_MAX_VALUE = 10;

    private static final String SEED_CARDINALITY = "40000";
    private static final int SLIDER_CARDINALITY = 11;
    private static final int TERRAIN_TYPE_CARDINALITY_LEGACY = 2;
    private static final int TERRAIN_TYPE_CARDINALITY = 4;
    private static final int SIZE_CARDINALITY_LEGACY = 3;
    private static final int SIZE_CARDINALITY = 7;
    private static final int DIFFICULTY_CARDINALITY = 4;
    private static final int RACE_CARDINALITY = 2;
    private static final int TEAM_CARDINALITY = 6;
    private static final BigInteger MAX_VALUE;
    private static final BigInteger MAX_VALUE_LEGACY;
    private static int LEGACY_MAX_PLAYERS = 6;

    private final Menu main_menu;
    private final TerrainMenuListener owner;

    private final PulldownMenu pulldown_size;
    private final EditLine editline_name;
    private final PulldownMenu pm_terrain_type;
    private final Slider slider_hills;
    private final Slider slider_vegetation;
    private final Slider slider_supplies;
    private final Label label_mapcode;
    private final HorizButton button_ok;
    private final HorizButton button_cancel;
    private final HorizButton button_mapcode;
    private final PulldownMenu[] difficulty_pulldown_menus;
    private final PulldownMenu[] race_pulldown_menus;
    private final ScrollablePulldownMenu[] team_pulldown_menus;
    private final PulldownButton[] difficulty_pulldown_buttons;
    private final PulldownButton[] race_pulldown_buttons;
    private final PulldownButton[] team_pulldown_buttons;
    private final Label[] labels_players;
    private final CheckBox cb_rated;
    private final boolean multiplayer;
    private final PulldownMenu pm_gamespeed;
    private final ResourceBundle bundle = ResourceBundle.getBundle(TerrainMenu.class.getName());
    private final GUIRoot gui_root;
    private final NetworkSelector network;
    private int min_players = 6;
    private int player_count = min_players;
    private int seed;
    private boolean show_demo = true;

    // Manual map selection moved to Map Editor; keep Single Player focused on procedural settings.
    // Manual map selection moved to Map Editor; keep Single Player focused on procedural settings.

    static {
        BigInteger max = BigInteger.ONE;
        max = max.multiply(new BigInteger(SEED_CARDINALITY));
        max = max.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {TERRAIN_TYPE_CARDINALITY_LEGACY}));
        max = max.multiply(new BigInteger(new byte[] {SIZE_CARDINALITY_LEGACY}));
        max = max.multiply(new BigInteger(new byte[] {RACE_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {TEAM_CARDINALITY}));
        for (int i = 1; i < LEGACY_MAX_PLAYERS; i++) {
            max = max.multiply(new BigInteger(new byte[] {DIFFICULTY_CARDINALITY}));
            max = max.multiply(new BigInteger(new byte[] {RACE_CARDINALITY}));
            max = max.multiply(new BigInteger(new byte[] {TEAM_CARDINALITY}));
        }
        MAX_VALUE_LEGACY = max;

        max = BigInteger.ONE;
        max = max.multiply(new BigInteger(SEED_CARDINALITY));
        max = max.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {TERRAIN_TYPE_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {SIZE_CARDINALITY}));
        MAX_VALUE = max;
    }

    public TerrainMenu(
            NetworkSelector network,
            GUIRoot gui_root,
            Menu main_menu,
            boolean multiplayer,
            TerrainMenuListener owner) {
        this.network = network;
        this.main_menu = main_menu;
        this.multiplayer = multiplayer;
        this.owner = owner;
        this.gui_root = gui_root;

        // headline
        Label label_headline;
        if (multiplayer) {
            label_headline =
                    new Label(
                            Utils.getBundleString(bundle, "new_game"),
                            Skin.getSkin().getHeadlineFont());
        } else {
            label_headline =
                    new Label(
                            Utils.getBundleString(bundle, "skirmish"),
                            Skin.getSkin().getHeadlineFont());
        }
        addChild(label_headline);
        Panel standard = new Panel(Utils.getBundleString(bundle, "standard_options"));
        Panel advanced = new Panel(Utils.getBundleString(bundle, "advanced_options"));
        Group group_map_options = new Group();

        // game name
        Label label_name =
                new Label(Utils.getBundleString(bundle, "game_name"), Skin.getSkin().getEditFont());
        Label label_default_name = null;
        editline_name = new EditLine(180, Game.MAX_LENGTH);
        if (multiplayer) {
            standard.addChild(label_name);
            String default_name =
                    Utils.getBundleString(
                            bundle,
                            "default_name",
                            new Object[] {Network.getMatchmakingClient().getProfile().getNick()});
            label_default_name = new Label(default_name, Skin.getSkin().getEditFont());
            editline_name.append(default_name);
            if (Renderer.isRegistered()) standard.addChild(editline_name);
            else standard.addChild(label_default_name);
        }
    String rated_tip =
        Utils.getBundleString(
            bundle,
            "rated_game_tip",
            new Object[] {Integer.valueOf(GameSession.MIN_WINS_FOR_RANKING)});
        cb_rated = new CheckBox(false, Utils.getBundleString(bundle, "rated_game"), rated_tip);
        if (multiplayer) {
            standard.addChild(cb_rated);
            cb_rated.setDisabled(
                    Network.getMatchmakingClient().getProfile() == null
                            || Network.getMatchmakingClient().getProfile().getWins()
                                    < GameSession.MIN_WINS_FOR_RANKING);
        }

        // gamespeed
        Group group_gamespeed = new Group();
        Label label_gamespeed =
                new Label(Utils.getBundleString(bundle, "gamespeed"), Skin.getSkin().getEditFont());
        group_gamespeed.addChild(label_gamespeed);
        pm_gamespeed = new PulldownMenu();
        pm_gamespeed.addItem(
                new PulldownItem(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_SLOW)));
        pm_gamespeed.addItem(
                new PulldownItem(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_NORMAL)));
        pm_gamespeed.addItem(
                new PulldownItem(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_FAST)));
        pm_gamespeed.addItem(
                new PulldownItem(
                        ServerMessageBundler.getGamespeedString(Game.GAMESPEED_LUDICROUS)));
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

        Label label_size =
                new Label(
                        Utils.getBundleString(bundle, "island_size"), Skin.getSkin().getEditFont());
        group_size.addChild(label_size);

        pulldown_size = new PulldownMenu();
        pulldown_size.addItem(
                new PulldownItem(ServerMessageBundler.getSizeString(Game.SIZE_SMALL)));
        pulldown_size.addItem(
                new PulldownItem(ServerMessageBundler.getSizeString(Game.SIZE_MEDIUM)));
        pulldown_size.addItem(
                new PulldownItem(ServerMessageBundler.getSizeString(Game.SIZE_LARGE)));
        pulldown_size.addItem(
                new PulldownItem(ServerMessageBundler.getSizeString(Game.SIZE_ENORMOUS)));
        pulldown_size.addItem(
                new PulldownItem(ServerMessageBundler.getSizeString(Game.SIZE_ARCHIPELAGO)));

        PulldownButton pb_size = new PulldownButton(gui_root, pulldown_size, 1, 150);
        group_size.addChild(pb_size);
        label_size.place();
        pb_size.place(label_size, RIGHT_MID);
        group_size.compileCanvas();
        group_map_options.addChild(group_size);
        pulldown_size.addItemChosenListener(new PulldownUpdateMapcodeListener());

        // seed
        Label label_seed =
                new Label(Utils.getBundleString(bundle, "map_code"), Skin.getSkin().getEditFont());
        label_mapcode = new Label("", Skin.getSkin().getHeadlineFont(), 500);

        Group group_seed = new Group();
        group_seed.addChild(label_seed);
        group_seed.addChild(label_mapcode);
        label_seed.place();
        label_mapcode.place(label_seed, RIGHT_MID);
        group_seed.compileCanvas();
        advanced.addChild(group_seed);

        // terrain_type
        Group group_terrain_type = new Group();
        Label label_terrain_type =
                new Label(
                        Utils.getBundleString(bundle, "terrain_type"),
                        Skin.getSkin().getEditFont());
        group_terrain_type.addChild(label_terrain_type);
        pm_terrain_type = new PulldownMenu();
        pm_terrain_type.addItem(
                new PulldownItem(
                        ServerMessageBundler.getTerrainTypeString(Game.TERRAIN_TYPE_NATIVE)));
        pm_terrain_type.addItem(
                new PulldownItem(
                        ServerMessageBundler.getTerrainTypeString(Game.TERRAIN_TYPE_VIKING)));
        PulldownButton pb_terrain_type = new PulldownButton(gui_root, pm_terrain_type, 0, 150);
        group_terrain_type.addChild(pb_terrain_type);
        label_terrain_type.place();
        pb_terrain_type.place(label_terrain_type, RIGHT_MID);
        group_terrain_type.compileCanvas();
        pm_terrain_type.addItemChosenListener(new PulldownUpdateMapcodeListener());
        group_map_options.addChild(group_terrain_type);

        // Single Player manual map load/clear has been removed; use Map Editor to load maps.

        Group group_sliders = new Group();
        // hills
        Label label_hills_low =
                new Label(Utils.getBundleString(bundle, "min"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_hills_low);
        Label label_hills_high =
                new Label(Utils.getBundleString(bundle, "max"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_hills_high);
        Label label_hills =
                new Label(Utils.getBundleString(bundle, "hills"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_hills);
        slider_hills = new Slider(SLIDER_LENGTH, 0, SLIDER_MAX_VALUE, SLIDER_MAX_VALUE / 2);
        slider_hills.addValueListener(new SliderUpdateMapcodeListener());
        group_sliders.addChild(slider_hills);

        // vegetation
        Label label_vegetation_low =
                new Label(Utils.getBundleString(bundle, "min"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_vegetation_low);
        Label label_vegetation_high =
                new Label(Utils.getBundleString(bundle, "max"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_vegetation_high);
        Label label_vegetation =
                new Label(Utils.getBundleString(bundle, "trees"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_vegetation);
        slider_vegetation = new Slider(SLIDER_LENGTH, 0, SLIDER_MAX_VALUE, SLIDER_MAX_VALUE / 2);
        slider_vegetation.addValueListener(new SliderUpdateMapcodeListener());
        group_sliders.addChild(slider_vegetation);

        // supplies
        Label label_supplies_low =
                new Label(Utils.getBundleString(bundle, "min"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_supplies_low);
        Label label_supplies_high =
                new Label(Utils.getBundleString(bundle, "max"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_supplies_high);
        Label label_supplies =
                new Label(Utils.getBundleString(bundle, "resources"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_supplies);
        slider_supplies = new Slider(SLIDER_LENGTH, 0, SLIDER_MAX_VALUE, SLIDER_MAX_VALUE / 2);
        slider_supplies.addValueListener(new SliderUpdateMapcodeListener());
        group_sliders.addChild(slider_supplies);

        // supplies
        label_supplies.place();
        label_supplies_low.place(label_supplies, RIGHT_MID);
        slider_supplies.place(label_supplies_low, RIGHT_MID);
        label_supplies_high.place(slider_supplies, RIGHT_MID);
        // vegetation
        label_vegetation.place(
                label_supplies, TOP_LEFT, Skin.getSkin().getFormData().getSectionSpacing());
        slider_vegetation.place(
                slider_supplies, TOP_MID, Skin.getSkin().getFormData().getSectionSpacing());
        label_vegetation_low.place(slider_vegetation, LEFT_MID);
        label_vegetation_high.place(slider_vegetation, RIGHT_MID);
        // hills
        label_hills.place(
                label_vegetation, TOP_LEFT, Skin.getSkin().getFormData().getSectionSpacing());
        slider_hills.place(
                slider_vegetation, TOP_MID, Skin.getSkin().getFormData().getSectionSpacing());
        label_hills_low.place(slider_hills, LEFT_MID);
        label_hills_high.place(slider_hills, RIGHT_MID);
        // sliders
        group_sliders.compileCanvas();
        advanced.addChild(group_sliders);

        // Player slots
        Label label_player_slots = new Label("Players", Skin.getSkin().getEditFont());
        Group group_num_players = new Group();

        group_num_players.addChild(label_player_slots);
        label_player_slots.place();
        advanced.addChild(group_num_players);

        ScrollablePulldownMenu pulldown_menu_slots = new ScrollablePulldownMenu(6);
        for (int i = min_players; i <= MatchmakingServerInterface.MAX_PLAYERS; i++) {
            pulldown_menu_slots.addItem(new PulldownItem(Integer.toString(i)));
        }

        PulldownButton pulldown_player_slots =
                new PulldownButton(gui_root, pulldown_menu_slots, 0, 150);
        group_num_players.addChild(pulldown_player_slots);
        pulldown_player_slots.place(label_player_slots, RIGHT_MID);
        group_num_players.compileCanvas();

        // races and teams
        ScrollableGroup group_race_team = new ScrollableGroup(200, 64);
        labels_players = new Label[MatchmakingServerInterface.MAX_PLAYERS];
        difficulty_pulldown_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        race_pulldown_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        team_pulldown_menus = new ScrollablePulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        difficulty_pulldown_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
        race_pulldown_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
        team_pulldown_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
        Random random = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
        random.nextFloat();
        for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            difficulty_pulldown_menus[i] = new PulldownMenu();
            race_pulldown_menus[i] = new PulldownMenu();
            team_pulldown_menus[i] = new ScrollablePulldownMenu(6);

            if (i == 0) {
                difficulty_pulldown_menus[i].addItem(
                        new PulldownItem(Utils.getBundleString(bundle, "human")));
            } else {
                difficulty_pulldown_menus[i].addItem(
                        new PulldownItem(Utils.getBundleString(bundle, "closed")));
                difficulty_pulldown_menus[i].addItem(
                        new PulldownItem(Utils.getBundleString(bundle, "easy_ai")));
                difficulty_pulldown_menus[i].addItem(
                        new PulldownItem(Utils.getBundleString(bundle, "normal_ai")));
                PulldownItem hard = new PulldownItem(Utils.getBundleString(bundle, "hard_ai"));
                difficulty_pulldown_menus[i].addItem(hard);
            }

            difficulty_pulldown_buttons[i] =
                    new PulldownButton(gui_root, difficulty_pulldown_menus[i], 0, 115);
            group_race_team.addChild(difficulty_pulldown_buttons[i]);

            for (int j = 0; j < RacesResources.getNumRaces(); j++) {
                PulldownItem pulldown_item_race = new PulldownItem(RacesResources.getRaceName(j));
                race_pulldown_menus[i].addItem(pulldown_item_race);
            }

            race_pulldown_buttons[i] = new PulldownButton(gui_root, race_pulldown_menus[i], 0, 115);
            group_race_team.addChild(race_pulldown_buttons[i]);
            for (int j = 0; j < MatchmakingServerInterface.MAX_PLAYERS; j++) {
                String team_str =
                        Utils.getBundleString(
                                bundle, "team", new Object[] {Integer.toString(j + 1)});
                PulldownItem pulldown_item_team = new PulldownItem(team_str);
                team_pulldown_menus[i].addItem(pulldown_item_team);
            }
            team_pulldown_buttons[i] = new PulldownButton(gui_root, team_pulldown_menus[i], i, 115);
            group_race_team.addChild(team_pulldown_buttons[i]);
            if (i == 0) {
                String player_str =
                        Utils.getBundleString(bundle, "player", new Object[] {Integer.toString(1)});
                labels_players[0] = new Label(player_str, Skin.getSkin().getEditFont());
                labels_players[0].setColor(Player.COLORS[0]);
                group_race_team.addChild(labels_players[0]);
                labels_players[0].place();
                difficulty_pulldown_buttons[0].place(labels_players[0], RIGHT_MID);
                race_pulldown_buttons[0].place(difficulty_pulldown_buttons[0], RIGHT_MID);
                team_pulldown_buttons[0].place(race_pulldown_buttons[0], RIGHT_MID);
            } else {
                String player_str =
                        Utils.getBundleString(
                                bundle, "player", new Object[] {Integer.toString(i + 1)});
                labels_players[i] = new Label(player_str, Skin.getSkin().getEditFont());
                labels_players[i].setColor(Player.COLORS[i]);
                group_race_team.addChild(labels_players[i]);
                labels_players[i].place(labels_players[i - 1], BOTTOM_RIGHT);
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
        button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener(new CancelButtonListener());
        button_mapcode = new HorizButton(Utils.getBundleString(bundle, "enter_map_code"), 170);
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
        // No Single Player map-load row
        group_map_options.compileCanvas();
        standard.addChild(group_map_options);

        // standard
        if (multiplayer) {
            label_name.place();
            if (Renderer.isRegistered()) editline_name.place(label_name, RIGHT_MID);
            else label_default_name.place(label_name, RIGHT_MID);
            cb_rated.place(
                    label_name, BOTTOM_LEFT, Skin.getSkin().getFormData().getSectionSpacing());
            group_map_options.place(cb_rated, BOTTOM_LEFT);
        } else {
            group_map_options.place();
            group_race_team.place(
                    group_map_options,
                    BOTTOM_LEFT,
                    Skin.getSkin().getFormData().getSectionSpacing());
        }

        standard.compileCanvas();
        standard.setDim(standard.getWidth(), standard.getHeight());
        // advanced
        group_sliders.place();
        group_num_players.place(
                group_sliders, BOTTOM_LEFT, Skin.getSkin().getFormData().getSectionSpacing());
        group_seed.place(
                group_num_players, BOTTOM_LEFT, Skin.getSkin().getFormData().getSectionSpacing());
        advanced.compileCanvas();

        PanelGroup panel_group = new PanelGroup(new Panel[] {standard, advanced}, 0);
        addChild(panel_group);
        pulldown_menu_slots.addItemChosenListener(
                new PulldownUpdatePlayersChangedListener(
                        group_race_team, standard, panel_group, this, multiplayer, label_headline));

        // Place objects
        label_headline.place();
        panel_group.place(label_headline, BOTTOM_LEFT);

        // buttons
        group_buttons.place(ORIGIN_BOTTOM_RIGHT);

        compileCanvas();
    randomize();

        // set standart game
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
                race_pulldown_menus[i].chooseItem(
                        (race_pulldown_menus[0].getChosenItemIndex() + 1) % 2);
            } else {
                difficulty_pulldown_menus[i].chooseItem(0);
            }
        }
        pulldown_size.chooseItem(1);
        if (!Renderer.isRegistered()) pm_terrain_type.chooseItem(0);

        // end of initial UI setup
        pulldown_menu_slots.chooseItem(0);
    }

    private final void setMapcodeLegacy() {
        BigInteger max_val = BigInteger.ONE;
        BigInteger result = BigInteger.ZERO;
        result = result.add((new BigInteger("" + seed)).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(SEED_CARDINALITY));
        int hills = slider_hills.getValue();
        result = result.add((new BigInteger(new byte[] {(byte) hills})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int vegetation_amount = slider_vegetation.getValue();
        result =
                result.add(
                        (new BigInteger(new byte[] {(byte) vegetation_amount})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int supplies_amount = slider_supplies.getValue();
        result =
                result.add((new BigInteger(new byte[] {(byte) supplies_amount})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int terrain_type = pm_terrain_type.getChosenItemIndex();
        result = result.add((new BigInteger(new byte[] {(byte) terrain_type})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {TERRAIN_TYPE_CARDINALITY_LEGACY}));
        int size = pulldown_size.getChosenItemIndex();
        result = result.add((new BigInteger(new byte[] {(byte) size})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SIZE_CARDINALITY_LEGACY}));

    String code = RegistrationKey.createString(result);
    label_mapcode.clear();
    label_mapcode.append(code);
    }

    private final void setMapcode() {
        BigInteger max_val = BigInteger.ONE;
        BigInteger result = BigInteger.ZERO;
        result = result.add((new BigInteger("" + seed)).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(SEED_CARDINALITY));
        int hills = slider_hills.getValue();
        result = result.add((new BigInteger(new byte[] {(byte) hills})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int vegetation_amount = slider_vegetation.getValue();
        result =
                result.add(
                        (new BigInteger(new byte[] {(byte) vegetation_amount})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int supplies_amount = slider_supplies.getValue();
        result =
                result.add((new BigInteger(new byte[] {(byte) supplies_amount})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int terrain_type = pm_terrain_type.getChosenItemIndex();
        result = result.add((new BigInteger(new byte[] {(byte) terrain_type})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {TERRAIN_TYPE_CARDINALITY}));
        int size = pulldown_size.getChosenItemIndex();
        result = result.add((new BigInteger(new byte[] {(byte) size})).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {SIZE_CARDINALITY}));
        String code = WordsEncoding.encode(result);
        label_mapcode.clear();
        label_mapcode.append(code);
    }

    public final void parseMapcode(String text) {
        String code = text.toUpperCase();
        show_demo = false;
        if (code.indexOf(' ') == -1) {
            // Legacy notation
            BigInteger result = RegistrationKey.parseBits(code);
            parseBigIntegerLegacy(result);
        } else {
            // New notation
            try {
                BigInteger result = WordsEncoding.decode(text);
                parseBigInteger(result);
            } catch (Exception e) {

            }
        }
        show_demo = true;
        label_mapcode.clear();
        label_mapcode.append(code);
        if (!multiplayer) onTerrainSettingChanged("map code parsed");
    }

    private final void parseBigInteger(BigInteger result) {
        boolean world_size_valid = true;
        boolean terrain_type_valid = true;
        BigInteger max_val = MAX_VALUE;
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SIZE_CARDINALITY}));
        int size = result.divide(max_val).intValue();
        if (pulldown_size.getSize() <= size) {
            world_size_valid = false;
        } else {
            pulldown_size.chooseItem(size);
        }
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {TERRAIN_TYPE_CARDINALITY}));
        int terrain_type = result.divide(max_val).intValue();
        if (pm_terrain_type.getSize() <= terrain_type) {
            terrain_type_valid = false;
        } else {
            pm_terrain_type.chooseItem(terrain_type);
        }
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int supplies_amount = result.divide(max_val).intValue();
        slider_supplies.setValue(supplies_amount);
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int vegetation_amount = result.divide(max_val).intValue();
        slider_vegetation.setValue(vegetation_amount);

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int hills = result.divide(max_val).intValue();
        slider_hills.setValue(hills);

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(SEED_CARDINALITY));
        seed = result.divide(max_val).intValue();

        if (!terrain_type_valid || !world_size_valid) {
            String message = Utils.getBundleString(bundle, "map_unsupported") + ": ";
            if (!world_size_valid) {
                message += Utils.getBundleString(bundle, "island_size").replace(":", "") + " ";
            }
            if (!terrain_type_valid) {
                message += Utils.getBundleString(bundle, "terrain_type").replace(":", "");
            }
            gui_root.addModalForm(new MessageForm(message));
        }
    }

    private final void parseBigIntegerLegacy(BigInteger result) {
        BigInteger max_val = MAX_VALUE_LEGACY;
        for (int i = LEGACY_MAX_PLAYERS - 1; i >= 1; i--) {
            result = result.mod(max_val);
            max_val = max_val.divide(new BigInteger(new byte[] {TEAM_CARDINALITY}));
            int team = result.divide(max_val).intValue();
            team_pulldown_menus[i].chooseItem(team);
            result = result.mod(max_val);
            max_val = max_val.divide(new BigInteger(new byte[] {RACE_CARDINALITY}));
            int race = result.divide(max_val).intValue();
            race_pulldown_menus[i].chooseItem(race);
            result = result.mod(max_val);
            max_val = max_val.divide(new BigInteger(new byte[] {DIFFICULTY_CARDINALITY}));
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
        max_val = max_val.divide(new BigInteger(new byte[] {TEAM_CARDINALITY}));
        int player_team = result.divide(max_val).intValue();
        team_pulldown_menus[0].chooseItem(player_team);
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {RACE_CARDINALITY}));
        int player_race = result.divide(max_val).intValue();
        race_pulldown_menus[0].chooseItem(player_race);
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SIZE_CARDINALITY_LEGACY}));
        int size = result.divide(max_val).intValue();
        pulldown_size.chooseItem(size);
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {TERRAIN_TYPE_CARDINALITY_LEGACY}));
        int terrain_type = result.divide(max_val).intValue();
        pm_terrain_type.chooseItem(terrain_type);
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int supplies_amount = result.divide(max_val).intValue();
        slider_supplies.setValue(supplies_amount);
        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int vegetation_amount = result.divide(max_val).intValue();
        slider_vegetation.setValue(vegetation_amount);

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(new byte[] {SLIDER_CARDINALITY}));
        int hills = result.divide(max_val).intValue();
        slider_hills.setValue(hills);

        result = result.mod(max_val);
        max_val = max_val.divide(new BigInteger(SEED_CARDINALITY));
        seed = result.divide(max_val).intValue();
    }

    public final void setSeed(int seed) {
        this.seed = seed;
        if (!multiplayer) onTerrainSettingChanged("seed set");
    }

    public final void setFocus() {
        button_ok.setFocus();
    }

    public final GUIObject getButtonOK() {
        return button_ok;
    }

    private final class CancelButtonListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            owner.terrainMenuCancel();
        }
    }

    private final void randomize() {
        Random random =
                new Random(
                        LocalEventQueue.getQueue().getHighPrecisionManager().getTick()
                                * LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
        random.nextInt();
        BigInteger rand_int = new BigInteger(100, random);
        parseBigIntegerLegacy(rand_int);
        setMapcode();
    if (!multiplayer) onTerrainSettingChanged("randomized");
    }

    protected final void doCancel() {
        if (multiplayer) new SelectGameMenu(network, gui_root, main_menu);
    }

    private final boolean isChosen(PulldownMenu menu) {
        return menu.getChosenItemIndex() != 0;
    }

    public final boolean startGame() {
        int hills = slider_hills.getValue();
        int vegetation_amount = slider_vegetation.getValue();
        int supplies_amount = slider_supplies.getValue();
        int terrain_type = pm_terrain_type.getChosenItemIndex();
        Game game;
        boolean rated = cb_rated.isMarked();
        if (rated)
            team_pulldown_menus[0].chooseItem(team_pulldown_menus[0].getChosenItemIndex() % 2);
        if (multiplayer) {
            String game_name = editline_name.getContents();
            if (game_name.length() < Game.MIN_LENGTH) {
            String min_name =
                Utils.getBundleString(
                    bundle,
                    "min_name_length",
                    new Object[] {Integer.valueOf(Game.MIN_LENGTH)});
                gui_root.addModalForm(new MessageForm(min_name));
                return false;
            }
            float random_start_pos = LocalEventQueue.getQueue().getTime() % 1f;
            game =
                    new Game(
                            game_name,
                            (byte) pulldown_size.getChosenItemIndex(),
                            (byte) terrain_type,
                            (byte) hills,
                            (byte) vegetation_amount,
                            (byte) supplies_amount,
                            rated,
                            (byte) (pm_gamespeed.getChosenItemIndex() + 1),
                            label_mapcode.getContents(),
                            random_start_pos,
                            Player.DEFAULT_MAX_UNIT_COUNT);
        } else {
            boolean has_enemy = false;
            for (int i = 1; i < race_pulldown_menus.length; i++)
                if (isChosen(difficulty_pulldown_menus[i])
                        && team_pulldown_menus[i].getChosenItemIndex()
                                != team_pulldown_menus[0].getChosenItemIndex()) has_enemy = true;
            if (!has_enemy) {
        String min_name =
            Utils.getBundleString(
                bundle, "min_num_teams", new Object[] {Integer.valueOf(2)});
                gui_root.addModalForm(new MessageForm(min_name));
                return false;
            }
            game = null;
        }
        if (owner != null) owner.terrainMenuOK();
        SelectGameMenu menu = null;
        if (multiplayer) menu = (SelectGameMenu) owner;
    // starting game setup logging
        System.out.println(
                "hills = "
                        + hills / (float) SLIDER_MAX_VALUE
                        + " | vegetation_amount = "
                        + vegetation_amount / (float) SLIDER_MAX_VALUE
                        + " | supplies_amount = "
                        + supplies_amount / (float) SLIDER_MAX_VALUE
                        + " | seed = "
                        + seed * seed);
    // ai string used via generateAINames()
        InGameInfo ingame_info =
                multiplayer
                        ? new MultiplayerInGameInfo(game.getRandomStartPos(), game.isRated())
                        : new DefaultInGameInfo();
    System.out.println("InGameInfo created" + player_count);
    GameNetwork game_network =
    Menu.startNewGameWithMap(
                        network,
                        gui_root,
                        menu,
                        new WorldParameters(
                                multiplayer ? game.getGamespeed() : Globals.gamespeed,
                                label_mapcode.getContents(),
                                Player.INITIAL_UNIT_COUNT,
                                multiplayer
                                        ? game.getMaxUnitCount()
                                        : Player.DEFAULT_MAX_UNIT_COUNT),
                        ingame_info,
                        new Menu.DefaultWorldInitAction(),
                        game,
                        SIZES[pulldown_size.getChosenItemIndex()],
                        terrain_type,
                        hills / (float) SLIDER_MAX_VALUE,
                        vegetation_amount / (float) SLIDER_MAX_VALUE,
                        supplies_amount / (float) SLIDER_MAX_VALUE,
                        seed * seed,
                        ARCHIPELAGO[pulldown_size.getChosenItemIndex()],
            generateAINames(),
            player_count,
            null);
        game_network
                .getClient()
                .getServerInterface()
                .setPlayerSlot(
                        0,
                        PlayerSlot.HUMAN,
                        race_pulldown_menus[0].getChosenItemIndex(),
                        team_pulldown_menus[0].getChosenItemIndex(),
                        !multiplayer,
                        PlayerSlot.AI_NONE);
        if (!multiplayer) {
            for (int i = 1; i < race_pulldown_menus.length; i++) {
                if (isChosen(difficulty_pulldown_menus[i]))
                    game_network
                            .getClient()
                            .getServerInterface()
                            .setPlayerSlot(
                                    i,
                                    PlayerSlot.AI,
                                    race_pulldown_menus[i].getChosenItemIndex(),
                                    team_pulldown_menus[i].getChosenItemIndex(),
                                    true,
                                    difficulty_pulldown_menus[i].getChosenItemIndex());
            }
            game_network.getClient().getServerInterface().startServer();
            System.out.println("Start server");
        }
        System.out.println("Map code: " + label_mapcode.getContents());
        return true;
    }

    /** Creates an array of translated AI names based on the number of max_players */
    private String[] generateAINames() {
        String ai_string = Utils.getBundleString(bundle, "ai");
        String[] ai_names = new String[MatchmakingServerInterface.MAX_PLAYERS];
        for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            ai_names[i] = ai_string + i;
        }
        return ai_names;
    }

    private final class MapcodeListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            gui_root.addModalForm(new MapcodeForm(TerrainMenu.this));
        }
    }

    private final class OKListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            boolean started = startGame();
            if (started) button_ok.setDisabled(true);
        }
    }

    private final class DisableListener implements ItemChosenListener {
        int i;

        public DisableListener(int i) {
            this.i = i;
        }

        public final void itemChosen(PulldownMenu menu, int item_index) {
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

    private final class PulldownUpdateMapcodeListener implements ItemChosenListener {
        public final void itemChosen(PulldownMenu menu, int item_index) {
            setMapcode();
            // Auto-clear manual map only for terrain-affecting pulldowns
            if (!multiplayer) {
                if (menu == pulldown_size || menu == pm_terrain_type) {
                    onTerrainSettingChanged("pulldown change");
                }
            }
        }
    }

    private final class PulldownUpdatePlayersChangedListener
            implements ItemChosenListener {
        ScrollableGroup group_race_team;
        Panel standard;
        PanelGroup panel_group;
        TerrainMenu terrain_menu;
        boolean is_multiplayer;
        Label label_headline;

        public PulldownUpdatePlayersChangedListener(
                ScrollableGroup group_race_team,
                Panel standard,
                PanelGroup panel_group,
                TerrainMenu terrain_menu,
                boolean is_multiplayer,
                Label label_headline) {
            this.group_race_team = group_race_team;
            this.standard = standard;
            this.panel_group = panel_group;
            this.terrain_menu = terrain_menu;
            this.is_multiplayer = is_multiplayer;
            this.label_headline = label_headline;
        }

        public final void itemChosen(PulldownMenu menu, int item_index) {
            player_count = item_index + min_players;
            if (is_multiplayer) return;

            // For single player we need to redraw all the controls (Actually multiplayer doesn't
            // use these controls...)
            group_race_team.clearChildren();
            standard.removeChild(group_race_team);
            group_race_team = new ScrollableGroup(200, 64);
            group_race_team.place();
            standard.addChild(group_race_team);
            for (int i = 0; i < player_count; i++) {
                difficulty_pulldown_menus[i] = new PulldownMenu();
                race_pulldown_menus[i] = new PulldownMenu();
                team_pulldown_menus[i] = new ScrollablePulldownMenu(6);

                if (i == 0) {
                    difficulty_pulldown_menus[i].addItem(
                            new PulldownItem(Utils.getBundleString(bundle, "human")));
                } else {
                    difficulty_pulldown_menus[i].addItem(
                            new PulldownItem(Utils.getBundleString(bundle, "closed")));
                    difficulty_pulldown_menus[i].addItem(
                            new PulldownItem(Utils.getBundleString(bundle, "easy_ai")));
                    difficulty_pulldown_menus[i].addItem(
                            new PulldownItem(Utils.getBundleString(bundle, "normal_ai")));
                    PulldownItem hard = new PulldownItem(Utils.getBundleString(bundle, "hard_ai"));
                    difficulty_pulldown_menus[i].addItem(hard);
                }

                difficulty_pulldown_buttons[i] =
                        new PulldownButton(gui_root, difficulty_pulldown_menus[i], 0, 115);
                group_race_team.addChild(difficulty_pulldown_buttons[i]);

                for (int j = 0; j < RacesResources.getNumRaces(); j++) {
                    PulldownItem pulldown_item_race =
                            new PulldownItem(RacesResources.getRaceName(j));
                    race_pulldown_menus[i].addItem(pulldown_item_race);
                }

                race_pulldown_buttons[i] =
                        new PulldownButton(gui_root, race_pulldown_menus[i], 0, 115);
                group_race_team.addChild(race_pulldown_buttons[i]);
                for (int j = 0; j < player_count; j++) {
                    String team_str =
                            Utils.getBundleString(
                                    bundle, "team", new Object[] {Integer.toString(j + 1)});
                    PulldownItem pulldown_item_team = new PulldownItem(team_str);
                    team_pulldown_menus[i].addItem(pulldown_item_team);
                }
                team_pulldown_buttons[i] =
                        new PulldownButton(
                                gui_root,
                                team_pulldown_menus[i],
                                StrictMath.min(i, player_count - 1),
                                115);
                group_race_team.addChild(team_pulldown_buttons[i]);
                if (i == 0) {
                    String player_str =
                            Utils.getBundleString(
                                    bundle, "player", new Object[] {Integer.toString(1)});
                    labels_players[0] = new Label(player_str, Skin.getSkin().getEditFont());
                    labels_players[0].setColor(Player.COLORS[0]);
                    group_race_team.addChild(labels_players[0]);
                    labels_players[0].place();
                    difficulty_pulldown_buttons[0].place(labels_players[0], RIGHT_MID);
                    race_pulldown_buttons[0].place(difficulty_pulldown_buttons[0], RIGHT_MID);
                    team_pulldown_buttons[0].place(race_pulldown_buttons[0], RIGHT_MID);
                } else {
                    String player_str =
                            Utils.getBundleString(
                                    bundle, "player", new Object[] {Integer.toString(i + 1)});
                    labels_players[i] = new Label(player_str, Skin.getSkin().getEditFont());
                    labels_players[i].setColor(Player.COLORS[i]);
                    group_race_team.addChild(labels_players[i]);
                    labels_players[i].place(labels_players[i - 1], BOTTOM_RIGHT);
                    difficulty_pulldown_buttons[i].place(labels_players[i], RIGHT_MID);
                    race_pulldown_buttons[i].place(difficulty_pulldown_buttons[i], RIGHT_MID);
                    team_pulldown_buttons[i].place(race_pulldown_buttons[i], RIGHT_MID);
                    difficulty_pulldown_menus[i].addItemChosenListener(new DisableListener(i));
                }
                difficulty_pulldown_menus[i].addItemChosenListener(
                        new PulldownUpdateMapcodeListener());
                race_pulldown_menus[i].addItemChosenListener(new PulldownUpdateMapcodeListener());
                team_pulldown_menus[i].addItemChosenListener(new PulldownUpdateMapcodeListener());
                // setup defaults
                if (i == 1) {
                    difficulty_pulldown_menus[i].chooseItem(1);
                } else if (i != 0) {
                    difficulty_pulldown_menus[i].chooseItem(0);
                }
            }

            group_race_team.compileCanvas();
            standard.setDim(standard.getWidth(), standard.getHeight());
            // For some reason we need to reset the position of the panel group and
            // the top label by the height of the panel group or we run into infinite growth.
            // Hacky but -- the ui is rough
            panel_group.setPos(0, panel_group.getY() - panel_group.getHeight());
            label_headline.setPos(0, label_headline.getY() - panel_group.getHeight());
            terrain_menu.compileCanvas();
        }
    }

    private final class PulldownUpdateSizeListener implements ItemChosenListener {
        public final void itemChosen(PulldownMenu menu, int item_index) {
            if (item_index == LARGE && !Renderer.isRegistered()) {
                menu.chooseItem(MEDIUM);
                if (show_demo) {
                    ResourceBundle db = ResourceBundle.getBundle(DemoForm.class.getName());
                    Form demo_form =
                            new DemoForm(
                                    gui_root,
                                    Utils.getBundleString(db, "large_islands_unavailable_header"),
                                    new GUIImage(
                                            512,
                                            256,
                                            0f,
                                            0f,
                                            1f,
                                            1f,
                                            "/textures/gui/demo_hugeislands"),
                                    Utils.getBundleString(db, "large_islands_unavailable"));
                    gui_root.addModalForm(demo_form);
                }
            }
            if (!multiplayer) onTerrainSettingChanged("size changed");
        }
    }

    private final class PulldownUpdateHardListener implements ItemChosenListener {
        public final void itemChosen(PulldownMenu menu, int item_index) {
            if (item_index == HARD && !Renderer.isRegistered()) {
                menu.chooseItem(NORMAL);
                if (show_demo) {
                    ResourceBundle db = ResourceBundle.getBundle(DemoForm.class.getName());
                    Form demo_form =
                            new DemoForm(
                                    gui_root,
                                    Utils.getBundleString(db, "hard_ai_unavailable_header"),
                                    new GUIImage(
                                            512, 256, 0f, 0f, 1f, 1f, "/textures/gui/demo_hardai"),
                                    Utils.getBundleString(db, "hard_ai_unavailable"));
                    gui_root.addModalForm(demo_form);
                }
            }
        }
    }

    private final class PulldownUpdateTerrainListener implements ItemChosenListener {
        public final void itemChosen(PulldownMenu menu, int item_index) {
            if (item_index == Game.TERRAIN_TYPE_VIKING && !Renderer.isRegistered()) {
                menu.chooseItem(Game.TERRAIN_TYPE_NATIVE);
                if (show_demo) {
                    ResourceBundle db = ResourceBundle.getBundle(DemoForm.class.getName());
                    Form demo_form =
                            new DemoForm(
                                    gui_root,
                                    Utils.getBundleString(db, "viking_islands_unavailable_header"),
                                    new GUIImage(
                                            512,
                                            256,
                                            0f,
                                            0f,
                                            1f,
                                            1f,
                                            "/textures/gui/demo_northernterrain"),
                                    Utils.getBundleString(db, "viking_islands_unavailable"));
                    gui_root.addModalForm(demo_form);
                }
            }
            if (!multiplayer) onTerrainSettingChanged("terrain type changed");
        }
    }

    private final class SliderUpdateMapcodeListener implements ValueListener {
        public final void valueSet(int value) {
            setMapcode();
            if (!multiplayer) onTerrainSettingChanged("slider changed");
        }
    }

    // Manual map selection helpers removed.

    // No-op: retained for historical call sites; Single Player no longer clears manual maps.
    private void onTerrainSettingChanged(String reason) {
        // Intentionally empty
    }
}
