package com.oddlabs.tt.util;

import com.oddlabs.tt.net.Distributable;

public interface Target extends Distributable {
	int getGridX();
	int getGridY();
	float getPositionX();
	float getPositionY();
	float getSize();
	boolean isDead();
	default boolean isAlive() { return !isDead(); }
}
