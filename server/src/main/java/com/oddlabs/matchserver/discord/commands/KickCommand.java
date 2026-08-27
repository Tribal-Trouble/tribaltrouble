package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.Client;
import com.oddlabs.matchserver.MatchmakingServer;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public class KickCommand extends DiscordCommand {

    private String command_name = "kick";
    private String command_description = "Disconnects an online player from the multiplayer server. Mod role required.";
    private String command_option_nick = "nick";
    private String command_option_reason = "reason";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String nick = getStringOption(event, command_option_nick);
        if (nick == null)
            return event.reply("Unable to retrieve nick to kick.").withEphemeral(true);
        String reason = getStringOption(event, command_option_reason);
        String moderator = event.getInteraction().getUser().getUsername();

        if (!kickOnlineClient(nick, reason, moderator, "kicked"))
            return event.reply("No player named '" + nick + "' is online.").withEphemeral(true);
        return event.reply("Kicked '" + nick + "'.").withEphemeral(true);
    }

    /**
     * Disconnects the client with the given profile nick if online. Returns false when no such
     * client is connected.
     */
    static boolean kickOnlineClient(String nick, String reason, String moderator, String action) {
        Client client = Client.getActiveClients().get(nick.toLowerCase());
        if (client == null)
            return false;
        String message = "You have been " + action + " by a moderator" + (reason != null ? ": " + reason : ".");
        client.getClientInterface().receivePrivateMessage("Server", message);
        MatchmakingServer.getLogger().info(
                nick + " " + action + " by Discord moderator " + moderator + (reason != null ? " (" + reason + ")" : ""));
        client.close();
        return true;
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        // spotless:off
        ApplicationCommandOptionData nick_option = ApplicationCommandOptionData.builder()
                .name(command_option_nick)
                .description("The in-game profile nick to kick")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .build();
        ApplicationCommandOptionData reason_option = ApplicationCommandOptionData.builder()
                .name(command_option_reason)
                .description("Reason shown to the player and logged")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(false)
                .build();
        return ApplicationCommandRequest.builder()
                .name(command_name)
                .description(command_description)
                .defaultMemberPermissions(KICK_MEMBERS_PERMISSION)
                .addOption(nick_option)
                .addOption(reason_option)
                .build();
        // spotless:on
    }
}
