package com.oddlabs.matchserver.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public abstract class DiscordCommand {

    /**
     * Discord permission bitset strings for default_member_permissions: Discord hides the command
     * from members lacking the permission and refuses invocations server-side, so this is the
     * moderation access gate. Per-role adjustments are configured in Server Settings >
     * Integrations.
     */
    protected static final String KICK_MEMBERS_PERMISSION = "2";

    protected static final String BAN_MEMBERS_PERMISSION = "4";

    public abstract String getCommandName();

    public abstract Mono<Void> executeCommand(ChatInputInteractionEvent event);

    public abstract ApplicationCommandRequest getCommand();

    protected static String getStringOption(ChatInputInteractionEvent event, String option_name) {
        return event.getOption(option_name).flatMap(ApplicationCommandInteractionOption::getValue).map(
                ApplicationCommandInteractionOptionValue::asString).orElse(null);
    }
}
