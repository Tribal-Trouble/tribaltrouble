package com.oddlabs.tt.net;

import com.oddlabs.tt.gui.InfoPrinter;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public final class ChatCommand {
	private final static Map<String, ChatMethod> commands = new HashMap<>();

	static {
			commands.put("message", ChatCommand::sendMessage);
			commands.put("msg", ChatCommand::sendMessage);
			commands.put("tell", ChatCommand::sendMessage);
			commands.put("whisper", ChatCommand::sendMessage);
			commands.put("info", ChatCommand::getInfo);
			commands.put("finger", ChatCommand::getInfo);
			commands.put("ignore", ChatCommand::ignore);
			commands.put("unignore", ChatCommand::unignore);
			commands.put("ignorelist", ChatCommand::ignoreList);
	}

    private final static Set<String> ignored_nicks = new HashSet<>();

	private final static ResourceBundle bundle = ResourceBundle.getBundle(ChatCommand.class.getName());

	public static boolean filterCommand(@NonNull InfoPrinter info_printer, @NonNull String text) {
		return filterCommand(info_printer, Collections.emptyMap(), text);
	}

	public static boolean filterCommand(@NonNull InfoPrinter info_printer, @NonNull Map<String,ChatMethod> custom_commands, @NonNull String text) {
		if (!text.startsWith("/"))
			return false;
		int fist_space = firstSpace(text);
		String cmd = text.substring(1, fist_space);
		String args = text.substring(fist_space).trim();
		ChatMethod method = custom_commands.getOrDefault(cmd, commands.get(cmd));
		if (method != null) {
			method.execute(info_printer, args);
		} else {
			String unknown_cmd_message = Utils.getBundleString(bundle, "unknown_command", cmd);
			info_printer.print(unknown_cmd_message);
		}
		return true;
	}

	private static int firstSpace(@NonNull String text) {
		int first_space = text.indexOf(' ');
		return first_space == -1 ? text.length() : first_space;
	}

	private static void sendMessage(@NonNull InfoPrinter info_printer, @NonNull String text) {
		int first_space = firstSpace(text);
		String nick = text.substring(0, first_space);
		String message = text.substring(first_space).trim();
		if (!Network.getMatchmakingClient().isConnected())
			info_printer.print(Utils.getBundleString(bundle, "not_connected"));
		else
			Network.getMatchmakingClient().sendPrivateMessage(info_printer.getGUIRoot(), nick, message);
	}

	private static void getInfo(@NonNull InfoPrinter info_printer, @NonNull String text) {
		int first_space = firstSpace(text);
		String nick = text.substring(0, first_space);
		if (!Network.getMatchmakingClient().isConnected())
			info_printer.print(Utils.getBundleString(bundle, "not_connected"));
		else
			Network.getMatchmakingClient().requestInfo(info_printer.getGUIRoot(), nick);
	}

	public static void ignore(@NonNull InfoPrinter info_printer, @NonNull String text) {
		int first_space = firstSpace(text);
		String nick = text.substring(0, first_space);
		boolean result = ignored_nicks.add(nick.toLowerCase());
		if (result) {
			String msg = Utils.getBundleString(bundle, "ignoring", nick);
			info_printer.print(msg);
		}
	}

	public static void unignore(@NonNull InfoPrinter info_printer, @NonNull String text) {
		int first_space = firstSpace(text);
		String nick = text.substring(0, first_space);
		boolean result = ignored_nicks.remove(nick.toLowerCase());
		if (result) {
			String msg = Utils.getBundleString(bundle, "unignoring", nick);
			info_printer.print(msg);
		}
	}

	public static boolean isIgnoring(@NonNull String nick) {
		return ignored_nicks.contains(nick.toLowerCase());
	}

	private static void ignoreList(@NonNull InfoPrinter info_printer, String text) {
		String[] nicks = new String[ignored_nicks.size()];
		ignored_nicks.toArray(nicks);
		String result;
		if (nicks.length == 0) {
			result = Utils.getBundleString(bundle, "ignore_list_empty");
		} else {
			result = Utils.getBundleString(bundle, "ignore_list");
                    for (String nick : nicks) {
                        result += " " + nick;
                    }
		}
		info_printer.print(result);
	}

    private ChatCommand() {
    }
}
