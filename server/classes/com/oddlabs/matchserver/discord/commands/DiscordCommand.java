package com.oddlabs.matchserver.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public abstract class DiscordCommand {

    public abstract String getCommandName();

    public abstract Mono<Void> executeCommand(ChatInputInteractionEvent event);

    public abstract ApplicationCommandRequest getCommand();
}
