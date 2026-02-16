package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchserver.discord.DiscordBotService;
import com.oddlabs.matchserver.models.ChatRoomMessageModel;

import java.util.*;

public final strictfp class ChatRoom {
    private static final Map<String, ChatRoom> chat_rooms = new HashMap();
    private static final int MAX_MESSAGES = 100;
    private ChatRoomMessageModel[] messages = new ChatRoomMessageModel[MAX_MESSAGES];
    private int currentIndex = 0;
    private int messageCount = 0;
    private final Set<Client> users = new HashSet();
    private final String name;

    public ChatRoom(String name) {
        this.name = name;
        DiscordBotService.getInstance()
                .getChatroomCoordinator()
                .ifPresent(
                        x -> {
                            x.addChatroom(this);
                        });
    }

    public static final Map getChatRooms() {
        return chat_rooms;
    }

    /**
     * Joins a standard chat room for the client. This method will find the first chat room that is
     * not more than half full and join the client to it. If no such room exists, a new one will be
     * created.
     *
     * @param client
     */
    public static final void joinStandardChatRoom(Client client) {
        // <= so that if all rooms are 1/2 full it creates a new one
        for (int i = 0; i <= chat_rooms.size(); i++) {
            String room_name = "Chatroom" + i + 1;
            ChatRoom room = getChatRoom(room_name);
            // skip room if its already half full and make another
            if (room.getUsers().size() > MatchmakingServerInterface.MAX_ROOM_USERS / 2) {
                continue;
            }

            client.joinRoom(room_name);
            sendExistingMessagesToClient(client, room);
            return; // Client has joined a room
        }
    }

    /** Sends any messages already in the chat room to a client that just joined */
    private static final void sendExistingMessagesToClient(Client client, ChatRoom room) {
        // Send existing messages to the new client
        for (int j = 0; j < room.messageCount; j++) {
            int index = (room.currentIndex - room.messageCount + j + MAX_MESSAGES) % MAX_MESSAGES;
            ChatRoomMessageModel message = room.messages[index];
            if (message != null) {
                System.out.println(
                        "Sending message to client: " + message.author + ": " + message.message);
                client.getClientInterface().receiveChatRoomMessage(message.author, message.message);
            }
        }
    }

    /**
     * Gets a chat room by name, creating it if it does not exist.
     *
     * @param room_name
     * @return
     */
    public static final ChatRoom getChatRoom(String room_name) {
        ChatRoom room = (ChatRoom) chat_rooms.get(room_name);
        if (room == null) {
            room = new ChatRoom(room_name);
            chat_rooms.put(room_name, room);
        }
        return room;
    }

    public static final boolean isNameValid(String name) {
        return name != null
                && name.length() <= MatchmakingServerInterface.MAX_ROOM_NAME_LENGTH
                && name.length() >= MatchmakingServerInterface.MIN_ROOM_NAME_LENGTH
                && areCharactersValid(name);
    }

    private static final boolean areCharactersValid(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (MatchmakingServerInterface.ALLOWED_ROOM_CHARS.indexOf(name.charAt(i)) == -1) {
                return false;
            }
        }
        return true;
    }

    public final void join(Client client) {
        // TODO check for size!!!!
        users.add(client);
        sendUsers();
    }

    public final void sendUsers() {
        Iterator it = users.iterator();
        ChatRoomUser[] chat_room_users = new ChatRoomUser[users.size()];
        int i = 0;
        while (it.hasNext()) {
            Client client = (Client) it.next();
            chat_room_users[i] =
                    new ChatRoomUser(client.getProfile().getNick(), client.isPlaying());
            i++;
        }
        it = users.iterator();
        while (it.hasNext()) {
            Client client = (Client) it.next();
            client.getClientInterface().receiveChatRoomUsers(chat_room_users);
        }
    }

    /**
     * Sends a message to all connected users in the chat room.
     *
     * @param owner
     * @param msg
     */
    public final void sendMessage(String owner, String msg) {
        addMessage(new ChatRoomMessageModel(owner, msg));
        for (Client client : users) {
            MatchmakingClientInterface ci = client.getClientInterface();
            ci.receiveChatRoomMessage(owner, msg);
        }
    }

    private void addMessage(ChatRoomMessageModel message) {
        messages[currentIndex] = message;
        currentIndex = (currentIndex + 1) % MAX_MESSAGES;
        if (messageCount < MAX_MESSAGES) {
            messageCount++;
        }
    }

    public final Set getUsers() {
        return users;
    }

    public final void leave(Client client) {
        if (users.contains(client)) {
            users.remove(client);
            if (users.size() == 0) {
                DiscordBotService.getInstance()
                        .getChatroomCoordinator()
                        .ifPresent(
                                x -> {
                                    x.removeChatroom(this);
                                });
                chat_rooms.remove(getName());
            } else {
                sendUsers();
            }
        }
    }

    public final String getName() {
        return name;
    }
}
