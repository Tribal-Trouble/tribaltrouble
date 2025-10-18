package com.oddlabs.tt.util;

import com.oddlabs.tt.net.Distributable;

public interface Target extends Distributable {
	int ACTION_DEFAULT = 1;
	int ACTION_MOVE = 2;
	int ACTION_ATTACK = 3;
	int ACTION_GATHER_REPAIR = 4;
	int ACTION_DEFEND = 5;

	int getGridX();
	int getGridY();
	float getPositionX();
	float getPositionY();
	float getSize();
	boolean isDead();
}
