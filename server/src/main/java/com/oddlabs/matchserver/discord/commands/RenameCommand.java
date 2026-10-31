package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.matchserver.Authenticator;
import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.InvalidUsernameException;
import com.oddlabs.matchserver.MatchmakingServer;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

public class RenameCommand extends DiscordCommand {

    private static final String UNLOCK_NOTE = "To unlock this profile you will need to submit a support request on discord.";

    private String command_name = "rename";
    private String command_description = "Renames a profile, locks it so it cannot be deleted and disconnects the player. Mod role required.";
    private String command_option_nick = "nick";
    private String command_option_new_nick = "new_nick";
    private String command_option_reason = "reason";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String nick = getStringOption(event, command_option_nick);
        String new_nick = getStringOption(event, command_option_new_nick);
        if (nick == null || new_nick == null)
            return event.reply("Unable to retrieve the nicks to rename.").withEphemeral(true);
        String reason = getStringOption(event, command_option_reason);
        String moderator = event.getInteraction().getUser().getUsername();

        try {
            Authenticator.checkUsername(new_nick);
        } catch (InvalidUsernameException e) {
            String problem = switch (e.getErrorCode()) {
                case MatchmakingClientInterface.USERNAME_ERROR_TOO_SHORT -> "is too short";
                case MatchmakingClientInterface.USERNAME_ERROR_TOO_LONG -> "is too long";
                default -> "has characters that are not allowed or a banned word";
            };
            return event.reply("'" + new_nick + "' " + problem + ".").withEphemeral(true);
        }
        if (new_nick.toLowerCase().startsWith("guest"))
            return event.reply("Profile names cannot start with 'Guest'.").withEphemeral(true);
        if (!new_nick.equalsIgnoreCase(nick) && DBInterface.nickExists(new_nick))
            return event.reply("A profile named '" + new_nick + "' already exists.").withEphemeral(true);

        if (!DBInterface.renameAndLockProfile(nick, new_nick))
            return event.reply("No profile named '" + nick + "' found.").withEphemeral(true);
        MatchmakingServer.getLogger().info(
                "Profile " + nick + " renamed to " + new_nick + " and name locked by Discord moderator " + moderator + (reason != null ? " (" + reason + ")" : ""));
        boolean was_online = KickCommand.kickOnlineClient(nick, reason, moderator, "renamed to " + new_nick,
                UNLOCK_NOTE);
        return event.reply(
                "Renamed '" + nick + "' to '" + new_nick + "' and locked the profile" + (was_online ? ". Disconnected the player." : ". Player was not online.")).withEphemeral(
                        true);
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        // spotless:off
        ApplicationCommandOptionData nick_option = ApplicationCommandOptionData.builder()
                .name(command_option_nick)
                .description("The in-game profile nick to rename")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .build();
        ApplicationCommandOptionData new_nick_option = ApplicationCommandOptionData.builder()
                .name(command_option_new_nick)
                .description("The name the profile gets")
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
                .addOption(new_nick_option)
                .addOption(reason_option)
                .build();
        // spotless:on
    }
}
