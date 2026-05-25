package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Profile;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.IntegerLabel;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Origin.AT_END;
import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

public final class ProfilesForm extends Form {
    private static final int NICK_SIZE = 200;

    private final @NonNull Menu main_menu;
    private final @NonNull SelectGameMenu game_menu;
    private final @NonNull MultiColumnComboBox<String> profile_list_box;
    private final @NonNull HorizButton join_button;
    private final @NonNull GUIRoot gui_root;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ProfilesForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    // to be removed if connection lost
    private NewProfileForm new_profile_form;
    private QuestionForm confirm_delete_form;

    public ProfilesForm(@NonNull GUIRoot gui_root, @NonNull Menu main_menu, @NonNull SelectGameMenu game_menu) {
        this.gui_root = gui_root;
        this.main_menu = main_menu;
        this.game_menu = game_menu;
        Label label_headline = new Label(i18n("profiles_caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        ColumnInfo[] infos = new ColumnInfo[]{new ColumnInfo(i18n("nick"), NICK_SIZE), new ColumnInfo(i18n("rating"),
                120), new ColumnInfo(i18n("wins"), 100), new ColumnInfo(i18n("losses"), 100), new ColumnInfo(i18n(
                        "invalid"), 100)};
        profile_list_box = new MultiColumnComboBox<>(gui_root, infos, 200);
        profile_list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(@NonNull String nick) {
                join(nick);
            }
        });
        addChild(profile_list_box);

        HorizButton create_profile_button = new HorizButton(i18n("create_new_profile"), 150);
        create_profile_button.addMouseClickListener((_, _, _, _) -> {
            new_profile_form = new NewProfileForm(gui_root, main_menu, ProfilesForm.this);
            main_menu.setMenu(new_profile_form);
        });
        addChild(create_profile_button);

        HorizButton delete_profile_button = new HorizButton(i18n("delete_profile"), 150);
        delete_profile_button.addMouseClickListener(new DeleteProfileListener());
        addChild(delete_profile_button);

        join_button = new HorizButton(i18n("join"), 100);
        join_button.addMouseClickListener((_, _, _, _) -> {
            String nick = profile_list_box.getSelected();
            if (nick == null) {
                gui_root.addModalForm(new MessageForm(i18n("no_profiles")));
            } else {
                join(nick);
            }
        });
        addChild(join_button);

        HorizButton logout_button = new HorizButton(i18n("logout"), 100);
        logout_button.addMouseClickListener((@NonNull MouseButton _, int _, int _, int _) -> this.cancel());
        addChild(logout_button);

        label_headline.place();
        profile_list_box.place(label_headline, BOTTOM_LEFT);
        logout_button.place(AT_END);
        join_button.place(logout_button, LEFT_MID);
        delete_profile_button.place(join_button, LEFT_MID);
        create_profile_button.place(delete_profile_button, LEFT_MID);

        compileCanvas();
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            join_button.setFocus(direction);
        }
    }

    @Override
    protected void doCancel() {
        Network.getMatchmakingClient().close();
    }

    public void receivedProfiles(Profile @NonNull [] profiles, String last_nick) {
        profile_list_box.clear();
        Row<String, Label> selected_row = null;
        for (Profile p : profiles) {
            Row<String, Label> row = new Row<>(new Label[]{new Label(p.getNick(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), NICK_SIZE), new IntegerLabel(p.getRating(),
                            Skin.getSkin().getMultiColumnComboBoxData().font()), new IntegerLabel(p.getWins(),
                                    Skin.getSkin().getMultiColumnComboBoxData().font()), new IntegerLabel(p.getLosses(),
                                            Skin.getSkin().getMultiColumnComboBoxData().font()), new IntegerLabel(
                                                    p.getInvalid(),
                                                    Skin.getSkin().getMultiColumnComboBoxData().font())}, p.getNick());
            profile_list_box.addRow(row);
            if (p.getNick().equalsIgnoreCase(last_nick))
                selected_row = row;
        }
        if (selected_row != null)
            profile_list_box.selectRow(selected_row);
    }

    private void join(String nick) {
        Network.getMatchmakingClient().setProfile(nick);
        main_menu.setMenuCentered(game_menu);
    }

    private final class DeleteProfileListener implements MouseClickListener {
        @Override
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            String nick = profile_list_box.getSelected();
            if (nick == null) {
                gui_root.addModalForm(new MessageForm(i18n("no_profiles")));
            } else {
                String confirm_str = i18n("confirm_delete", nick);
                confirm_delete_form = new QuestionForm(confirm_str, (_, _, _, _) -> {
                    Network.getMatchmakingClient().deleteProfile(nick);
                    Network.getMatchmakingClient().requestProfiles();
                });
                gui_root.addModalForm(confirm_delete_form);
            }
        }
    }

    public void connectionLost() {
        if (new_profile_form != null)
            new_profile_form.connectionLost();
        if (confirm_delete_form != null)
            confirm_delete_form.connectionLost();
        remove();
    }
}
