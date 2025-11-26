package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Login;
import com.oddlabs.matchmaking.LoginDetails;
import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.MainMenu;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.ChatRoomInfo;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.net.MatchmakingListener;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

public final class MatchmakingConnectingForm extends Form implements MatchmakingListener {
	private final Form parent_form;
	private final MainMenu main_menu;
	private final ResourceBundle bundle = ResourceBundle.getBundle(MatchmakingConnectingForm.class.getName());
	private final GUIRoot gui_root;
	private final @NonNull NetworkSelector network;

	public MatchmakingConnectingForm(@NonNull NetworkSelector network, GUIRoot gui_root, Form parent_form, MainMenu main_menu, Login login, LoginDetails login_details) {
		this.parent_form = parent_form;
		this.main_menu = main_menu;
		this.gui_root = gui_root;
		this.network = network;
		Label info_label = new Label(Utils.getBundleString(bundle, "connecting"), Skin.getSkin().getHeadlineFont());
		addChild(info_label);
		HorizButton cancel_button = new CancelButton(120);
		addChild(cancel_button);
		cancel_button.addMouseClickListener((int _, int _, int _, int _) -> this.cancel());

		// Place objects
		info_label.place();
		cancel_button.place(info_label, BOTTOM_MID);

		// headline
		compileCanvas();
		centerPos();
		Network.setMatchmakingListener(this);
		Network.getMatchmakingClient().login(network, login, login_details);
	}

	@Override
	public void clearList(int type) {
		assert false;
	}

	@Override
	public void receivedList(int type, Object[] names) {
		assert false;
	}

	@Override
	public void joinedChat(ChatRoomInfo info) {
		assert false;
	}

	@Override
	public void updateChatRoom(ChatRoomInfo info) {
		assert false;
	}

	@Override
	public void receivedProfiles(Profile[] profiles, String last_nick) {
		assert false;
	}

	@Override
	public void doRemove() {
		super.doRemove();
		Network.setMatchmakingListener(null);
	}

	@Override
	public void connectionLost() {
		remove();
		gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "connection_failed")));
	}

	@Override
	public void loginError(int error_code) {
		remove();
		String error_message;
		switch (error_code) {
			case MatchmakingClientInterface.USERNAME_ERROR_TOO_MANY:
				error_message = Utils.getBundleString(bundle, "username_error_too_many");
				break;
			case MatchmakingClientInterface.USER_ERROR_VERSION_TOO_OLD:
				error_message = Utils.getBundleString(bundle, "user_error_version_too_old");
				break;
			case MatchmakingClientInterface.USER_ERROR_NO_SUCH_USER:
				error_message = Utils.getBundleString(bundle, "user_error_no_such_user");
				break;
			case MatchmakingClientInterface.USER_ERROR_INVALID_EMAIL:
				error_message = Utils.getBundleString(bundle, "user_error_invalid_email");
				break;
			case MatchmakingClientInterface.USERNAME_ERROR_ALREADY_EXISTS:
				error_message = Utils.getBundleString(bundle, "username_error_already_exists");
				break;
			case MatchmakingClientInterface.USERNAME_ERROR_INVALID_CHARACTERS:
				error_message = Utils.getBundleString(bundle, "username_error_invalid_characters");
				break;
			case MatchmakingClientInterface.USERNAME_ERROR_TOO_LONG:
				error_message = Utils.getBundleString(bundle, "username_error_too_long");
				break;
			case MatchmakingClientInterface.USERNAME_ERROR_TOO_SHORT:
				error_message = Utils.getBundleString(bundle, "username_error_too_short");
				break;
			default:
				throw new RuntimeException("Unknown error code: " + error_code);
		}
		gui_root.addModalForm(new MessageForm(error_message));
	}

	@Override
	public void loggedIn() {
		remove();
		if (parent_form != null)
			parent_form.remove();
		new SelectGameMenu(network, gui_root, main_menu);
	}

	@Override
	protected void doCancel() {
		Network.getMatchmakingClient().close();
	}
}
