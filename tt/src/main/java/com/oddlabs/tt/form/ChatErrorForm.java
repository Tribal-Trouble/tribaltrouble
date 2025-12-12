package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class ChatErrorForm extends MessageForm {
	private static @NonNull String getErrorFromCode(int error_code) {
		ResourceBundle bundle = ResourceBundle.getBundle(ChatErrorForm.class.getName());
        return switch (error_code) {
            case MatchmakingClientInterface.CHAT_ERROR_TOO_MANY_USERS ->
                    Utils.getBundleString(bundle, "chat_error_too_many_users");
            case MatchmakingClientInterface.CHAT_ERROR_INVALID_NAME ->
                    Utils.getBundleString(bundle, "chat_error_invalid_name");
            case MatchmakingClientInterface.CHAT_ERROR_NO_SUCH_NICK ->
                    Utils.getBundleString(bundle, "chat_error_no_such_nick");
            default -> throw new RuntimeException("Unknown error code: " + error_code);
        };
	}
	
	public ChatErrorForm(int error_code) {
		super(getErrorFromCode(error_code));
	}
}
