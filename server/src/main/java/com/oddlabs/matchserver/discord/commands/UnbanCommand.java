package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.MatchmakingServer;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public class UnbanCommand extends DiscordCommand {

    private String command_name = "unban";
    private String command_description = "Lifts the ban on the account owning a profile nick. Mod role required.";
    private String command_option_nick = "nick";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String nick = getStringOption(event, command_option_nick);
        if (nick == null)
            return event.reply("Unable to retrieve nick to unban.").withEphemeral(true);
        String moderator = event.getInteraction().getUser().getUsername();

        if (!DBInterface.setBannedByNick(nick, false))
            return event.reply("No profile named '" + nick + "' found.").withEphemeral(true);
        MatchmakingServer.getLogger().info(
                "Account owning nick " + nick + " unbanned by Discord moderator " + moderator);
        return event.reply("Unbanned the account owning '" + nick + "'.").withEphemeral(true);
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        // spotless:off
        ApplicationCommandOptionData nick_option = ApplicationCommandOptionData.builder()
                .name(command_option_nick)
                .description("The in-game profile nick whose account to unban")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .build();
        return ApplicationCommandRequest.builder()
                .name(command_name)
                .description(command_description)
                .defaultMemberPermissions(BAN_MEMBERS_PERMISSION)
                .addOption(nick_option)
                .build();
        // spotless:on
    }
}
