package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseClickListener;
import org.jspecify.annotations.NonNull;

public final class OKListener implements MouseClickListener {
    private final Form form;

    public OKListener(Form form) {
        this.form = form;
    }

    @Override
    public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        form.remove();
    }
}
