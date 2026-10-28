package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.MatchmakingServer;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public class NameLockCommand extends DiscordCommand {

    private String command_name = "namelock";
    private String command_description = "Locks a profile so the player cannot delete it, or unlocks it. Mod role required.";
    private String command_option_nick = "nick";
    private String command_option_locked = "locked";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String nick = getStringOption(event, command_option_nick);
        if (nick == null)
            return event.reply("Unable to retrieve nick to lock.").withEphemeral(true);
        boolean locked = event.getOption(command_option_locked).flatMap(
                ApplicationCommandInteractionOption::getValue).map(
                        ApplicationCommandInteractionOptionValue::asBoolean).orElse(true);
        String moderator = event.getInteraction().getUser().getUsername();

        if (!DBInterface.setNameLocked(nick, locked))
            return event.reply("No profile named '" + nick + "' found.").withEphemeral(true);
        String action = locked ? "locked" : "unlocked";
        MatchmakingServer.getLogger().info(
                "Profile " + nick + " name " + action + " by Discord moderator " + moderator);
        return event.reply(
                "Profile '" + nick + "' is now " + action + ".").withEphemeral(true);
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        // spotless:off
        ApplicationCommandOptionData nick_option = ApplicationCommandOptionData.builder()
                .name(command_option_nick)
                .description("The in-game profile nick to lock or unlock")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .build();
        ApplicationCommandOptionData locked_option = ApplicationCommandOptionData.builder()
                .name(command_option_locked)
                .description("True stops the player deleting the profile, False allows it again")
                .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                .required(true)
                .build();
        return ApplicationCommandRequest.builder()
                .name(command_name)
                .description(command_description)
                .defaultMemberPermissions(BAN_MEMBERS_PERMISSION)
                .addOption(nick_option)
                .addOption(locked_option)
                .build();
        // spotless:on
    }
}
