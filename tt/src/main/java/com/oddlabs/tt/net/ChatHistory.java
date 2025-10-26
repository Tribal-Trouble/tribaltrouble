package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ChatHistory implements ChatListener {
	private final static int MAX_HISTORY = 50;

	private final List<String> messages = new ArrayList<>(MAX_HISTORY);

	public final void clear() {
		messages.clear();
	}

    @Override
	public abstract void chat(ChatMessage message);

	protected final void addMessage(String msg) {
        while (messages.size() >= MAX_HISTORY) {
            messages.remove(0);
        }
		messages.add(msg);
	}

	final @NonNull List<String> getMessages() {
		return messages;
	}
}
