package com.oddlabs.matchserver.discord.commands;

import com.oddlabs.matchserver.DBInterface;
import com.oddlabs.matchserver.WebsiteLinkHelper;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import reactor.core.publisher.Mono;

public class OnlineCommand extends DiscordCommand {
    private String command_name = "online";
    private String command_description = "Displays tribal trouble profiles that are online now";

    public OnlineCommand() {}

    @Override
    public String getCommandName() {
        return command_name;
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String[] onlineProfiles = DBInterface.getOnlineProfiles();
        int totalOnline = onlineProfiles.length;
        EmbedCreateSpec.Builder builder =
                EmbedCreateSpec.builder().color(Color.BLUE).title(totalOnline + " users in game");

        // Add fields with up to 10 profile names per field
        for (int i = 0; i < totalOnline; i += 10) {
            int end = Math.min(i + 10, totalOnline);
            StringBuilder fieldValue = new StringBuilder();
            for (int j = i; j < end; j++) {
                String linkedName =
                        WebsiteLinkHelper.getProfileLink(onlineProfiles[j], onlineProfiles[j]);
                fieldValue.append(linkedName);
                if (j < end - 1) fieldValue.append(", ");
            }
            builder.addField("", fieldValue.toString(), false);
        }

        return event.reply().withEmbeds(builder.build());
    }

    /**
     * Gets the command as a built ApplicationCommandRequest used for creating Discord commands in
     * discord Generally only needs to be called once ever for the bot unless recreating a command.
     */
    @Override
    public ApplicationCommandRequest getCommand() {
        ApplicationCommandRequest onlineCommand =
                ApplicationCommandRequest.builder()
                        .name(command_name)
                        .description(command_description)
                        .build();

        return onlineCommand;
    }
}
