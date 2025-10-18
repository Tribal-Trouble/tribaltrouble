package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.util.Target;

public interface Occupant extends Target {
	int STATIC = 10;

	int getPenalty();
}
