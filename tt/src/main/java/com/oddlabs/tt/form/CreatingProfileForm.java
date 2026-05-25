package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.net.ProfileListener;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

public final class CreatingProfileForm extends Form implements ProfileListener {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(CreatingProfileForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final Form profiles_form;
    private final Menu main_menu;
    private final GUIRoot gui_root;

    public CreatingProfileForm(GUIRoot gui_root, Form profiles_form, Menu main_menu, String nick) {
        this.gui_root = gui_root;
        this.profiles_form = profiles_form;
        this.main_menu = main_menu;
        Label info_label = new Label(i18n("creating"), Skin.getSkin().getHeadlineFont());
        addChild(info_label);
        HorizButton cancel_button = new CancelButton(120);
        addChild(cancel_button);
        cancel_button.addMouseClickListener((_, _, _, _) -> this.cancel());

        // Place objects
        info_label.place();
        cancel_button.place(info_label, BOTTOM_MID);

        // headline
        compileCanvas();
        centerPos();

        Network.getMatchmakingClient().setCreatingProfileListener(this);
        Network.getMatchmakingClient().createProfile(nick);
    }

    @Override
    public void success() {
        remove();
        main_menu.setMenuCentered(profiles_form);
        Network.getMatchmakingClient().requestProfiles();
    }

    @Override
    public void error(int error_code) {
        remove();
        String error_message = switch (error_code) {
            case MatchmakingClientInterface.USERNAME_ERROR_TOO_MANY -> i18n("username_error_too_many");
            case MatchmakingClientInterface.PROFILE_ERROR_GUEST -> i18n("profile_error_guest", "Guest");
            case MatchmakingClientInterface.USERNAME_ERROR_ALREADY_EXISTS -> i18n("username_error_already_exists");
            case MatchmakingClientInterface.USERNAME_ERROR_INVALID_CHARACTERS ->
                i18n("username_error_invalid_characters");
            case MatchmakingClientInterface.USERNAME_ERROR_TOO_LONG -> i18n("username_error_too_long");
            case MatchmakingClientInterface.USERNAME_ERROR_TOO_SHORT -> i18n("username_error_too_short");
            default -> throw new IllegalArgumentException("Unknown error code: " + error_code);
        };
        gui_root.addModalForm(new MessageForm(error_message));
    }

}
