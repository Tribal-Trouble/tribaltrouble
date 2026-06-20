package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Element;
import org.jspecify.annotations.NonNull;

/**
 * Visual element that appears as a target indicator when the user clicks on the landscape.
 */
public final class LandscapeTargetRespond extends Element<LandscapeTargetRespond> implements Animated {
    public static final int SIZE = 128;
    private static final float SECOND_PER_PICK_RESPOND = 1f / 3f;

    private float time;

    public LandscapeTargetRespond(@NonNull World world, float x, float y) {
        super(world.getElementRoot());
        setPosition(x, y);
        setPositionZ(world.getHeightMap().getNearestHeight(x, y));
        setBounds(x - SIZE / 2, x + SIZE / 2, y - SIZE / 2, y + SIZE / 2, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY);
        register();
    }

    @Override
    protected @NonNull LandscapeTargetRespond self() {
        return this;
    }

    @Override
    public void animate(float t) {
        if (time > 0) {
            time = Math.max(0, time - t);
        } else {
            remove();
        }
    }

    public float getProgress() {
        return time / SECOND_PER_PICK_RESPOND;
    }

    @Override
    public void register() {
        super.register();
        time = SECOND_PER_PICK_RESPOND;
        Renderer.getRenderer().getEventQueue().getManager().registerAnimation(this);
    }

    @Override
    public void remove() {
        super.remove();
        Renderer.getRenderer().getEventQueue().getManager().removeAnimation(this);
    }
}
