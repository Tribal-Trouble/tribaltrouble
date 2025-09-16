package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.ScrollableGroup;
import com.oddlabs.tt.gui.ScrollablePulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

/**
 * Reusable "Players" section from Single Player menu.
 * Provides per-slot controls: Human/Closed/AI(difficulty), Race (Natives/Vikings), Team.
 * Exposes getters for the current configuration and a player-count pulldown.
 */
public final class PlayersSection extends Group {
    // Index mapping mirrors TerrainMenu behavior:
    // For slot 0, difficulty menu contains only "Human" at index 0.
    // For slots > 0, indexes are: 0=Closed, 1=Easy AI, 2=Normal AI, 3=Hard AI.
    public static final int OPEN_INDEX = 0; // slot 0 only
    public static final int CLOSED_INDEX = 0;
    public static final int COMPUTER_EASY_INDEX = 1;
    public static final int COMPUTER_NORMAL_INDEX = 2;
    public static final int COMPUTER_HARD_INDEX = 3;

    private final GUIRoot guiRoot;
    private final ResourceBundle bundle;

    private final Label[] labels_players;
    private final PulldownMenu[] difficulty_menus; // 0: human/closed, 1..: AI
    private final PulldownMenu[] race_menus;
    private final ScrollablePulldownMenu[] team_menus;
    private final PulldownButton[] difficulty_buttons;
    private final PulldownButton[] race_buttons;
    private final PulldownButton[] team_buttons;

    private final PulldownMenu slots_menu;
    private final PulldownButton slots_button;
    private final boolean showPlayerCountRow;

    private static final int MIN_PLAYERS = 6; // mirror TerrainMenu default
    private int player_count = MIN_PLAYERS;

    public PlayersSection(GUIRoot guiRoot) {
        this(guiRoot, ResourceBundle.getBundle(TerrainMenu.class.getName()), true);
    }

    public PlayersSection(GUIRoot guiRoot, ResourceBundle bundle) {
        this(guiRoot, bundle, true);
    }

    public PlayersSection(GUIRoot guiRoot, ResourceBundle bundle, boolean showPlayerCountRow) {
        this.guiRoot = guiRoot;
        this.bundle = bundle;
        this.showPlayerCountRow = showPlayerCountRow;

        // Player count row (optionally shown)
        Label label_player_slots = new Label("Players", Skin.getSkin().getEditFont());
        Group group_num_players = new Group();
        group_num_players.addChild(label_player_slots);
        label_player_slots.place();

        // Scrollable player-count pulldown, range 6..MAX
        slots_menu = new ScrollablePulldownMenu(6);
        for (int i = MIN_PLAYERS; i <= MatchmakingServerInterface.MAX_PLAYERS; i++) {
            slots_menu.addItem(new PulldownItem(Integer.toString(i)));
        }
        // Default selection: 6 players (index 0)
        slots_button = new PulldownButton(guiRoot, slots_menu, 0, 150);
        group_num_players.addChild(slots_button);
        slots_button.place(label_player_slots, GUIObject.RIGHT_MID);
        group_num_players.compileCanvas();
        if (showPlayerCountRow) {
            addChild(group_num_players);
        }

        // Players grid (scrollable): label + [human/ai] [race] [team]
    ScrollableGroup group_race_team = new ScrollableGroup(200, 64);
    // Make scrollbar appear with thumb at the visual top, while content order stays the same
    group_race_team.setInvertedScrollbar(true);
        labels_players = new Label[MatchmakingServerInterface.MAX_PLAYERS];
        difficulty_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        race_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
    team_menus = new ScrollablePulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        difficulty_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
        race_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
        team_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];

        Font font = Skin.getSkin().getEditFont();
        for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            difficulty_menus[i] = new PulldownMenu();
            race_menus[i] = new PulldownMenu();
            team_menus[i] = new ScrollablePulldownMenu(6);

            if (i == 0) {
                // Local player: fixed Human entry
                difficulty_menus[i].addItem(new PulldownItem(Utils.getBundleString(bundle, "human")));
            } else {
                difficulty_menus[i].addItem(new PulldownItem(Utils.getBundleString(bundle, "closed")));
                difficulty_menus[i].addItem(new PulldownItem(Utils.getBundleString(bundle, "easy_ai")));
                difficulty_menus[i].addItem(new PulldownItem(Utils.getBundleString(bundle, "normal_ai")));
                difficulty_menus[i].addItem(new PulldownItem(Utils.getBundleString(bundle, "hard_ai")));
            }

            difficulty_buttons[i] = new PulldownButton(guiRoot, difficulty_menus[i], 0, 115);
            group_race_team.addChild(difficulty_buttons[i]);

            for (int r = 0; r < RacesResources.getNumRaces(); r++) {
                race_menus[i].addItem(new PulldownItem(RacesResources.getRaceName(r)));
            }
            race_buttons[i] = new PulldownButton(guiRoot, race_menus[i], 0, 115);
            group_race_team.addChild(race_buttons[i]);

            for (int t = 0; t < MatchmakingServerInterface.MAX_PLAYERS; t++) {
                String team_str = Utils.getBundleString(bundle, "team", new Object[] {Integer.toString(t + 1)});
                team_menus[i].addItem(new PulldownItem(team_str));
            }
            team_buttons[i] = new PulldownButton(guiRoot, team_menus[i], i, 115);
            group_race_team.addChild(team_buttons[i]);

