package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class PrivateMessageForm extends Form {
	private final static int BUTTON_WIDTH = 100;
	private final static int EDITLINE_WIDTH = 240;

	private final @NonNull EditLine editline_name;
	private final String nick;
	private final GUIRoot gui_root;

	public PrivateMessageForm(GUIRoot gui_root, String nick) {
		this.gui_root = gui_root;
		this.nick = nick;
		ResourceBundle bundle = ResourceBundle.getBundle(PrivateMessageForm.class.getName());
		// headline
		Label label_headline = new Label(Utils.getBundleString(bundle, "private_message_caption"), Skin.getSkin().getHeadlineFont());
		addChild(label_headline);

		Label label_name = new Label(Utils.getBundleString(bundle, "to", nick), Skin.getSkin().getEditFont());
		editline_name = new EditLine(EDITLINE_WIDTH, 256);
		editline_name.addEnterListener(new OKListener());

		addChild(label_name);
		addChild(editline_name);


		ButtonObject button_ok = new OKButton(BUTTON_WIDTH);
		button_ok.addMouseClickListener(new OKListener());
		ButtonObject button_cancel = new CancelButton(BUTTON_WIDTH);
		button_cancel.addMouseClickListener((int button, int x, int y, int clicks) -> this.cancel());

		addChild(button_ok);
		addChild(button_cancel);

		// Place objects
		label_headline.place();
		label_name.place(label_headline, BOTTOM_LEFT);
		editline_name.place(label_name, RIGHT_MID);
		button_cancel.place(ORIGIN_BOTTOM_RIGHT);
		button_ok.place(button_cancel, LEFT_MID);
		compileCanvas();
		centerPos();
	}

        @Override
	public void setFocus() {
		editline_name.setFocus();
	}

	private void send() {
		String message = editline_name.getContents();
		Network.getMatchmakingClient().sendPrivateMessage(gui_root, nick, message);
		remove();
	}

	private final class OKListener implements MouseClickListener, EnterListener {
                @Override
		public void mouseClicked(int button, int x, int y, int clicks) {
			send();
		}

                @Override
		public void enterPressed(CharSequence text) {
			send();
		}
	}
}
