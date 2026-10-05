package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.EditorSessionInfo;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;

/**
 * Joins a shared map editor session: waits while a player in it hands over the island, then opens the editor on it.
 */
final class JoinSessionForm extends Form implements EditorSession.JoinListener {
    private static final int STATUS_WIDTH = 420;
    private static final int BUTTON_WIDTH = 100;

    private final @NonNull GUIRoot gui_root;
    private final @NonNull NetworkSelector network;
    private final @NonNull Label label_status;
    private final @NonNull HorizButton button_cancel;
    // The session being joined, until the island is here.
    private @Nullable EditorSession joining;

    JoinSessionForm(@NonNull GUIRoot gui_root, @NonNull NetworkSelector network, @NonNull EditorSessionInfo info) {
        this.gui_root = gui_root;
        this.network = network;
        Label label_headline = new Label(MapEditor.i18n("session_joining", info.getName()),
                Skin.getSkin().getHeadlineFont());
        label_status = new Label("", Skin.getSkin().getEditFont(), STATUS_WIDTH);
        button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> cancel());
        addChild(label_headline);
        addChild(label_status);
        addChild(button_cancel);
        label_headline.place();
        label_status.place(label_headline, BOTTOM_LEFT, Skin.getSkin().getFormData().sectionSpacing());
        button_cancel.place(label_status, BOTTOM_RIGHT, Skin.getSkin().getFormData().sectionSpacing());
        compileCanvas();
        centerPos();
        joining = EditorSession.join(info, this);
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            button_cancel.setFocus(direction);
        }
    }

    @Override
    protected void doCancel() {
        EditorSession session = joining;
        joining = null;
        if (session != null)
            session.leave();
    }

    @Override
    public void progress(@NonNull String status) {
        label_status.set(status);
    }

    @Override
    public void arrived(@NonNull EditorSession session, @NonNull MapFile map) {
        joining = null;
        remove();
        ProgressForm.setProgressForm(network, gui_root.getGUI(), new MapEditorLoader(network, map.settings(),
                map.name(), map.description(), map.heights(), map.resources(), map.spawns(),
                new MapEditorLoader.SessionStart.Join(session)));
    }

    @Override
    public void failed(@NonNull String reason) {
        joining = null;
        remove();
        gui_root.addModalForm(new MessageForm(reason));
    }
}
