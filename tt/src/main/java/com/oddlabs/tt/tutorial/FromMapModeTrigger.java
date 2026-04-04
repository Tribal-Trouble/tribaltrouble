package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.camera.GameCamera;
import org.jspecify.annotations.NonNull;

public final class FromMapModeTrigger extends TutorialTrigger {
    public FromMapModeTrigger() {
        super(.1f, 1f, "from_map_mode");
    }

    @Override
    protected void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getDelegate().getCamera() instanceof GameCamera)
            tutorial.next(new SelectUnitTrigger());
    }
}
