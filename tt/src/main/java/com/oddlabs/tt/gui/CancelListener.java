package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseClickListener;
import org.jspecify.annotations.NonNull;

public final class CancelListener implements MouseClickListener {
    private final Form form;

    public CancelListener(Form form) {
        this.form = form;
    }

    @Override
    public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        form.cancel();
    }
}
