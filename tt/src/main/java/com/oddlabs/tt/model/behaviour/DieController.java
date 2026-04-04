package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class DieController extends Controller {
    private final @NonNull Unit unit;

    public DieController(@NonNull Unit unit) {
        super(0);
        this.unit = unit;
    }

    @Override
    public void decide() {
        unit.setBehaviour(new DieBehaviour(unit));
    }
}
