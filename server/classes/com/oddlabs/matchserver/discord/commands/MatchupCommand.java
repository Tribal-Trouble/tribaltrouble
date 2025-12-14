package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.WebsiteLinkHelper;
import com.oddlabs.matchserver.models.VersusMatchupModel;
import com.oddlabs.matchserver.models.VersusMatchupResultModel;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;

public class MatchupCommand extends DiscordCommand {
    private String command_name = "matchup";
    private String command_description =
            "Displays the win loss record for a player vs another player";
    private String command_option_p1 = "player1";
    private String command_option_p2 = "player2";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String player1 =
                event.getOption(command_option_p1)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse(null);
        String player2 =
                event.getOption(command_option_p2)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse(null);
        if (player1 == null || player2 == null)
            return event.reply("Both player1 and player2 options are required.");

        Profile[] profiles = DBInterface.getProfilesByNick(new String[] {player1, player2});
        if (profiles.length < 2) {
            return event.reply("Could not find requested players");
        }

        VersusMatchupResultModel matchupResult =
                DBInterface.getMatchupStats(player1, player2, true);
        EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder()
                        .color(Color.ORANGE)
                        .title(String.format("%s vs %s", player1, player2))
                        .description(
                                String.format(
                                        "Comparing 1v1 stats for %s and %s",
                                        WebsiteLinkHelper.getProfileLink(player1, player1),
                                        WebsiteLinkHelper.getProfileLink(player2, player2)));

        int totalGames = matchupResult.getTotalGamesPlayed();
        builder.addField("Games played: ", Integer.toString(totalGames), false);
        builder.addField(
                String.format("Wins vs %s", player2),
                Integer.toString(matchupResult.getPlayer1Wins()),
                false);
        builder.addField(
                String.format("Losses vs %s", player2),
                Integer.toString(matchupResult.getPlayer2Wins()),
                false);
        float winRate = ((float) matchupResult.getPlayer1Wins() / totalGames) * 100;
        builder.addField(
                String.format("Win rate vs %s", player2), String.format("%.2f%%", winRate), false);
        builder.addField("\u200B", "\u200B", false);
        builder.addField("Recent Matchups", "", false);
        int matchupNum = 1;

        for (VersusMatchupModel matchup : matchupResult.getRecentMatchups()) {
            java.sql.Timestamp startTime = matchup.getStartTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
            String formattedTime = sdf.format(startTime);

            String field_value = String.format("Winner: %s", matchup.getWinner());
            field_value += String.format("\nStart time: %s", formattedTime);
            if (matchup.getGameReplayUrl() != null) {
                field_value += String.format("\n[Watch Replay](%s)", matchup.getGameReplayUrl());
            }

            builder.addField(matchupNum + ". " + matchup.getGameName(), field_value, false);
            matchupNum++;
        }
        return event.reply().withEmbeds(builder.build()).retry(3);
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        ApplicationCommandRequest matchupCommand =
                ApplicationCommandRequest.builder()
                        .name(command_name)
                        .description(command_description)
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_p1)
                                        .description("First player to compare")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .required(true)
                                        .build())
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_p2)
                                        .description("Second player to compare")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .required(true)
                                        .build())
                        .build();

        return matchupCommand;
    }
}
