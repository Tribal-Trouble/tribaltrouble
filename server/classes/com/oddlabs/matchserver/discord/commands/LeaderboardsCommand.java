package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchmaking.RankingEntry;
import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.WebsiteLinkHelper;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardsCommand extends DiscordCommand {

    private boolean debug = false;
    private String command_name = "leaderboard";
    private String command_description = "Displays the current leaderboard";
    private String command_option_start = "start";
    private String command_option_count = "count";

    public LeaderboardsCommand() {}

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {

        long start =
                event.getOption(command_option_start)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asLong)
                        .orElse(0L);
        long count =
                event.getOption(command_option_count)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asLong)
                        .orElse(25L);

        if (start > Integer.MAX_VALUE || count > Integer.MAX_VALUE) {
            return event.reply("Invalid start or count value.");
        }
        int startInt = (int) start;
        int countInt = (int) count;
        if (count > 200) {
            return event.reply("You can only request up to 200 ranks at a time.");
        }

        if (startInt < 0) {
            startInt = 0;
        }

        if (countInt <= 0) {
            countInt = 25;
        }
        printDebug("Fetching leaderboards from " + startInt + " count " + countInt);
        RankingEntry[] ranks = DBInterface.getRankings(startInt, countInt);
        printDebug("Fetched " + ranks.length + " ranks from the database.");
        List<EmbedCreateSpec> embeds = new ArrayList<>();
        try {
            for (int i = 0; i < ranks.length; i += 25) {
                printDebug(
                        "Creating embed for leaderboard "
                                + (i + startInt)
                                + "-"
                                + Math.min(i + 25, ranks.length));

                EmbedCreateSpec.Builder builder =
                        EmbedCreateSpec.builder()
                                .color(Color.BLUE)
                                .title(
                                        "Leaderboards "
                                                + (i + startInt + 1)
                                                + "-"
                                                + Math.min(
                                                        i + startInt + 25,
                                                        i + startInt + ranks.length));
                for (int j = i; j < Math.min(i + 25, ranks.length); j++) {
                    String name = String.format("%s", ranks[j].getName());
                    String title = String.format("%d. %s", j + startInt + 1, name);
                    String message =
                            String.format(
                                    "Rating: %s, Wins: %s, Losses: %s, %s",
                                    ranks[j].getRating(),
                                    ranks[j].getWins(),
                                    ranks[j].getLosses(),
                                    WebsiteLinkHelper.getProfileLink(
                                            "Profile", ranks[j].getName()));
                    builder.addField(title, message, false);
                    printDebug("Added rank " + title + " to embed.");
                }
                embeds.add(builder.build());
            }
        } catch (Exception e) {
            printDebug("Exception occurred while creating embeds: " + e.getMessage());
        }
        if (embeds.size() == 0) {
            return event.reply("No rankings available to display.");
        }

        return event.reply().withEmbeds(embeds).retry(3);
    }

    /**
     * Gets the command as a built ApplicationCommandRequest used for creating Discord commands in
     * discord Generally only needs to be called once ever for the bot unless recreating a command.
     */
    @Override
    public ApplicationCommandRequest getCommand() {
        ApplicationCommandRequest leaderboardCommand =
                ApplicationCommandRequest.builder()
                        .name(command_name)
                        .description(command_description)
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_start)
                                        .description("Rank to start showing the leaderboards from")
                                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                        .required(false)
                                        .build())
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_count)
                                        .description("Number of ranks to show")
                                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                        .required(false)
                                        .build())
                        .build();

        return leaderboardCommand;
    }

    private void printDebug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
