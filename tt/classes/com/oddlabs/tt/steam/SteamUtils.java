package com.oddlabs.tt.steam;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.player.Player;
import com.codedisaster.steamworks.SteamAPI;

public class SteamUtils {
    public static void trySteamStatAndAchievementsOnLoss(World viewer, Player local_player) {
        if(SteamAchievementManager.getAchievementManager() == null || !SteamAPI.isSteamRunning())
            return;
        SteamAchievementManager.debugPrint("Loss! Updating steam stat losses!");
        Player[] players = viewer.getPlayers();
        boolean another_opposing_human = false;
        for (Player player : players) {
            if (player != local_player && local_player.isEnemy(player) && player.getAI() == null) {
                another_opposing_human = true;
                break;
            }
        }
        SteamAchievementManager.debugPrint("another_opposing_human: " + another_opposing_human);
        if(another_opposing_human) {
            SteamAchievementManager.debugPrint("Lost against another human! Updating steam stat losses!");
            int losses = SteamAchievementManager.getAchievementManager().getStat(SteamStatNames.MP_TOTAL_LOSSES, 0);
            int new_losses = losses + 1;
            SteamAchievementManager.getAchievementManager().setStat(SteamStatNames.MP_TOTAL_LOSSES, new_losses);
            if(new_losses <= 100)
                SteamAchievementManager.getAchievementManager().updateAchievementProgress(SteamAchievementNames.LOSE_100_MULTIPLAYER_GAMES, new_losses, 100);
            SteamAchievementManager.debugPrint("Resetting current win streak to 0");

            SteamAchievementManager.getAchievementManager().setStat(SteamStatNames.MP_CURRENT_WIN_STREAK, 0);
            if(new_losses >= 100) {
                SteamAchievementManager.debugPrint("Unlocked achievement: LOSE_100_MULTIPLAYER_GAMES");
                SteamAchievementManager.getAchievementManager().unlockAchievement(SteamAchievementNames.LOSE_100_MULTIPLAYER_GAMES);
            }
        }
    }

    
    public static void trySteamStatAndAchievementsOnWin(World viewer, Player local_player) {
        if(SteamAchievementManager.getAchievementManager() == null || !SteamAPI.isSteamRunning())
            return;
        SteamAchievementManager.debugPrint("Win! Updating steam stats and achievements");
        Player[] players = viewer.getPlayers();
        boolean another_opposing_human = false;
        for (Player player : players) {
            if (player != local_player && local_player.isEnemy(player) && player.getAI() == null) {
                another_opposing_human = true;
                break;
            }
        }
        SteamAchievementManager.debugPrint("another_opposing_human: " + another_opposing_human);
        if(another_opposing_human) {
            SteamAchievementManager.debugPrint("Won against another human! Updating steam stat wins!");
            int old_wins = SteamAchievementManager.getAchievementManager().getStat(SteamStatNames.MP_TOTAL_WINS, 0);
            int new_wins = old_wins + 1;
            int current_streak = SteamAchievementManager.getAchievementManager().getStat(SteamStatNames.MP_CURRENT_WIN_STREAK, 0);
            int new_streak = current_streak + 1;
            SteamAchievementManager.getAchievementManager().setStat(SteamStatNames.MP_TOTAL_WINS, new_wins);
            SteamAchievementManager.getAchievementManager().setStat(SteamStatNames.MP_CURRENT_WIN_STREAK, new_streak);
            if(new_streak <= 10)
                SteamAchievementManager.getAchievementManager().updateAchievementProgress(SteamAchievementNames.WIN_STREAK_10, current_streak, 10);

            if(new_wins <= 10)
                SteamAchievementManager.getAchievementManager().updateAchievementProgress(SteamAchievementNames.WIN_10_MULTIPLAYER_GAMES, new_wins, 10);
            if(new_wins <= 100)
                SteamAchievementManager.getAchievementManager().updateAchievementProgress(SteamAchievementNames.WIN_100_MULTIPLAYER_GAMES, new_wins, 100);
            if(new_wins <= 1000)
                SteamAchievementManager.getAchievementManager().updateAchievementProgress(SteamAchievementNames.WIN_1000_MULTIPLAYER_GAMES, new_wins, 1000);
            
            if(new_wins >= 10) {
                SteamAchievementManager.debugPrint("Unlocked achievement: WIN_10_MULTIPLAYER_GAMES");
                SteamAchievementManager.getAchievementManager().unlockAchievement(SteamAchievementNames.WIN_10_MULTIPLAYER_GAMES);
            }
            if(new_wins >= 100) {
                SteamAchievementManager.debugPrint("Unlocked achievement: WIN_100_MULTIPLAYER_GAMES");
                SteamAchievementManager.getAchievementManager().unlockAchievement(SteamAchievementNames.WIN_100_MULTIPLAYER_GAMES);
            }
            if(new_wins >= 1000) {
                SteamAchievementManager.debugPrint("Unlocked achievement: WIN_1000_MULTIPLAYER_GAMES");
                SteamAchievementManager.getAchievementManager().unlockAchievement(SteamAchievementNames.WIN_1000_MULTIPLAYER_GAMES);
            }
            if(new_streak >= 10) {
                SteamAchievementManager.debugPrint("Unlocked achievement: WIN_STREAK_10");
                SteamAchievementManager.getAchievementManager().unlockAchievement(SteamAchievementNames.WIN_STREAK_10);
            }

            int best_streak = SteamAchievementManager.getAchievementManager().getStat(SteamStatNames.MP_BEST_WIN_STREAK, 0);            
            if(new_streak > best_streak) {
                SteamAchievementManager.debugPrint("New best win streak: " + new_streak);
                SteamAchievementManager.getAchievementManager().setStat(SteamStatNames.MP_BEST_WIN_STREAK, new_streak);
            }
        }
    }
}
