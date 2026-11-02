package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.EditorSessionInfo;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.ServerMessageBundler;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * The multiplayer menu's window of shared map editor sessions: islands players are editing together. Join one to edit
 * it with them, or host one of your own.
 */
public final class EditorSessionsForm extends Form {
    private static final int NAME_WIDTH = 330;
    private static final int HOST_WIDTH = 170;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_WIDTH_LONG = 170;
    private static final int LIST_HEIGHT = 350;

    private final @NonNull GUIRoot gui_root;
    private final @NonNull NetworkSelector network;
    private final @NonNull MultiColumnComboBox<EditorSessionInfo> list;
    private final @NonNull HorizButton button_join;
    private final @NonNull HorizButton button_close;

    /** @param host opens the map editor's start window to host a session from, once this window is closed */
    public EditorSessionsForm(@NonNull GUIRoot gui_root, @NonNull NetworkSelector network, @NonNull Runnable host) {
        this.gui_root = gui_root;
        this.network = network;
        Label label_headline = new Label(MapEditor.i18n("session_headline"), Skin.getSkin().getHeadlineFont());
        ColumnInfo[] columns = new ColumnInfo[]{new ColumnInfo(MapEditor.i18n("column_name"),
                NAME_WIDTH), new ColumnInfo(MapEditor.i18n("column_host"), HOST_WIDTH), new ColumnInfo(MapEditor.i18n(
                        "column_size"), 120), new ColumnInfo(MapEditor.i18n("column_editors"), 100)};
        list = new MultiColumnComboBox<>(gui_root, columns, LIST_HEIGHT);
        list.addRowListener(new RowListener<>() {
            @Override
            public void rowChosen(@NonNull EditorSessionInfo info) {
                button_join.setDisabled(false);
            }

            @Override
            public void rowDoubleClicked(@NonNull EditorSessionInfo info) {
                join(info);
            }
        });

        HorizButton button_update = new HorizButton(MapEditor.i18n("session_update"), BUTTON_WIDTH_LONG);
        button_update.addMouseClickListener((_, _, _, _) -> refresh());
        HorizButton button_host = new HorizButton(MapEditor.i18n("session_host"), BUTTON_WIDTH_LONG);
        button_host.addMouseClickListener((_, _, _, _) -> {
            if (Network.getMatchmakingClient().getProfile() != null) {
                remove();
                host.run();
            }
        });
        button_join = new HorizButton(MapEditor.i18n("session_join"), BUTTON_WIDTH);
        button_join.addMouseClickListener((_, _, _, _) -> {
            EditorSessionInfo selected = list.getSelected();
            if (selected != null)
                join(selected);
        });
        button_close = new HorizButton(MapEditor.i18n("session_close"), BUTTON_WIDTH);
        button_close.addMouseClickListener((_, _, _, _) -> cancel());

        addChild(label_headline);
        addChild(list);
        addChild(button_update);
        addChild(button_host);
        addChild(button_join);
        addChild(button_close);
        label_headline.place();
        list.place(label_headline, BOTTOM_LEFT);
        button_update.place(list, BOTTOM_LEFT);
        button_host.place(button_update, RIGHT_MID);
        button_join.place(button_host, RIGHT_MID);
        button_close.place(list, BOTTOM_RIGHT);
        compileCanvas();
        centerPos();
        button_join.setDisabled(true);
        refresh();
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            button_close.setFocus(direction);
        }
    }

    private static void refresh() {
        Network.getMatchmakingClient().requestList(MatchmakingServerInterface.TYPE_EDITOR_SESSION_LIST);
    }

    /** Empties the list, as a new one is coming from the server. */
    public void clear() {
        list.clear();
        button_join.setDisabled(true);
    }

    /** Adds sessions the server lists. */
    public void add(Object @NonNull [] sessions) {
        Font font = Skin.getSkin().getMultiColumnComboBoxData().font();
        for (Object entry : sessions) {
            if (!(entry instanceof EditorSessionInfo info))
                continue;
            Row<EditorSessionInfo, Label> row = new Row<>(List.of(
                    new Label(info.getName(), font, NAME_WIDTH),
                    new Label(info.getHost(), font, HOST_WIDTH),
                    new Label(ServerMessageBundler.getSizeString(info.getSize()), font),
                    new Label(MapEditor.i18n("session_editors", info.getMembers(), EditorSessionInfo.MAX_MEMBERS),
                            font)),
                    info);
            list.addRow(row);
        }
    }

    private void join(@NonNull EditorSessionInfo info) {
        if (Network.getMatchmakingClient().getProfile() == null)
            return;
        if (info.getMembers() >= EditorSessionInfo.MAX_MEMBERS) {
            gui_root.addModalForm(new MessageForm(MapEditor.i18n("session_error_" + EditorSessionInfo.ERROR_FULL)));
            return;
        }
        gui_root.addModalForm(new JoinSessionForm(gui_root, network, info));
    }
}
