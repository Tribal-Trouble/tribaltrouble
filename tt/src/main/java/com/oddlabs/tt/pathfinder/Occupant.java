package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.model.Target;

public interface Occupant extends Target {
    int STATIC = 10;

    int getPenalty();
}
