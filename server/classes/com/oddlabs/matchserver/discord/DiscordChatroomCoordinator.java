package com.oddlabs.matchserver.discord;

import com.oddlabs.matchserver.ChatRoom;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;

import reactor.core.Disposable;

import java.util.HashMap;

public class DiscordChatroomCoordinator {
    private boolean debug = false;
    // TT Chatroom instance -> associated discord text channel
    private HashMap<ChatRoom, TextChannel> chatRoomChannels = new HashMap<ChatRoom, TextChannel>();
    // TT Chatroom instance -> to its subscription to a message sending
    private HashMap<ChatRoom, Disposable> discordMessageSubscriptions =
            new HashMap<ChatRoom, Disposable>();

    /**
     * Adds a chatroom and its associated discord channel to the coordinator.
     *
     * @param chatroom
     */
    public void addChatroom(ChatRoom chatroom) {
        TextChannel discordChannel = getDiscordChannelForRoom(chatroom.getName());
        if (discordChannel == null) {
            LogDebug("Discord channel for " + chatroom.getName() + " not found!");
        } else {
            chatRoomChannels.put(chatroom, discordChannel);
            subscribeToDiscordMessages(chatroom);
            LogDebug(
                    "Discord channel for "
                            + chatroom.getName()
                            + " found: "
                            + discordChannel.getName()
                            + ". Registered to coordinator.");
        }
    }

    /**
     * Safely removes a chatroom from the coordinator by cleaning up any subscriptions
     *
     * @param chatroom
     */
    public void removeChatroom(ChatRoom chatroom) {
        discordMessageSubscriptions.get(chatroom).dispose();
        discordMessageSubscriptions.remove(chatroom);
        chatRoomChannels.remove(chatroom);
    }

    /**
     * Sends a discord message to the chatroom if it can
     *
     * @param chatRoom
     * @param owner
     * @param msg
     */
    public void sendDiscordMessage(ChatRoom chatRoom, String owner, String msg) {
        try {
            TextChannel discordChannel = chatRoomChannels.get(chatRoom);
            if (discordChannel == null) {
                LogDebug("Discord channel for chat room " + chatRoom.getName() + " not found!");
                return;
            }

            LogDebug("sending message to discord");
            if (!msg.startsWith("<")) msg = formatChat(owner, msg);

            discordChannel
                    .createMessage(msg)
                    .retry(3)
                    .subscribe(
                            _ -> {},
                            error ->
                                    LogDebug(
                                            "Failed to send Discord message: "
                                                    + error.getMessage()));

        } catch (Exception e) {
            LogDebug("Error sending discord message: " + e.getMessage());
        }
    }

    /**
     * Sends a discord embed to the specified chatroom instance if it can
     *
     * @param discordChannel
     * @param embed
     * @param roomName
     */
    public void sendDiscordEmbed(ChatRoom chatroom, EmbedCreateSpec embed) {
        TextChannel discordChannel = this.chatRoomChannels.get(chatroom);
        if (discordChannel == null) {
            LogDebug("Discord channel for chat room " + chatroom.getName() + " not found!");
            return;
        }
        sendDiscordEmbed(discordChannel, embed);
    }

    /**
     * Sends a discord embed to a specific discord channel
     *
     * @param discordChannel
     * @param embed
     */
    public void sendDiscordEmbed(TextChannel discordChannel, EmbedCreateSpec embed) {
        try {
            if (discordChannel != null) {
                discordChannel
                        .createMessage(embed)
                        .retry(3)
                        .subscribe(
                                _ -> {},
                                error ->
                                        LogDebug(
                                                "Failed to send Discord embed: "
                                                        + error.getMessage()));
            }
        } catch (Exception e) {
            LogDebug("Error sending discord embed: " + e.getMessage());
        }
    }

    /**
     * Gets the Discord channel associated with a chat room based on a naming convention.
     *
     * @param roomName
     * @return
     */
    private TextChannel getDiscordChannelForRoom(String roomName) {
        try {
            int roomNumber = Integer.parseInt(roomName.substring("Chatroom".length()));
            return DiscordBotService.getInstance().getDiscordChannelByTTRoomNumber(roomNumber);
        } catch (Exception e) {
            System.err.println(
                    "Error getting Discord channel for room " + roomName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Event handler for a discord message sent into a connected chatroom
     *
     * @param event
     * @param chatRoom
     */
    private void handleIncomingDiscordMessage(MessageCreateEvent event, ChatRoom chatRoom) {
        LogDebug("Handling Discord message for chatroom " + chatRoom.getName());
        String content = event.getMessage().getContent();
        event.getMember()
                .ifPresent(
                        member -> {
                            String displayName = member.getDisplayName();
                            String author = "@" + displayName;
                            LogDebug("Processing Discord message from " + author + ": " + content);
                            chatRoom.sendMessage(author, content);
                        });
    }

    /**
     * Subscribes to receiving Discord messages for a chat room.
     *
     * @param chatRoom
     */
    private void subscribeToDiscordMessages(ChatRoom chatRoom) {
        TextChannel discordChannel = chatRoomChannels.get(chatRoom);

        Disposable messageSubscription =
                discordChannel
                        .getClient()
                        .on(MessageCreateEvent.class)
                        .filter(
                                event ->
                                        event.getMessage()
                                                .getChannelId()
                                                .equals(discordChannel.getId()))
                        .filter(
                                event ->
                                        !event.getMessage()
                                                .getAuthor()
                                                .map(
                                                        user ->
                                                                user.getId()
                                                                        .equals(
                                                                                DiscordBotService
                                                                                        .getInstance()
                                                                                        .getBotId()))
                                                .orElse(false))
                        .subscribe(event -> handleIncomingDiscordMessage(event, chatRoom));
        discordMessageSubscriptions.put(chatRoom, messageSubscription);
    }

    private String formatChat(String owner, String message) {
        return "<" + owner + "> " + message;
    }

    private void LogDebug(String message) {
        if (debug) {
            System.out.println("DiscordChatroomCoordinator: " + message);
        }
    }
}
