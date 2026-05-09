package com.oddlabs.tt.gui;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.tt.form.PrivateMessageForm;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.net.ChatCommand;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

public class ChatPanel extends Panel implements ChatListener {
    private static final int PULLDOWN_INDEX_MESSAGE = 0;
    private static final int PULLDOWN_INDEX_INFO = 1;
    private static final int PULLDOWN_INDEX_IGNORE = 2;

    // Playing list has spectate between info and ignore
    private static final int PULLDOWN_INDEX_PLAYING_SPECTATE = 2;
    private static final int PULLDOWN_INDEX_PLAYING_IGNORE = 3;

    private final @NonNull MultiColumnComboBox<ChatRoomUser> lobby_users_list_box;
    private final @NonNull MultiColumnComboBox<ChatRoomUser> playing_users_list_box;
    private final @NonNull TextBox chat_box;
    private final @NonNull EditLine chat_line;
    private final @NonNull GUIRoot gui_root;

    private final int user_list_width;

    private @Nullable PrivateMessageForm private_message_form;

    private static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(ChatPanel.class.getName());
    }

    private static @NonNull String getI18N(@NonNull String key) {
        return Utils.getBundleString(getBundle(), key);
    }

    public ChatPanel(@NonNull GUIRoot gui_root, @NonNull ChatRoomInfo info, int compare_width, int compare_height, int button_width, @NonNull EnterListener chat_listener, @NonNull MouseClickListener leave_listener) {
        super(getI18N("chat"));
        this.gui_root = gui_root;
        FormData fdata = Skin.getSkin().getFormData();
        Box pdata = Skin.getSkin().getPanelData().box();
        Box edata = Skin.getSkin().getEditBox();

        Label label_headline = new Label(info.name(), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        int edit_line_height = edata.getBottomOffset() + edata.getTopOffset() + Skin.getSkin().getEditFont().getHeight();
        int height = compare_height - pdata.getTopOffset() - pdata.getBottomOffset() - edit_line_height - label_headline.getHeight() - 2 * fdata.objectSpacing();
        int user_list_height = (height - Skin.getSkin().getFormData().objectSpacing()) / 2;//- Skin.getSkin().editFont().getHeight();
        user_list_width = 2 * button_width + 2 * fdata.objectSpacing() - Skin.getSkin().getScrollBarData().scrollBar().getWidth();

        ColumnInfo[] lobby_infos = new ColumnInfo[]{
                new ColumnInfo(getI18N("lobby"), user_list_width)};
        lobby_users_list_box = new MultiColumnComboBox<>(gui_root, lobby_infos, user_list_height, true);
        addChild(lobby_users_list_box);

        ColumnInfo[] playing_infos = new ColumnInfo[]{
                new ColumnInfo(getI18N("playing"), user_list_width)};
        playing_users_list_box = new MultiColumnComboBox<>(gui_root, playing_infos, user_list_height, true);
        addChild(playing_users_list_box);

        PulldownMenu<ChatRoomUser> lobby_pulldown_menu = new PulldownMenu<>();
        lobby_pulldown_menu.addItem(new PulldownItem<>(getI18N("message")));
        lobby_pulldown_menu.addItem(new PulldownItem<>(getI18N("info")));
        lobby_pulldown_menu.addItem(new PulldownItem<>(""));
        lobby_pulldown_menu.addItemChosenListener(new PulldownListener(lobby_users_list_box));
        lobby_users_list_box.setPulldownMenu(lobby_pulldown_menu);

        ChatRoomUserDoubleClickedListener lobby_double_clicked = new ChatRoomUserDoubleClickedListener(lobby_pulldown_menu);
        lobby_users_list_box.addRowListener(lobby_double_clicked);

        PulldownMenu<ChatRoomUser> playing_pulldown_menu = new PulldownMenu<>();
        playing_pulldown_menu.addItem(new PulldownItem<>(getI18N("message")));
        playing_pulldown_menu.addItem(new PulldownItem<>(getI18N("info")));
        playing_pulldown_menu.addItem(new PulldownItem<>(getI18N("spectate")));
        playing_pulldown_menu.addItem(new PulldownItem<>(""));
        playing_pulldown_menu.addItemChosenListener(new PlayingPulldownListener(playing_users_list_box));
        playing_users_list_box.setPulldownMenu(playing_pulldown_menu);

        ChatRoomUserDoubleClickedListener playing_double_clicked = new ChatRoomUserDoubleClickedListener(playing_pulldown_menu, PULLDOWN_INDEX_PLAYING_IGNORE);
        playing_users_list_box.addRowListener(playing_double_clicked);

        int width = compare_width - pdata.getLeftOffset() - pdata.getRightOffset() - lobby_users_list_box.getWidth();
        chat_box = new TextBox(width, height, Skin.getSkin().getEditFont(), Integer.MAX_VALUE);
        addChild(chat_box);

        chat_line = new EditLine(width, 256);
        chat_line.addEnterListener(chat_listener);
        chat_line.addEnterListener(_ -> chat_line.clear());
        addChild(chat_line);

        HorizButton button_send = new HorizButton(getI18N("send"), button_width);
        button_send.addMouseClickListener((_, _, _, _) -> chat_line.enterPressedAll());
        addChild(button_send);

        HorizButton button_leave = new HorizButton(getI18N("leave"), button_width);
        button_leave.addMouseClickListener(leave_listener);
        addChild(button_leave);

        // Place chat panel objects
        label_headline.place(Origin.AT_START);
        chat_box.place(label_headline, BOTTOM_LEFT);
        lobby_users_list_box.place(chat_box, RIGHT_TOP, 0);
        playing_users_list_box.place(lobby_users_list_box, BOTTOM_LEFT);
        chat_line.place(chat_box, BOTTOM_LEFT);
        button_send.place(chat_line, RIGHT_MID);
        button_leave.place(button_send, RIGHT_MID);
        compileCanvas();
        update(info);
    }

    public final void update(@NonNull ChatRoomInfo info) {
        ChatRoomUser[] users = info.users();
        if (users != null) {
            lobby_users_list_box.clear();
            playing_users_list_box.clear();
            for (ChatRoomUser user : users) {
                int label_width = user_list_width - (Skin.getSkin().getMultiColumnComboBoxData().box().getLeftOffset() + Skin.getSkin().getMultiColumnComboBoxData().box().getRightOffset());
                Label label = new Label(user.getNick(), Skin.getSkin().getMultiColumnComboBoxData().font(), label_width);
                Row<ChatRoomUser, Label> row = new Row<>(new Label[]{label}, user);
                if (!user.isPlaying()) {
                    lobby_users_list_box.addRow(row);
                } else {
                    playing_users_list_box.addRow(row);
                }
            }
        }
        refreshMessages();
    }

    @Override
    public final void chat(@NonNull ChatMessage message) {
        if (message.type() != ChatMessage.Type.PRIVATE && message.type() != ChatMessage.Type.CHATROOM)
            return;
        if (message.type() != ChatMessage.Type.PRIVATE) {
            getTab().updateNotify();
        }
        refreshMessages();
    }

    private void refreshMessages() {
        List<@NonNull String> messages = Network.getMatchmakingClient().getChatRoomHistory();
        chat_box.clear();
        boolean first = true;
        for (var message : messages) {
            if (!first) {
                chat_box.append("\n");
            } else {
                first = false;
            }
            chat_box.append(message);
        }
        chat_box.setOffsetY(Integer.MAX_VALUE);
    }

    @Override
    public final void setFocus() {
        chat_line.setFocus();
    }

    public final void connectionLost() {
        if (private_message_form != null)
            private_message_form.remove();
    }

    private final class PulldownListener implements ItemChosenListener<@NonNull ChatRoomUser> {
        private final MultiColumnComboBox<ChatRoomUser> box;

        public PulldownListener(MultiColumnComboBox<@NonNull ChatRoomUser> box) {
            this.box = box;
        }

        @Override
        public void itemChosen(@NonNull PulldownMenu<@NonNull ChatRoomUser> menu, int item_index) {
            ChatRoomUser user = box.getRightClickedRowData();
            String nick = user.getNick();
            switch (item_index) {
                case PULLDOWN_INDEX_MESSAGE:
                    gui_root.addModalForm(new PrivateMessageForm(gui_root, nick));
                    break;
                case PULLDOWN_INDEX_INFO:
                    Network.getMatchmakingClient().requestInfo(gui_root, nick);
                    break;
                case PULLDOWN_INDEX_IGNORE:
                    if (ChatCommand.isIgnoring(nick))
                        ChatCommand.unignore(gui_root.getInfoPrinter(), nick);
                    else
                        ChatCommand.ignore(gui_root.getInfoPrinter(), nick);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected pulldown index");
            }
            box.setFocus();
        }
    }

    private final class PlayingPulldownListener implements ItemChosenListener<@NonNull ChatRoomUser> {
        private final MultiColumnComboBox<ChatRoomUser> box;

        public PlayingPulldownListener(MultiColumnComboBox<@NonNull ChatRoomUser> box) {
            this.box = box;
        }

        @Override
        public void itemChosen(@NonNull PulldownMenu<@NonNull ChatRoomUser> menu, int item_index) {
            ChatRoomUser user = box.getRightClickedRowData();
            String nick = user.getNick();
            switch (item_index) {
                case PULLDOWN_INDEX_MESSAGE:
                    gui_root.addModalForm(new PrivateMessageForm(gui_root, nick));
                    break;
                case PULLDOWN_INDEX_INFO:
                    Network.getMatchmakingClient().requestInfo(gui_root, nick);
                    break;
                case PULLDOWN_INDEX_PLAYING_SPECTATE:
                    Network.getMatchmakingClient().requestSpectate(gui_root, nick);
                    break;
                case PULLDOWN_INDEX_PLAYING_IGNORE:
                    if (ChatCommand.isIgnoring(nick))
                        ChatCommand.unignore(gui_root.getInfoPrinter(), nick);
                    else
                        ChatCommand.ignore(gui_root.getInfoPrinter(), nick);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected pulldown index");
            }
            box.setFocus();
        }
    }

    private final class ChatRoomUserDoubleClickedListener implements RowListener<@NonNull ChatRoomUser> {
        private final @NonNull PulldownMenu<@NonNull ChatRoomUser> menu;
        private final int ignoreIndex;

        public ChatRoomUserDoubleClickedListener(@NonNull PulldownMenu<@NonNull ChatRoomUser> menu) {
            this(menu, PULLDOWN_INDEX_IGNORE);
        }

        public ChatRoomUserDoubleClickedListener(@NonNull PulldownMenu<@NonNull ChatRoomUser> menu, int ignoreIndex) {
            this.menu = menu;
            this.ignoreIndex = ignoreIndex;
        }

        @Override
        public void rowDoubleClicked(@NonNull ChatRoomUser user) {
            private_message_form = new PrivateMessageForm(gui_root, user.getNick());
            gui_root.addModalForm(private_message_form);
        }

        @Override
        public void rowChosen(@NonNull ChatRoomUser user) {
            String item_text = ChatCommand.isIgnoring(user.getNick())
                    ? getI18N("unignore") : getI18N("ignore");
            menu.getItem(ignoreIndex).setLabelString(item_text);
        }
    }

}
