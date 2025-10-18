package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Unit;

public interface MagicFactory {
	public float getHitRadius();
	public float getSecondsPerAnim();
	public float getSecondsPerInit();
	public float getSecondsPerRelease();
	public Magic execute(Unit src);
}
