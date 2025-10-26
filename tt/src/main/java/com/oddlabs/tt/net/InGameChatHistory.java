package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;

public final class InGameChatHistory extends ChatHistory {
        @Override
	public void chat(@NonNull ChatMessage message) {
		if (message.type == ChatMessage.Type.PRIVATE || message.type == ChatMessage.Type.NORMAL ||  message.type == ChatMessage.Type.TEAM) {
			addMessage(message.formatLong());
		}
	}
}
