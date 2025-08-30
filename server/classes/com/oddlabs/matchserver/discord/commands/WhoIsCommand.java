package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.DBInterface;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public class WhoIsCommand extends DiscordCommand {

    private boolean debug = false;
    private String command_name = "whois";
    private String command_description =
            "Displays tribal trouble profiles associated with the discord user and vice versa.";
    private String command_option_lookup_name = "user";

    public WhoIsCommand() {}

    @Override
    public String getCommandName() {
        return command_name;
    }

    // TODO: Make this less 'pingy' with the mentions - maybe making ephemeral
    // replies would be the
    // easiest way
    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String user_name =
                event.getOption(command_option_lookup_name)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse("");

        printDebug("Looking up user: " + user_name);
        // If a discord user was mentioned, extract their ID by removing non-numeric
        // characters
        // (string will be <@long>)
        String idStr = user_name.replaceAll("[^0-9]", "");
        long discordUserId = -1;
        try {
            discordUserId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
        }

        if (discordUserId != -1) {
            String[] registeredProfiles =
                    DBInterface.getProfilesRegisteredToDiscordUser(discordUserId);
            if (registeredProfiles.length == 0) {
                return event.reply("No profiles registered for user: " + discordUserId);
            }
            return event.reply(
                    "Registered profiles for user "
                            + toDiscordMention(discordUserId)
                            + ": "
                            + String.join(", ", registeredProfiles));
        } else {
            long discord_id_for_user = DBInterface.getDiscordUserIdForProfile(user_name);
            if (discord_id_for_user != -1) {
                return event.reply(
                        "Discord ID for tribal trouble nick '"
                                + user_name
                                + "': "
                                + toDiscordMention(discord_id_for_user));
            } else {
                return event.reply(
                        "No Discord ID found for tribal trouble nick: '" + user_name + "'");
            }
        }
    }

    /**
     * Gets the command as a built ApplicationCommandRequest used for creating Discord commands in
     * discord Generally only needs to be called once ever for the bot unless recreating a command.
     */
    @Override
    public ApplicationCommandRequest getCommand() {
        ApplicationCommandRequest whoIsCommand =
                ApplicationCommandRequest.builder()
                        .name(command_name)
                        .description(command_description)
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_lookup_name)
                                        .description(
                                                "The user to look up (@discord_user or"
                                                        + " tt_nickname)")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .required(true)
                                        .build())
                        .build();

        return whoIsCommand;
    }

    private String toDiscordMention(long discordId) {
        return "<@" + discordId + ">";
    }

    private Mono<String> toDiscordUserName(long discordId, ChatInputInteractionEvent event) {
        return event.getInteraction()
                .getGuild()
                .flatMap(guild -> guild.getMemberById(Snowflake.of(discordId)))
                .map(member -> member.getNickname().orElse(member.getUsername()));
    }

    private void printDebug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
