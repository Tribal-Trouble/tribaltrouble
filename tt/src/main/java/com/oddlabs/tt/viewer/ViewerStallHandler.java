package com.oddlabs.tt.viewer;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.WaitingForPlayersForm;
import com.oddlabs.tt.net.StallHandler;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;

final class ViewerStallHandler implements StallHandler {

    private static final float SHOW_WAITING_DELAY_SECONDS = 3f;

    private static final ResourceBundle bundle = ResourceBundle.getBundle(ViewerStallHandler.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull WorldViewer viewer;

    private float local_stall_time;
    private int stall_tick;
    private @Nullable WaitingForPlayersForm waiting_for_players_form;

    ViewerStallHandler(@NonNull WorldViewer viewer) {
        this.viewer = viewer;
    }

    private void resetStallTime() {
        local_stall_time = LocalEventQueue.getQueue().getTime();
    }

    @Override
    public void stopStall() {
        if (waiting_for_players_form != null) {
            waiting_for_players_form.remove();
            waiting_for_players_form = null;
        }
    }

    @Override
    public void peerhubFailed() {
        stopStall();
        viewer.close();
        // On the result screen the outcome is already decided and displayed; losing the remaining
        // connections (e.g. the P2P host leaving) is not worth a dialog.
        if (!viewer.isGameConcluded())
            viewer.getGUIRoot().addModalForm(new MessageForm(i18n("connection_lost")));
    }

    @Override
    public void processStall(int tick) {
        if (stall_tick != tick) {
            IO.println("Stalled on tick " + tick);
            stall_tick = tick;
            resetStallTime();
        }
        float elapsed_time = LocalEventQueue.getQueue().getTime() - local_stall_time;
        if (tick == 0 || elapsed_time > SHOW_WAITING_DELAY_SECONDS) {
            if (waiting_for_players_form == null && !viewer.isGameConcluded()) {
                waiting_for_players_form = new WaitingForPlayersForm(viewer);
                viewer.getGUIRoot().addModalForm(waiting_for_players_form);
            }
        }
    }
}
