package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public final class CreateChatRoomForm extends Form {
	private static final int BUTTON_WIDTH = 100;
	private static final int EDITLINE_WIDTH = 240;

	private final @NonNull EditLine editline_name;
	private final Menu main_menu;
	private final SelectGameMenu menu;
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(CreateChatRoomForm.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}
	private final GUIRoot gui_root;

	public CreateChatRoomForm(GUIRoot gui_root, Menu main_menu, SelectGameMenu menu) {
		this.gui_root = gui_root;
		this.main_menu = main_menu;
		this.menu = menu;
		// headline
		Label label_headline = new Label(i18n("create_room"), Skin.getSkin().getHeadlineFont());
		addChild(label_headline);

		Label label_name = new Label(i18n("name"), Skin.getSkin().getEditFont());
		editline_name = new EditLine(EDITLINE_WIDTH,
				MatchmakingServerInterface.MAX_ROOM_NAME_LENGTH,
				MatchmakingServerInterface.ALLOWED_ROOM_CHARS,
				Origin.AT_START);
		editline_name.addEnterListener(new OKListener());

		addChild(label_name);
		addChild(editline_name);


		ButtonObject button_ok = new OKButton(BUTTON_WIDTH);
		button_ok.addMouseClickListener(new OKListener());
		ButtonObject button_cancel = new CancelButton(BUTTON_WIDTH);
		button_cancel.addMouseClickListener( (_, _, _, _) -> this.cancel());

		addChild(button_ok);
		addChild(button_cancel);

		// Place objects
		label_headline.place();
		label_name.place(label_headline, BOTTOM_LEFT);
		editline_name.place(label_name, RIGHT_MID);
		button_cancel.place(Origin.AT_END);
		button_ok.place(button_cancel, LEFT_MID);
		compileCanvas();
	}

	@Override
	public void setFocus(@NonNull FocusDirection direction) {
		if (direction == FocusDirection.BACKWARD) {
			super.setFocus(direction);
		} else {
			editline_name.setFocus(direction);
		}
	}

	@Override
	protected void doCancel() {
		main_menu.setMenuCentered(menu);
	}

	private void create() {
		String name = editline_name.getContents();
		if (name.isEmpty()) {
			String min_name_error = i18n("min_name_error", MatchmakingServerInterface.MIN_ROOM_NAME_LENGTH);
			gui_root.addModalForm(new MessageForm(min_name_error));
		} else {
			Network.getMatchmakingClient().joinRoom(gui_root, name);
		}
		main_menu.setMenuCentered(menu);
	}

	private final class OKListener implements MouseClickListener, EnterListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			create();
		}

		@Override
		public void enterPressed(@NonNull CharSequence text) {
			create();
		}
	}
}
