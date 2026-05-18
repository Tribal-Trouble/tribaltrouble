package com.oddlabs.tt.trigger;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.player.AdvancedAI;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.steam.SteamAchievementNames;
import com.oddlabs.tt.steam.SteamManager;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.ResourceBundle;

public final class GameOverTrigger implements Animated {

    private final int @NonNull [] teams;
    private final boolean @NonNull [] dead_tribes;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(GameOverTrigger.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull WorldViewer viewer;

    public GameOverTrigger(@NonNull WorldViewer viewer) {
        this.viewer = viewer;
        viewer.getWorld().getAnimationManagerRealTime().registerAnimation(this);
        teams = new int[MatchmakingServerInterface.MAX_PLAYERS];
        dead_tribes = new boolean[viewer.getWorld().getPlayers().length];
        Arrays.fill(dead_tribes, false);
    }

    @Override
    public void animate(float t) {
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
                        String defeat_message = i18n("defeat_message", current.getPlayerInfo().getName());
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

    private void tryUnlockAchievements(@NonNull Player local_player, Player @NonNull [] players) {
        if (SteamManager.getInstance() == null) return;
        // Ludicrous speed only
        if (viewer.getWorld().getGamespeed() != 4) return;

        int ai_team = -1;
        boolean is_player_alone = true;
        boolean all_hards_same_team = true;
        int hard_ais_on_same_team = 0;
        int current_player_team = local_player.getPlayerInfo().getTeam();

        for (Player current : players) {
            if (current != local_player && current.getPlayerInfo().getTeam() == current_player_team) {
                is_player_alone = false;
                break;
            }

            if (current != local_player
                    && current.getAI() instanceof AdvancedAI ai
                    && ai.getDifficulty() == AdvancedAI.DIFFICULTY_HARD) {
                if (ai_team == -1) {
                    ai_team = current.getPlayerInfo().getTeam();
                    hard_ais_on_same_team++;
                } else if (current.getPlayerInfo().getTeam() == ai_team) {
                    hard_ais_on_same_team++;
                } else {
                    all_hards_same_team = false;
                }
            }
        }

        if (!is_player_alone || !all_hards_same_team) return;

        int map_size = viewer.getWorld().getMapSize();
        if (hard_ais_on_same_team >= 3 && map_size == Game.SIZE_SMALL) {
            SteamManager.unlockAchievement(SteamAchievementNames.BEAT_3_HARDS_ON_SMALL);
        } else if (hard_ais_on_same_team >= 5 && map_size == Game.SIZE_MEDIUM) {
            SteamManager.unlockAchievement(SteamAchievementNames.BEAT_5_HARDS_ON_MEDIUM);
        }
    }

    private int countTeams(Player @NonNull [] players) {
        for (int i = 0; i < players.length; i++) {
            teams[i] = 0;
        }

        for (Player current : players) {
            if (viewer.getPeerHub().isAlive(current) && current.getPlayerInfo().getTeam() != PlayerInfo.TEAM_NEUTRAL)
                teams[current.getPlayerInfo().getTeam()]++;
        }

        int team_count = 0;
        for (int team : teams) {
            if (team > 0)
                team_count++;
        }
        return team_count;
    }

    public void disable() {
        viewer.getWorld().getAnimationManagerRealTime().removeAnimation(this);
    }

    private void createDelayTrigger(@NonNull String text) {
        GUIRoot gui_root = viewer.getGUIRoot();
        new GameOverDelayTrigger(viewer, gui_root.getDelegate().getCamera(), text);
    }

    private void doGameOver(int team_count) {
        viewer.getPeerHub().leaveGame();
        if (team_count < 2) {
            createDelayTrigger(i18n("you_defeated_game_over"));
        } else {
            createDelayTrigger(i18n("you_defeated"));
        }
        disable();
    }

    private void doGameWon() {
        viewer.getPeerHub().gameWon();
        createDelayTrigger(i18n("you_victorious"));
        disable();
    }

    private void stop() {
        createDelayTrigger(i18n("game_over"));
        disable();
    }
}
