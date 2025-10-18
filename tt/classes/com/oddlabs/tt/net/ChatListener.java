package com.oddlabs.tt.net;

@FunctionalInterface
public interface ChatListener {
	void chat(ChatMessage message);
}
