package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.matchmaking.MatchmakingServerInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ChatRoom {
    private static final Map<String, ChatRoom> chat_rooms = new HashMap<>();

    private final Set<Client> users = new HashSet<>();
    private final String name;

    public ChatRoom(String name) {
        this.name = name;
    }

    public static Map<String, ChatRoom> getChatRooms() {
        return chat_rooms;
    }

    public static void joinStandardChatRoom(Client client) {
        int room_postfix = 0;
        while (true) {
            room_postfix++;
            ChatRoom room = getChatRoom("Chatroom" + room_postfix);
            if (room.getUsers().size() > MatchmakingServerInterface.MAX_ROOM_USERS / 2) {
                // skip room
                continue;
            }
            client.joinRoom(room.getName());
            return;
        }
    }

    public static ChatRoom getChatRoom(String room_name) {
        ChatRoom room = chat_rooms.get(room_name);
        if (room == null) {
            room = new ChatRoom(room_name);
            chat_rooms.put(room_name, room);
        }
        return room;
    }

    public static boolean isNameValid(String name) {
        return name != null && name.length() <= MatchmakingServerInterface.MAX_ROOM_NAME_LENGTH
                && name.length() >= MatchmakingServerInterface.MIN_ROOM_NAME_LENGTH && areCharactersValid(name);
    }

    private static boolean areCharactersValid(String name) {
        for (int i = 0; i < name.length(); i++)
            if (MatchmakingServerInterface.ALLOWED_ROOM_CHARS.indexOf(name.charAt(i)) == -1)
                return false;
        return true;
    }

    public boolean join(Client client) {
        if (users.size() >= MatchmakingServerInterface.MAX_ROOM_USERS) {
            return false;
        }
        users.add(client);
        sendUsers();
        return true;
    }

    public void sendUsers() {
        Iterator<Client> it = users.iterator();
        ChatRoomUser[] chat_room_users = new ChatRoomUser[users.size()];
        int i = 0;
        while (it.hasNext()) {
            Client client = it.next();
            chat_room_users[i] = new ChatRoomUser(client.getProfile().getNick(), client.isPlaying());
            i++;
        }
        it = users.iterator();
        while (it.hasNext()) {
            Client client = it.next();
            client.getClientInterface().receiveChatRoomUsers(chat_room_users);
        }
    }

    public void sendMessage(String msg, String owner) {
        Iterator<Client> it = users.iterator();
        while (it.hasNext()) {
            Client client = it.next();
            client.getClientInterface().receiveChatRoomMessage(msg, owner);
        }
    }

    public Set<Client> getUsers() {
        return users;
    }

    public void leave(Client client) {
        if (users.contains(client)) {
            users.remove(client);
            if (users.size() == 0) {
                chat_rooms.remove(getName());
            } else {
                sendUsers();
            }
        }
    }

    public String getName() {
        return name;
    }
}
