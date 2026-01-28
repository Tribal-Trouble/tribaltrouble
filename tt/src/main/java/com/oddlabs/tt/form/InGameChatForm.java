package com.oddlabs.tt.form;

import com.oddlabs.tt.delegate.ControllableCameraDelegate;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.InfoPrinter;
import com.oddlabs.tt.gui.RadioButton;
import com.oddlabs.tt.gui.RadioButtonGroup;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.TextBox;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.net.ChatCommand;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.ChatMethod;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;

public final class InGameChatForm extends Form implements ChatListener {
	private static final int CHAT_WIDTH = 400;
	private static final int BUTTON_WIDTH = 50;
	private static final int CHAT_HEIGHT = 150;
	private static final ResourceBundle bundle = ResourceBundle.getBundle(InGameChatForm.class.getName());

	private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	private final @NonNull EditLine chat_line;
	private final InfoPrinter info_printer;
	private final @NonNull TextBox chat_box;
	private final @NonNull RadioButtonGroup radio_button_group;
	private final @NonNull RadioButton radio_all;
	private final @NonNull RadioButton radio_team;
	private final WorldViewer viewer;

	public InGameChatForm(InfoPrinter info_printer, WorldViewer viewer) {
		super(i18n( "chat"));
		this.viewer = viewer;

		this.info_printer = info_printer;
		chat_line = new EditLine(CHAT_WIDTH, 256);
		addChild(chat_line);
		chat_line.addEnterListener(new ChatListener());

		HorizButton button_send = new HorizButton(i18n("send"), BUTTON_WIDTH);
		addChild(button_send);
		button_send.addMouseClickListener(( _,  _,  _,  _) -> chat_line.enterPressedAll());

		chat_box = new TextBox(CHAT_WIDTH + BUTTON_WIDTH, CHAT_HEIGHT, Skin.getSkin().getEditFont(), Integer.MAX_VALUE);
		addChild(chat_box);

		radio_button_group = new RadioButtonGroup();

		radio_all = new RadioButton(true, radio_button_group, i18n("send_to_all"));
		addChild(radio_all);

		radio_team = new RadioButton(false, radio_button_group, i18n("send_to_team"));
		addChild(radio_team);

		chat_line.place();
		button_send.place(chat_line, RIGHT_MID);
		chat_box.place(chat_line, TOP_LEFT);
		radio_all.place(chat_line, BOTTOM_LEFT);
		radio_team.place(chat_line, BOTTOM_RIGHT);
		compileCanvas();
		Network.getMatchmakingClient().clearInGameChatHistory();
	}

	@Override
	protected void doAdd() {
		super.doAdd();
		Network.getChatHub().addListener(this);
		refreshMessages();
	}

	@Override
	protected void doRemove() {
		super.doRemove();
		Network.getChatHub().removeListener(this);
	}

	public void setReceivers(boolean all) {
		radio_button_group.mark(all ? radio_all : radio_team);
	}

	@Override
	public void setFocus() {
		chat_line.setFocus();
	}

	@Override
	public void chat(ChatMessage message) {
		refreshMessages();
	}

	private void refreshMessages() {
		List<String> messages = Network.getMatchmakingClient().getInGameChatHistory();
		chat_box.clear();
		for (int i = 0; i < messages.size(); i++) {
			if (i != 0)
				chat_box.append("\n");
			chat_box.append(messages.get(i));
		}
		chat_box.setOffsetY(Integer.MAX_VALUE);
	}

	@Override
	public void mouseMoved(int x, int y) {
		((ControllableCameraDelegate)getParent()).mouseMoved(x, y);
	}

	private final class ChatListener implements EnterListener {
		@Override
		public void enterPressed(@NonNull CharSequence text) {
			String chat = text.toString();
			if (!chat.isEmpty()) {
				chat_line.clear();
				Map<String,ChatMethod> commands =
						Map.of("iamacheater",  (_,_) -> viewer.getCheat().enable());
				if (!ChatCommand.filterCommand(info_printer, commands, chat)) {
					viewer.getPeerHub().sendChat(chat, radio_button_group.getMarked() == radio_team);
				}
			} else {
				cancel();
			}
		}
	}
}
