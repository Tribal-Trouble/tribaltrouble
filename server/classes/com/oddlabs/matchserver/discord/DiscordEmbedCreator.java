package com.oddlabs.matchserver.discord;

import com.oddlabs.matchmaking.GamePlayer;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.PlayerTypes;
import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.WebsiteLinkHelper;
import com.oddlabs.matchserver.models.GameDataModel;

import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.HashMap;
import java.util.Map;

/** Helpers for quickly and safely sending different Discord embed messages. */
public class DiscordEmbedCreator {
    /**
     * Returns a map of team index to a comma-separated string of player nicknames. Example: {0:
     * "Alice, Bob", 1: "Charlie, Dave"}
     */
    private static Map<Integer, String> getTeamLineup(GamePlayer[] players) {
        Map<Integer, String> teamPlayers = new HashMap<>();
        for (GamePlayer player : players) {
            int team = player.getTeam();

            // Add player to their team's list
            String currentTeamList = teamPlayers.getOrDefault(team, "");
            if (!currentTeamList.isEmpty()) {
                currentTeamList += ", ";
            }
            currentTeamList += player.getNick();
            teamPlayers.put(team, currentTeamList);
        }
        return teamPlayers;
    }

    /** Returns a formatted string of all human player nicknames. */
    public static String getFormattedHumanNicks(GamePlayer[] players) {
        StringBuilder allNicks = new StringBuilder();

        for (GamePlayer player : players) {
            if (player.getPlayerType() == PlayerTypes.Human) {
                if (allNicks.length() > 0) {
                    allNicks.append(", ");
                }
                allNicks.append(player.getNick());
            }
        }
        return allNicks.toString();
    }

    /** Sends a Discord embed message when humans lose to bots. */
    public static void SendHumansLoseToBotsDiscordEmbed(int game_id, GameSession session) {
        if (!DiscordBotService.getInstance().isInitialized()) return;
        GameDataModel data = DBInterface.getGame(game_id, true);
        String game_name = data.getName();

        Map<Integer, String> playerData = getTeamLineup(session.getPlayerInfo());

        String replayUrl = WebsiteLinkHelper.getReplayUrl(game_id);
        String description =
                getFormattedHumanNicks(session.getPlayerInfo()) + " lost playing against AI";
        if (replayUrl != null) {
            description += String.format("\n[Watch here](%s)", replayUrl);
        }
        discord4j.core.spec.EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder()
                        .color(Color.GREEN)
                        .title(game_name)
                        .description(description);

        // Add fields for each team
        for (Map.Entry<Integer, String> entry : playerData.entrySet()) {
            int teamId = entry.getKey();
            String playerList = entry.getValue();
            builder.addField("Team " + (teamId + 1), playerList, false);
        }

        EmbedCreateSpec embed = builder.build();
        TextChannel gameActivityChannel =
                DiscordBotService.getInstance().getGameActivityChannel().orElse(null);
        DiscordBotService.getInstance()
                .getChatroomCoordinator()
                .ifPresent(coordinator -> coordinator.sendDiscordEmbed(gameActivityChannel, embed));
    }

    /**
     * Humans win against humans (and possibly bots)
     *
     * @param winning_team_index
     */
    public static void SendHumansWinAgainstOtherHumans(
            int winning_team_index, int game_id, GameSession session) {
        if (!DiscordBotService.getInstance().isInitialized()) return;

        GameDataModel data = DBInterface.getGame(game_id, false);
        String game_name = data.getName();

        Map<Integer, String> playerData = getTeamLineup(session.getPlayerInfo());

        String replayUrl = WebsiteLinkHelper.getReplayUrl(game_id);
        String description = "Team " + (winning_team_index + 1) + " won";
        if (replayUrl != null) {
            description += String.format("\n[Watch here](%s)", replayUrl);
        }
        discord4j.core.spec.EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder()
                        .color(Color.GREEN)
                        .title(game_name)
                        .description(description);

        // Add fields for each team
        for (Map.Entry<Integer, String> entry : playerData.entrySet()) {
            int teamId = entry.getKey();
            String playerList = entry.getValue();
            builder.addField("Team " + (teamId + 1), playerList, false);
        }

        EmbedCreateSpec embed = builder.build();
        TextChannel gameActivityChannel =
                DiscordBotService.getInstance().getGameActivityChannel().orElse(null);
        DiscordBotService.getInstance()
                .getChatroomCoordinator()
                .ifPresent(coordinator -> coordinator.sendDiscordEmbed(gameActivityChannel, embed));
    }