            String player_str = Utils.getBundleString(bundle, "player", new Object[] {Integer.toString(i + 1)});
            labels_players[i] = new Label(player_str, font);
            labels_players[i].setColor(Player.COLORS[i % Player.COLORS.length]);
            group_race_team.addChild(labels_players[i]);

            if (i == 0) {
                labels_players[i].place();
            } else {
                labels_players[i].place(labels_players[i - 1], GUIObject.BOTTOM_RIGHT);
            }
            difficulty_buttons[i].place(labels_players[i], GUIObject.RIGHT_MID);
            race_buttons[i].place(difficulty_buttons[i], GUIObject.RIGHT_MID);
            team_buttons[i].place(race_buttons[i], GUIObject.RIGHT_MID);
        }

    group_race_team.compileCanvas();
    addChild(group_race_team);

    // Place immediate child groups before compiling this container
    if (showPlayerCountRow) {
        group_num_players.place();
        group_race_team.place(
            group_num_players,
            GUIObject.BOTTOM_LEFT,
            Skin.getSkin().getFormData().getSectionSpacing());
    } else {
        // No player count row in this section; anchor grid at origin
        group_race_team.place();
    }

        // Defaults: slot 0 human, team 1; others closed and team=slot index+1
        // Align pulldown to current default player_count (6)
        slots_menu.chooseItem(player_count - MIN_PLAYERS);
        race_buttons[0].getMenu().chooseItem(0);
        team_buttons[0].getMenu().chooseItem(0);
        for (int i = 1; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            difficulty_buttons[i].getMenu().chooseItem(CLOSED_INDEX);
            race_buttons[i].getMenu().chooseItem(0);
            team_buttons[i].getMenu().chooseItem(i);
        }
        // Provide a sensible default opponent similar to TerrainMenu: slot 1 Easy AI, alternate race
        if (MatchmakingServerInterface.MAX_PLAYERS > 1) {
            difficulty_buttons[1].getMenu().chooseItem(COMPUTER_EASY_INDEX);
            int altRace = (race_buttons[0].getMenu().getChosenItemIndex() + 1) % RacesResources.getNumRaces();
            race_buttons[1].getMenu().chooseItem(altRace);
            team_buttons[1].getMenu().chooseItem(1);
        }
        // Hook difficulty change listeners to toggle row controls for Closed vs AI
        for (int i = 1; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            final int idx = i;
            difficulty_menus[i].addItemChosenListener(new com.oddlabs.tt.guievent.ItemChosenListener() {
                public void itemChosen(PulldownMenu menu, int item_index) {
                    // Only toggle row controls; difficulty pulldown stays enabled
                    boolean within = idx < player_count;
                    boolean rowEnabled = within && item_index != CLOSED_INDEX;
                    labels_players[idx].setDisabled(!rowEnabled);
                    race_buttons[idx].setDisabled(!rowEnabled);
                    team_buttons[idx].setDisabled(!rowEnabled);
                }
            });
        }
        refreshEnabledState();
    compileCanvas();

        // Update player_count when slots pulldown changes
        slots_menu.addItemChosenListener(new ItemChosenListener() {
            public void itemChosen(PulldownMenu menu, int item_index) {
                player_count = item_index + MIN_PLAYERS;
                refreshEnabledState();
            }
        });
    }

    private void refreshEnabledState() {
        // Within player_count, difficulty pulldown is enabled but race/team depend on Closed vs AI
        for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            boolean within = i < player_count;
            // Difficulty control is enabled for active rows; disabled beyond count
            difficulty_buttons[i].setDisabled(!within);
            if (i == 0) {
                // Local player row: label, race, team enabled only if within
                labels_players[i].setDisabled(!within);
                race_buttons[i].setDisabled(!within);
                team_buttons[i].setDisabled(!within);
            } else {
                boolean closed = difficulty_buttons[i].getMenu().getChosenItemIndex() == CLOSED_INDEX;
                boolean rowEnabled = within && !closed;
                labels_players[i].setDisabled(!rowEnabled);
                race_buttons[i].setDisabled(!rowEnabled);
                team_buttons[i].setDisabled(!rowEnabled);
            }
        }
    }

    public int getPlayerCount() { return player_count; }

    public boolean isHuman(int slot) { return slot == 0; }

    public boolean isClosed(int slot) {
        if (slot == 0) return false;
        return difficulty_buttons[slot].getMenu().getChosenItemIndex() == CLOSED_INDEX;
    }

    public boolean isAI(int slot) {
        if (slot == 0) return false;
        int idx = difficulty_buttons[slot].getMenu().getChosenItemIndex();
        return idx >= COMPUTER_EASY_INDEX && idx <= COMPUTER_HARD_INDEX;
    }

    public int getAIDifficultyIndex(int slot) {
        if (!isAI(slot)) return -1;
        // Match TerrainMenu: pass raw chosen index (1..3) to setPlayerSlot
        return difficulty_buttons[slot].getMenu().getChosenItemIndex();
    }

    public int getRaceIndex(int slot) {
        return race_buttons[slot].getMenu().getChosenItemIndex();
    }

    public int getTeamIndex(int slot) {
        return team_buttons[slot].getMenu().getChosenItemIndex();
    }

    public PulldownMenu getSlotsMenu() { return slots_menu; }
    public PulldownMenu getRaceMenu(int slot) { return race_buttons[slot].getMenu(); }
    public PulldownMenu getTeamMenu(int slot) { return team_buttons[slot].getMenu(); }
    public PulldownMenu getDifficultyMenu(int slot) { return difficulty_buttons[slot].getMenu(); }
}
