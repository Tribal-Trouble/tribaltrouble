package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.DBInterface;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import reactor.core.publisher.Mono;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterProfileToDiscordUserCommand extends DiscordCommand {
    private boolean debug = false;
    private String command_name = "register-user";
    private String command_description =
            "Registers a tribal trouble user profile to the Discord user. TT user must reply in"
                    + " game.";
    private String command_option_profile_name = "profile_name";

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        return event.deferReply().withEphemeral(true).then(doRegistrationInteraction(event));
    }

    /** Static dicitonary of profiles awaiting to be processed by a user response. */
    private class ProfileRegistrationTimeout {
        long discord_user_id;
        String profile_name;
        Timer timer;

        public ProfileRegistrationTimeout(
                long discord_user_id, String profile_name, ChatInputInteractionEvent event) {
            this.discord_user_id = discord_user_id;
            this.profile_name = profile_name;
            this.timer = new Timer(true);
            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            printDebug(
                                    "Timeout: Removing profile "
                                            + profile_name
                                            + " from processingProfiles");
                            RegisterProfileToDiscordUserCommand.processingProfiles.remove(
                                    profile_name);
                            event.createFollowup(
                                            "No response from tribal trouble user. Profile"
                                                    + " registration timed out.")
                                    .then()
                                    .subscribe();
                            printDebug(
                                    "Timeout: Removed "
                                            + profile_name
                                            + " from processingProfiles");
                        }
                    },
                    60000);
        }

        public String getNick() {
            return profile_name;
        }

        public long getDiscordUserId() {
            return discord_user_id;
        }

        public void stopTimer() {
            timer.cancel();
        }
    }

    public static ConcurrentHashMap<String, Runnable> processingProfiles =
            new ConcurrentHashMap<>();

    public Mono<Void> doRegistrationInteraction(ChatInputInteractionEvent event) {
        String profileToRegisterName =
                event.getOption(command_option_profile_name)
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse(null);
        if (profileToRegisterName == null)
            return event.createFollowup("Unable to retrieve profile name to register.")
                    .withEphemeral(true)
                    .then();
        profileToRegisterName = profileToRegisterName.toLowerCase();
        long discord_user_id = event.getInteraction().getUser().getId().asLong();
        if (DBInterface.isProfileRegisteredToDiscord(profileToRegisterName)) {
            return event.createFollowup("Profile is already registered to a Discord user.")
                    .withEphemeral(true)
                    .then();
        }
        com.oddlabs.matchserver.Client client =
                (com.oddlabs.matchserver.Client)
                        com.oddlabs.matchserver.Client.getActiveClients()
                                .get(profileToRegisterName.toLowerCase());
        if (client != null) {
            // Found the client with the matching profile name
            // You can now send a private message or perform other actions
            printDebug("Found active client with profile name: " + profileToRegisterName);
            final ProfileRegistrationTimeout registration =
                    new ProfileRegistrationTimeout(discord_user_id, profileToRegisterName, event);
            processingProfiles.put(
                    profileToRegisterName,
                    () -> {
                        registration.stopTimer();
                        DBInterface.registerProfileToDiscordUser(
                                registration.getNick(), registration.getDiscordUserId());
                        event.createFollowup(
                                        "Successfully registered profile: "
                                                + registration.getNick())
                                .withEphemeral(true)
                                .then()
                                .subscribe();
                        processingProfiles.remove(registration.getNick());
                    });
            client.sendPrivateMessage(
                    profileToRegisterName,
                    "A discord user is requesting to register this profile. Allow this? Reply with"
                            + " y/n");
            return event.createFollowup(
                            "Sent registration request to profile: "
                                    + profileToRegisterName
                                    + ". Please respond in-game.")
                    .withEphemeral(true)
                    .then();
        } else {
            // Handle not found
            printDebug("Could not find active client with profile name: " + profileToRegisterName);
            return event.createFollowup("Failed to register profile: " + profileToRegisterName)
                    .withEphemeral(true)
                    .then();
        }
    }

    @Override
    public ApplicationCommandRequest getCommand() {
        ApplicationCommandRequest registerUserCommand =
                ApplicationCommandRequest.builder()
                        .name(command_name)
                        .description(command_description)
                        .addOption(
                                ApplicationCommandOptionData.builder()
                                        .name(command_option_profile_name)
                                        .description(
                                                "The in-game profile name to register to your"
                                                        + " Discord user")
                                        .type(
                                                ApplicationCommandOption.Type.STRING
                                                        .getValue()) // 3 is
                                        // STRING
                                        // type
                                        .required(true)
                                        .build())
                        .build();

        return registerUserCommand;
    }

    private void printDebug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
