package com.oddlabs.tt.delegate;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.form.CampaignForm;
import com.oddlabs.tt.form.LoginForm;
import com.oddlabs.tt.form.MatchmakingConnectingForm;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.SelectGameMenu;
import com.oddlabs.tt.form.TerrainMenuForm;
import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.net.Client;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.p2p.P2P;
import com.oddlabs.tt.p2p.P2PProvider;
import com.oddlabs.tt.steam.SteamManager;
import org.jspecify.annotations.NonNull;

/**
 * The game main menu
 */
public final class MainMenu extends Menu {
    public MainMenu(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull Camera camera) {
        super(network, gui_root, camera);
        reload();
        SteamManager.clearRichPresence();
        SteamManager.setInActiveWorld(false);
        P2P.get().leave();
        P2P.get().setJoinHandler(new P2PProvider.JoinHandler() {
            @Override
            public void lobbyJoined(P2PProvider.@NonNull JoinInfo info) {
                if (Network.getMatchmakingClient().isConnected()) {
                    // Online mode owns live server state (login, chat, possibly a game lobby); a P2P
                    // match on top would strand all of it. Refuse loudly instead of tearing it down.
                    P2P.get().leave();
                    gui_root.addModalForm(new MessageForm(Menu.i18n("leave_online_first")));
                    return;
                }
                joinGame(network, gui_root.getGUI(), Client.P2P_HOST_ID, false, info.gamespeed(), info.mapcode(),
                        null, info.randomStartPos(), info.maxUnitCount(), info.size());
            }

            @Override
            public void versionMismatch() {
                gui_root.addModalForm(new MessageForm(Menu.i18n("p2p_version_mismatch")));
            }
        });
    }

    private void addGameTypeButtons() {
        MenuButton tutorial = new MenuButton(Menu.i18n("tutorial"), COLOR_NORMAL, COLOR_ACTIVE);
        tutorial.addMouseClickListener((_, _, _, _) -> setMenu(new TutorialForm(getNetwork(), getGUIRoot())));
        addChild(tutorial);

        MenuButton campaign_menu = new MenuButton(Menu.i18n("campaign"), COLOR_NORMAL, COLOR_ACTIVE);
        campaign_menu.addMouseClickListener((_, _, _, _) -> setMenu(new CampaignForm(getNetwork(), getGUIRoot(),
                MainMenu.this)));
        addChild(campaign_menu);

        MenuButton single_player = new MenuButton(Menu.i18n("skirmish"), COLOR_NORMAL, COLOR_ACTIVE);
        single_player.addMouseClickListener((_, _, _, _) -> setMenu(new TerrainMenuForm(getNetwork(), getGUIRoot(),
                MainMenu.this)));
        addChild(single_player);

        if (P2P.get().isAvailable()) {
            MenuButton play_with_friends = new MenuButton(Menu.i18n("friends_on", P2P.get().getPlatformName()),
                    COLOR_NORMAL, COLOR_ACTIVE);
            play_with_friends.addMouseClickListener((_, _, _, _) -> setMenu(new TerrainMenuForm(getNetwork(),
                    getGUIRoot(), MainMenu.this, true)));
            addChild(play_with_friends);
        }

        if (!Settings.getSettings().hide_multiplayer) {
            MenuButton multi_player = new MenuButton(Menu.i18n("multiplayer"), COLOR_NORMAL, COLOR_ACTIVE);
            multi_player.addMouseClickListener((_, _, _, _) -> {
                if (Network.getMatchmakingClient().isConnected()) {
                    new SelectGameMenu(getNetwork(), getGUIRoot(), MainMenu.this);
                } else {
                    Network.getMatchmakingClient().close();
                    if (Settings.getSettings().isOfficialServer() && SteamManager.getInstance() != null) {
                        new MatchmakingConnectingForm(getNetwork(), getGUIRoot(), null, MainMenu.this, true);
                    } else {
                        new LoginForm(getNetwork(), getGUIRoot(), MainMenu.this);
                    }
                }
            });
            addChild(multi_player);
        }
    }

    @Override
    protected void addButtons() {
        addGameTypeButtons();

        addDefaultOptionsButton();

        addExitButton();

        if (Network.getMatchmakingClient().isConnected()) {
            new SelectGameMenu(getNetwork(), getGUIRoot(), this);
        }
    }
}
