package com.oddlabs.tt.trigger;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.player.AdvancedAI;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.steam.SteamAchievementManager;
import com.oddlabs.tt.steam.SteamAchievementNames;
import com.oddlabs.tt.steam.SteamUtils;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;

import java.util.ResourceBundle;

public final strictfp class GameOverTrigger implements Animated {
    private final int[] teams;
    private final boolean[] dead_tribes;
    private final ResourceBundle bundle = ResourceBundle.getBundle(GameOverTrigger.class.getName());
    private final WorldViewer viewer;

    public GameOverTrigger(WorldViewer viewer) {
        this.viewer = viewer;
        viewer.getWorld().getAnimationManagerRealTime().registerAnimation(this);
        teams = new int[MatchmakingServerInterface.MAX_PLAYERS];
        dead_tribes = new boolean[viewer.getWorld().getPlayers().length];
        for (int i = 0; i < dead_tribes.length; i++) dead_tribes[i] = false;
    }

    public final void animate(float t) {
        Player[] players = viewer.getWorld().getPlayers();
        Player local_player = viewer.getLocalPlayer();
        boolean enemy_alive = false;

        for (int i = 0; i < players.length; i++) {
            Player current = players[i];
            if (!dead_tribes[i]) {
                if (!viewer.getPeerHub().isAlive(current)) {
                    if (current == local_player) {
                        doGameOver(countTeams(players));
                        return;
                    } else {
                        dead_tribes[i] = true;
                        String defeat_message =
                                Utils.getBundleString(
                                        bundle,
                                        "defeat_message",
                                        new Object[] {current.getPlayerInfo().getName()});
                        viewer.getPeerHub().receiveChat(PeerHub.SYSTEM_NAME, defeat_message, false);
                    }
                } else if (local_player.isEnemy(current)) {
                    enemy_alive = true;
                }
            }
        }
        if (!enemy_alive) {
            tryUnlockAchievements(local_player, players);
            doGameWon();
            return;
        }
        if (countTeams(players) < 2) {
            stop();
        }
    }

    /** Unlock achievements that may occur after a game is won or lost */
    private void tryUnlockAchievements(Player local_player, Player[] players) {
        // If ludicrous speed
        if (viewer.getWorld().getGamespeed() == 4) {
            // Small Island

            int ai_team = -1;
            // Check for 3 hard ais on the same team and only a single player
            boolean is_player_alone = true;
            boolean all_hards_same_team = true;
            int hard_ais_on_same_team = 0;
            int current_player_team = local_player.getPlayerInfo().getTeam();
            for (Player current : players) {
                if (current != local_player
                        && current.getPlayerInfo().getTeam() == current_player_team) {
                    is_player_alone = false;
                    break;
                }

                if (current != local_player
                        && current.getAI() instanceof AdvancedAI
                        && ((AdvancedAI) current.getAI()).getDifficulty()
                                == AdvancedAI.DIFFICULTY_HARD) {
                    if (ai_team == -1) {
                        ai_team = current.getPlayerInfo().getTeam();
                        hard_ais_on_same_team++;
                    } else if (current.getPlayerInfo().getTeam() == ai_team) {
                        hard_ais_on_same_team++;
                    } else {
                        // Hard AI on different team
                        all_hards_same_team = false;
                    }
                }
            }
            if (hard_ais_on_same_team >= 3
                    && is_player_alone
                    && all_hards_same_team
                    && viewer.getWorld().getMapSize() == Game.SIZE_SMALL) {
                SteamAchievementManager.getAchievementManager()
                        .unlockAchievement(SteamAchievementNames.BEAT_3_HARDS_ON_SMALL);
            } else if (hard_ais_on_same_team >= 5
                    && is_player_alone
                    && all_hards_same_team
                    && viewer.getWorld().getMapSize() == Game.SIZE_MEDIUM) {
                SteamAchievementManager.getAchievementManager()
                        .unlockAchievement(SteamAchievementNames.BEAT_5_HARDS_ON_MEDIUM);
            }
        }
    }

    private final int countTeams(Player[] players) {
        for (int i = 0; i < players.length; i++) {
            teams[i] = 0;
        }

        for (int i = 0; i < players.length; i++) {
            Player current = players[i];
            if (viewer.getPeerHub().isAlive(current)
                    && current.getPlayerInfo().getTeam() != PlayerInfo.TEAM_NEUTRAL)
                teams[current.getPlayerInfo().getTeam()]++;
        }

        int team_count = 0;
        for (int i = 0; i < teams.length; i++) {
            if (teams[i] > 0) team_count++;
        }
        return team_count;
    }

    public final void updateChecksum(StateChecksum checksum) {}

    public final void disable() {
        viewer.getWorld().getAnimationManagerRealTime().removeAnimation(this);
    }

    private final void createDelayTrigger(String text) {
        GUIRoot gui_root = viewer.getGUIRoot();
        new GameOverDelayTrigger(viewer, gui_root.getDelegate().getCamera(), text);
    }

    private final void doGameOver(int team_count) {
        viewer.getPeerHub().leaveGame();
        if (team_count < 2) {
            createDelayTrigger(Utils.getBundleString(bundle, "you_defeated_game_over"));
        } else {
            createDelayTrigger(Utils.getBundleString(bundle, "you_defeated"));
        }
        SteamUtils.trySteamStatAndAchievementsOnLoss(viewer.getWorld(), viewer.getLocalPlayer());
        disable();
    }

    private final void doGameWon() {
        viewer.getPeerHub().gameWon();
        SteamUtils.trySteamStatAndAchievementsOnWin(viewer.getWorld(), viewer.getLocalPlayer());
        createDelayTrigger(Utils.getBundleString(bundle, "you_victorious"));
        disable();
    }

    private final void stop() {
        createDelayTrigger(Utils.getBundleString(bundle, "game_over"));
        disable();
    }
}
