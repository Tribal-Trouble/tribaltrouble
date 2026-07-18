package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.RosterTemplate;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.steam.SteamLobbySession;
import org.jspecify.annotations.NonNull;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

/**
 * Hosts the game negotiation panel for a serverless Steam match, standing in for the
 * SelectGameMenu panel slot that normal matchmaking games use. Also owns the invite entry point,
 * since without a matchmaking game browser the Steam overlay invite is the only way in.
 */
public final class SteamGameForm extends Form {
    // Three of these plus spacing must fit inside the panel width; GameMenu puts Game info on its
    // own row for the Steam lobby.
    private static final int BUTTON_WIDTH = 170;
    private static final int PANEL_COMPARE_WIDTH = 620;
    private static final int PANEL_COMPARE_HEIGHT = 460;

    public SteamGameForm(@NonNull GameNetwork game_network, @NonNull GUIRoot gui_root, @NonNull Game game,
            WorldGenerator generator, int player_slot, int player_count) {
        GameMenu game_menu = new GameMenu(game_network, gui_root, null, game, generator, player_slot,
                PANEL_COMPARE_WIDTH, PANEL_COMPARE_HEIGHT, BUTTON_WIDTH, player_count);
        game_menu.setCloseAction(() -> {
            remove();
            SteamLobbySession.leave();
        });
        addChild(game_menu);
        game_network.getClient().setConfigurationListener(game_menu);
        RosterTemplate initial_roster = game_network.getInitialRoster();
        if (initial_roster != null) {
            game_menu.applyInitialRoster(initial_roster);
        }

        game_menu.place();
        if (SteamLobbySession.isHost()) {
            HorizButton invite_button = new HorizButton("Invite friends", BUTTON_WIDTH);
            invite_button.addMouseClickListener((_, _, _, _) -> SteamLobbySession.openInviteDialog());
            addChild(invite_button);
            invite_button.place(game_menu, BOTTOM_MID);
        }
        compileCanvas();
        centerPos();

        if (SteamLobbySession.isHost())
            SteamLobbySession.openInviteDialog();
    }
}
