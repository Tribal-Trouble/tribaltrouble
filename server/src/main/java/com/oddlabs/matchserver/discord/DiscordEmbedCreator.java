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
import java.util.StringJoiner;

/**
 * Helpers for quickly and safely sending different Discord embed messages.
 */
public class DiscordEmbedCreator {

    private DiscordEmbedCreator() {
    }

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

    /**
     * Returns a formatted string of all human player nicknames.
     */
    public static String getFormattedHumanNicks(GamePlayer[] players) {
        StringJoiner allNicks = new StringJoiner(", ");

        for (GamePlayer player : players) {
            if (player.getPlayerType() == PlayerTypes.Human) {
                if (allNicks.length() > 0) {
                    allNicks.add(player.getNick());
                }
            }
        }
        return allNicks.toString();
    }

    /**
     * Core builder/sender to eliminate repetition.
     *
     * @param gameId               The ID of the game.
     * @param includeMapCodeLookup Whether to include a link to look up the map code
     * @param session              The game session object.
     * @param color                The color of the embed sidebar.
     * @param descriptionPrefix    A prefix for the description, e.g. "Team 2 Won" or "Player lost
     *                             playing vs AI".
     */
    private static void buildAndSendEmbed(
            int gameId,
            Boolean includeMapCodeLookup,
            GameSession session,
            Color color,
            String descriptionPrefix // eg. Team 2 Won, or Player lost playing vs AI
    ) {

        if (!DiscordBotService.getInstance().isInitialized()) {
            return;
        }
        // Fetch game data
        GameDataModel data = DBInterface.getGame(gameId, true);
        String gameName = data.getName();
        String mapCode = includeMapCodeLookup ? data.getMapcode() : null;
        String replayUrl = WebsiteLinkHelper.getReplayUrl(gameId);

        StringBuilder desc = new StringBuilder(descriptionPrefix);

        if (replayUrl != null) {
            desc.append("\n[Watch here](").append(replayUrl).append(")");
        }

        // Start building the embed

        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder().title(gameName).color(color).description(
                desc.toString());

        // Add a field for each team
        Map<Integer, String> teamLineup = getTeamLineup(session.getPlayerInfo());
        for (Map.Entry<Integer, String> entry : teamLineup.entrySet()) {
            String teamName = "Team " + (entry.getKey() + 1); // Teams are 0-indexed internally
            String playerNicks = entry.getValue();
            embed.addField(teamName, playerNicks, false);
        }

        // Add a footer with the map code if applicable
        if (mapCode != null && !mapCode.isEmpty()) {
            embed.addField("Map Code: ", mapCode, false);
        }

        EmbedCreateSpec builtEmbed = embed.build();

        // Send to the Correct Channel
        TextChannel gameActivityChannel = DiscordBotService.getInstance().getGameActivityChannel().orElse(null);

        DiscordBotService.getInstance().getChatroomCoordinator().ifPresent(
                coordinator -> coordinator.sendDiscordEmbed(gameActivityChannel, builtEmbed));
    }

    /**
     * Sends a Discord embed message when humans lose to bots
     */
    public static void SendHumansLoseToBotsDiscordEmbed(GameSession session, int gameID) {
        String nicks = getFormattedHumanNicks(session.getPlayerInfo());
        String prefix = (nicks.isEmpty() ? "Human" : nicks) + " lost playing against AI!";
        buildAndSendEmbed(gameID, true, session, Color.RED, prefix);
    }

    /**
     * Sends a Discord embed message when humans Win Vs Humans (and maybe bots)
     */
    public static void SendHumansWinAgainstOtherHumans(
            int winning_team_index, GameSession session, int gameID) {
        String prefix = "Team " + (winning_team_index + 1) + " Won!";
        buildAndSendEmbed(gameID, true, session, Color.GREEN, prefix);
    }

    /**
     * Sends a Discord embed message when Humans Win Vs Bots
     */
    public static void SendHumansWinAgainstBotsDiscordEmbed(
            int winning_team_index, GameSession session, int gameID) {
        String prefix = "Team " + (winning_team_index + 1) + " Won playing against AI!";
        buildAndSendEmbed(gameID, true, session, Color.GREEN, prefix);
    }

    /**
     * Sends a Discord embed message when the game was invalidated
     */
    public static void SendInvalidatedGameDiscordEmbed(GameSession session, int gameID) {
        String prefix = "Game Invalidated! Someone may have cheated!";
        buildAndSendEmbed(gameID, true, session, Color.RED, prefix);
    }

    /**
     * Sends a Discord embed message when the game starts.
     */
    public static void SendGameStartedDiscordEmbed(GameSession session, int gameID) {
        String prefix = "Game Started!";
        buildAndSendEmbed(gameID, true, session, Color.GRAY, prefix);
    }
}
