package com.oddlabs.matchserver.models;

public class ChatRoomMessageModel {
    public final String author;
    public final String message;

    public ChatRoomMessageModel(String author, String message) {
        this.author = author;
        this.message = message;
    }
}
