package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.EditorSessionInfo;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.InfoPrinter;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.TextBox;
import com.oddlabs.tt.net.ChatCommand;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;

/**
 * The chat of a shared editor session, as a game's chat: what was said so far above a line to type in. Enter sends
 * the line, and on an empty line closes the chat; chat commands such as /ignore work as in a game.
 */
final class EditorChatForm extends Form {
    private static final int CHAT_WIDTH = 400;
    private static final int BUTTON_WIDTH = 60;
    private static final int CHAT_HEIGHT = 150;

    private final @NonNull InfoPrinter info_printer;
    private final @NonNull EditorSession session;
    private final @NonNull EditLine chat_line;
    private final @NonNull TextBox chat_box;

    EditorChatForm(@NonNull InfoPrinter info_printer, @NonNull EditorSession session) {
        super(MapEditor.i18n("chat"));
        this.info_printer = info_printer;
        this.session = session;
        chat_line = new EditLine(CHAT_WIDTH, EditorSessionInfo.MAX_CHAT_LENGTH);
        chat_line.addEnterListener(this::send);
        HorizButton button_send = new HorizButton(MapEditor.i18n("chat_send"), BUTTON_WIDTH);
        button_send.addMouseClickListener((_, _, _, _) -> chat_line.enterPressedAll());
        chat_box = new TextBox(CHAT_WIDTH + BUTTON_WIDTH, CHAT_HEIGHT, Skin.getSkin().getEditFont(),
                Integer.MAX_VALUE);
        addChild(chat_line);
        addChild(button_send);
        addChild(chat_box);
        chat_line.place();
        button_send.place(chat_line, RIGHT_MID);
        chat_box.place(chat_line, TOP_LEFT);
        compileCanvas();
        refresh();
    }

    /** Shows the session's chat as it is now, scrolled to the newest. */
    void refresh() {
        List<String> messages = session.getChat();
        chat_box.clear();
        for (int i = 0; i < messages.size(); i++) {
            if (i != 0)
                chat_box.append("\n");
            chat_box.append(messages.get(i));
        }
        chat_box.setOffsetY(Integer.MAX_VALUE);
    }

    private void send(@NonNull CharSequence text) {
        String line = text.toString();
        if (line.isBlank()) {
            cancel();
            return;
        }
        chat_line.clear();
        if (!ChatCommand.filterCommand(info_printer, line))
            session.sendChat(line);
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            chat_line.setFocus(direction);
        }
    }
}
