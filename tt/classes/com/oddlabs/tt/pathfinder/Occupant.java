package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.util.Target;

public interface Occupant extends Target {
	public final static int STATIC = 10;

	public int getPenalty();
}
