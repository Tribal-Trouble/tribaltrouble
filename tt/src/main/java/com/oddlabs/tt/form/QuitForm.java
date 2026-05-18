package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class QuitForm extends QuestionForm {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(QuitForm.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public QuitForm(final GUIRoot gui_root) {
        super(i18n(PeerHub.isWaitingForAck() ? "confirm_quit_waiting_for_ack" : "confirm_quit"), (_, _, _,
                _) -> Renderer.shutdown());
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED) {
            if (event.consumeAction(GameAction.GLOBAL_QUIT)) {
                Renderer.shutdown();
                event.consume();
                return;
            }
        }
        super.handleInput(event);
    }
}