    /** Sends a Discord embed message when humans win against bots. */
    public static void SendHumansWinAgainstBotsDiscordEmbed(
            int winning_team_index, int game_id, GameSession session) {
        if (!DiscordBotService.getInstance().isInitialized()) return;
        GameDataModel data = DBInterface.getGame(game_id, true);
        String game_name = data.getName();

        Map<Integer, String> playerData = getTeamLineup(session.getPlayerInfo());

        String replayUrl = WebsiteLinkHelper.getReplayUrl(game_id);
        String description = "Team " + (winning_team_index + 1) + " won playing against AI";
        if (replayUrl != null) {
            description += String.format("\n[Watch here](%s)", replayUrl);
        }
        discord4j.core.spec.EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder()
                        .color(Color.GREEN)
                        .title(game_name)
                        .description(description);

        // Add fields for each team
        for (Map.Entry<Integer, String> entry : playerData.entrySet()) {
            int teamId = entry.getKey();
            String playerList = entry.getValue();
            builder.addField("Team " + (teamId + 1), playerList, false);
        }

        EmbedCreateSpec embed = builder.build();
        TextChannel gameActivityChannel =
                DiscordBotService.getInstance().getGameActivityChannel().orElse(null);
        DiscordBotService.getInstance()
                .getChatroomCoordinator()
                .ifPresent(coordinator -> coordinator.sendDiscordEmbed(gameActivityChannel, embed));
    }

    /** Sends a Discord embed message when the game was invalidated. */
    public static void SendInvalidatedGameDiscordEmbed(GameSession session, int database_id) {
        if (!DiscordBotService.getInstance().isInitialized()) return;
        GameDataModel data = DBInterface.getGame(database_id, true);
        String game_name = data.getName();
        Map<Integer, String> playerData = getTeamLineup(session.getPlayerInfo());
        String replayUrl = WebsiteLinkHelper.getReplayUrl(database_id);
        String description = "The game was invalidated. Someone may have cheated!";
        if (replayUrl != null) {
            description += String.format("\n[Watch here](%s)", replayUrl);
        }
        discord4j.core.spec.EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder()
                        .color(Color.RED)
                        .title(game_name)
                        .description(description);

        // Add fields for each team
        for (Map.Entry<Integer, String> entry : playerData.entrySet()) {
            int teamId = entry.getKey();
            String playerList = entry.getValue();
            builder.addField("Team " + (teamId + 1), playerList, false);
        }

        EmbedCreateSpec embed = builder.build();

        TextChannel gameActivityChannel =
                DiscordBotService.getInstance().getGameActivityChannel().orElse(null);
        DiscordBotService.getInstance()
                .getChatroomCoordinator()
                .ifPresent(coordinator -> coordinator.sendDiscordEmbed(gameActivityChannel, embed));
    }

    public static void SendGameStartedDiscordEmbed(int game_id, GameSession session) {
        if (!DiscordBotService.getInstance().isInitialized()) return;
        GameDataModel data = DBInterface.getGame(game_id, false);
        if (data == null) return; // If the game name is null, we can't send an embed
        String game_name = data.getName();

        Map<Integer, String> playerData = getTeamLineup(session.getPlayerInfo());

        String replayUrl = WebsiteLinkHelper.getReplayUrl(game_id);
        String description = "Game started!";
        if (replayUrl != null) {
            description += String.format("\n[Watch here](%s)", replayUrl);
        }
        discord4j.core.spec.EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder().title(game_name).description(description);

        // Add fields for each team
        for (Map.Entry<Integer, String> entry : playerData.entrySet()) {
            int teamId = entry.getKey();
            String playerList = entry.getValue();
            builder.addField("Team " + (teamId + 1), playerList, false);
        }

        EmbedCreateSpec embed = builder.build();

        TextChannel gameActivityChannel =
                DiscordBotService.getInstance().getGameActivityChannel().orElse(null);
        DiscordBotService.getInstance()
                .getChatroomCoordinator()
                .ifPresent(coordinator -> coordinator.sendDiscordEmbed(gameActivityChannel, embed));
    }
}
