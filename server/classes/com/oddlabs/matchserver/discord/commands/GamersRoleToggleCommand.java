package com.oddlabs.matchserver.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public class GamersRoleToggleCommand extends DiscordCommand {
    private final String command_name = "iamagamer";
    private final String command_description = "Toggle the Gamers role for yourself";
    private static final String GAMERS_ROLE_NAME = "Gamers";

    public GamersRoleToggleCommand() {}

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        // Get the member who executed the command
        return event.getInteraction()
                .getMember()
                .map(
                        member -> {
                            // Find the "Gamers" role in the guild
                            return member.getGuild()
                                    .flatMapMany(guild -> guild.getRoles())
                                    .filter(
                                            role ->
                                                    role.getName()
                                                            .equalsIgnoreCase(GAMERS_ROLE_NAME))
                                    .next() // Get the first (and should be only) matching role
                                    .flatMap(
                                            gamersRole -> {
                                                // Check if the member already has the role
                                                return member.getRoleIds()
                                                                .contains(gamersRole.getId())
                                                        ? member.removeRole(gamersRole.getId())
                                                                .then(
                                                                        event.reply(
                                                                                "You have been"
                                                                                    + " removed"
                                                                                    + " from the "
                                                                                        + GAMERS_ROLE_NAME
                                                                                        + " role!"))
                                                        : member.addRole(gamersRole.getId())
                                                                .then(
                                                                        event.reply(
                                                                                "You have been"
                                                                                    + " added to"
                                                                                    + " the "
                                                                                        + GAMERS_ROLE_NAME
                                                                                        + " role!"));
                                            })
                                    .switchIfEmpty(
                                            // Role not found
                                            event.reply("Gamers role not found"));
                        })
                .orElse(event.reply("This command can only be used in a server."))
                .onErrorResume(
                        error -> {
                            System.out.println(
                                    "Error in GamersRoleToggleCommand: " + error.getMessage());
                            return event.reply("An error occurred while toggling the Gamers role.");
                        })
                .then();
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        return ApplicationCommandRequest.builder()
                .name(command_name)
                .description(command_description)
                .build();
    }
}
