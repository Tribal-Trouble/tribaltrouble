package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchmaking.RankingEntry;
import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.ServerConfiguration;
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

public class RankCommand extends DiscordCommand {
    private String command_name = "rank";
    private String command_description =
            "Shows the rank of a user and users around them on the leaderboard";
    private String command_option_lookup_name = "tt_user";
    private String command_option_range = "range";

    public RankCommand() {}

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String nick =
                event.getOption(command_option_lookup_name)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse(null);

        long range =
                event.getOption(command_option_range)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asLong)
                        .orElse(2L);
        if (range > 10) {
            range = 10;
        }
        if (nick == null) {
            return event.reply("Please provide a valid user profile name.").then();
        }

        RankingEntry[] rankingEntry = DBInterface.getRankings(nick, (int) range);
        if (rankingEntry.length == 0) {
            return event.reply("Could not find ranking information for user: " + nick).then();
        }

        // Reply with the ranking information

        EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder()
                        .color(Color.BLUE)
                        .title(String.format("%s ranking", nick));

        for (RankingEntry entry : rankingEntry) {
            String profileLink = WebsiteLinkHelper.getProfileLink("Profile", entry.getName());
            builder.addField(
                    String.format(
                            "%d. %s %s",
                            entry.getRanking(),
                            entry.getName(),
                            entry.getName().equals(nick)
                                    ? ServerConfiguration.getInstance()
                                            .get(ServerConfiguration.VIKING_CHIEF_EMOJI)
                                    : ""),
                    String.format(
                            "Rating: %d, Wins: %d, Losses: %d, Invalid: %d, %s",
                            entry.getRating(),
                            entry.getWins(),
                            entry.getLosses(),
                            entry.getInvalid(),
                            profileLink),
                    false);
        }
        return event.reply().withEmbeds(builder.build()).then();
    }

    /**
     * Gets the command as a built ApplicationCommandRequest used for creating Discord commands in
     * discord Generally only needs to be called once ever for the bot unless recreating a command.
     */
    @Override
    public ApplicationCommandRequest getCommand() {
        ApplicationCommandRequest rankCommand =
                ApplicationCommandRequest.builder()
                        .name(command_name)
                        .description(command_description)
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_lookup_name)
                                        .description("The tribal trouble profile name to lookup")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .required(true)
                                        .build())
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_range)
                                        .description(
                                                "The range of users to display around the looked up"
                                                        + " user")
                                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                        .required(false)
                                        .build())
                        .build();

        return rankCommand;
    }
}
