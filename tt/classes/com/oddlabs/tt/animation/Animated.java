package com.oddlabs.tt.animation;

import com.oddlabs.tt.util.StateChecksum;

/**
 * A user interface element that changes over time
 */
public interface Animated {
	void animate(float t);
	void updateChecksum(StateChecksum checksum);
}
