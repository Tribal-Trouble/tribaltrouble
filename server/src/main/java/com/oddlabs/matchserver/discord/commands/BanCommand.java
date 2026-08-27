package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.MatchmakingServer;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public class BanCommand extends DiscordCommand {

    private String command_name = "ban";
    private String command_description = "Bans the account owning a profile nick and disconnects the player. Mod role required.";
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
            return event.reply("Unable to retrieve nick to ban.").withEphemeral(true);
        String reason = getStringOption(event, command_option_reason);
        String moderator = event.getInteraction().getUser().getUsername();

        if (!DBInterface.setBannedByNick(nick, true))
            return event.reply(
                    "No profile named '" + nick + "' found. Guest players have no account to ban; use /kick instead.").withEphemeral(
                            true);
        MatchmakingServer.getLogger().info(
                "Account owning nick " + nick + " banned by Discord moderator " + moderator + (reason != null ? " (" + reason + ")" : ""));
        boolean was_online = KickCommand.kickOnlineClient(nick, reason, moderator, "banned");
        return event.reply(
                "Banned the account owning '" + nick + "'" + (was_online ? " and disconnected the player." : ". Player was not online.")).withEphemeral(
                        true);
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        // spotless:off
        ApplicationCommandOptionData nick_option = ApplicationCommandOptionData.builder()
                .name(command_option_nick)
                .description("The in-game profile nick whose account to ban")
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
                .defaultMemberPermissions(BAN_MEMBERS_PERMISSION)
                .addOption(nick_option)
                .addOption(reason_option)
                .build();
        // spotless:on
    }
}
