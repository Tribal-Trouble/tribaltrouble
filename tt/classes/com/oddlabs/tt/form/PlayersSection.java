package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
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
    public static final int OPEN_INDEX = 0; // human for slot 0, open/closed for others
    public static final int CLOSED_INDEX = 1;
    public static final int COMPUTER_EASY_INDEX = 2;
    public static final int COMPUTER_NORMAL_INDEX = 3;
    public static final int COMPUTER_HARD_INDEX = 4;

    private final GUIRoot guiRoot;
    private final ResourceBundle bundle;

    private final Label[] labels_players;
    private final PulldownMenu[] difficulty_menus; // 0: human/closed, 1..: AI
    private final PulldownMenu[] race_menus;
    private final PulldownMenu[] team_menus;
    private final PulldownButton[] difficulty_buttons;
    private final PulldownButton[] race_buttons;
    private final PulldownButton[] team_buttons;

    private final PulldownMenu slots_menu;
    private final PulldownButton slots_button;

    private int player_count = 6; // mirror TerrainMenu default

    public PlayersSection(GUIRoot guiRoot) {
        this(guiRoot, ResourceBundle.getBundle(TerrainMenu.class.getName()));
    }

    public PlayersSection(GUIRoot guiRoot, ResourceBundle bundle) {
        this.guiRoot = guiRoot;
        this.bundle = bundle;

        // Player count row
        Label label_player_slots = new Label("Players", Skin.getSkin().getEditFont());
        Group group_num_players = new Group();
        group_num_players.addChild(label_player_slots);
        label_player_slots.place();

        slots_menu = new PulldownMenu();
        for (int i = 1; i <= MatchmakingServerInterface.MAX_PLAYERS; i++) {
            slots_menu.addItem(new PulldownItem(Integer.toString(i)));
        }
        slots_button = new PulldownButton(guiRoot, slots_menu, 5, 150);
        group_num_players.addChild(slots_button);
    slots_button.place(label_player_slots, GUIObject.RIGHT_MID);
        group_num_players.compileCanvas();
        addChild(group_num_players);

        // Players grid: label + [human/ai] [race] [team]
        Group group_race_team = new Group();
        labels_players = new Label[MatchmakingServerInterface.MAX_PLAYERS];
        difficulty_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        race_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        team_menus = new PulldownMenu[MatchmakingServerInterface.MAX_PLAYERS];
        difficulty_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
        race_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];
        team_buttons = new PulldownButton[MatchmakingServerInterface.MAX_PLAYERS];

        Font font = Skin.getSkin().getEditFont();
        for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            difficulty_menus[i] = new PulldownMenu();
            race_menus[i] = new PulldownMenu();
            team_menus[i] = new PulldownMenu();

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
            labels_players[i].setColor(Player.COLORS[i]);
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
    group_num_players.place();
    group_race_team.place(
        group_num_players,
        GUIObject.BOTTOM_LEFT,
        Skin.getSkin().getFormData().getSectionSpacing());

        // Defaults: slot 0 human, team 1; others closed and team=slot index+1
        slots_menu.chooseItem(player_count - 1);
        race_buttons[0].getMenu().chooseItem(0);
        team_buttons[0].getMenu().chooseItem(0);
        for (int i = 1; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            difficulty_buttons[i].getMenu().chooseItem(CLOSED_INDEX);
            race_buttons[i].getMenu().chooseItem(0);
            team_buttons[i].getMenu().chooseItem(i);
        }
    refreshEnabledState();
    compileCanvas();

        // Update player_count when slots pulldown changes
        slots_menu.addItemChosenListener(new ItemChosenListener() {
            public void itemChosen(PulldownMenu menu, int item_index) {
                player_count = item_index + 1;
                refreshEnabledState();
            }
        });
    }

    private void refreshEnabledState() {
        // Show first player_count rows as active; others dim/disable
        for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            boolean enabled = i < player_count;
            labels_players[i].setDisabled(!enabled);
            difficulty_buttons[i].setDisabled(!enabled);
            race_buttons[i].setDisabled(!enabled);
            team_buttons[i].setDisabled(!enabled);
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
        // TerrainMenu stores difficulty as (chosenIndex - 1)
        return difficulty_buttons[slot].getMenu().getChosenItemIndex() - 1;
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
