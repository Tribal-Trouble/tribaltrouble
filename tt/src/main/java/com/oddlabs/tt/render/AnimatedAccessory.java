package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;

/**
 * An {@link Accessory} that requires continuous animation updates.
 * Used for dynamic visuals like particle emitters or cycling animations.
 */
public non-sealed interface AnimatedAccessory extends Accessory, Animated {
}
