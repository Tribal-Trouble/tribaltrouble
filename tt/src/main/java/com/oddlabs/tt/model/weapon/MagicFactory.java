package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Unit;

public interface MagicFactory {
    float getHitRadius();

    float getSecondsPerAnim();

    float getSecondsPerInit();

    float getSecondsPerRelease();

    Magic execute(Unit src);
}
