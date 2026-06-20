package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Element;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

/**
 * A transient expanding-ring effect rendered by {@link com.oddlabs.tt.render.SonicBlastRenderer}.
 * Advances through its animation each game tick and removes itself when its duration elapses.
 */
public final class SonicBlastEffect extends Element<SonicBlastEffect> implements Animated {
    private final @NonNull World world;
    private final @NonNull Vector3f position;
    private final float maxRadius;
    private final float duration;
    private final Color.@NonNull Linear color;
    private float time;
    private boolean dead;

    @SuppressWarnings("unchecked")
    public SonicBlastEffect(@NonNull World world, @NonNull Vector3f position, float maxRadius, float duration) {
        this(world, position, maxRadius, duration, new Color.Linear(0.7f, 0.85f, 1.0f, 1.0f));
    }

    @SuppressWarnings("unchecked")
    public SonicBlastEffect(@NonNull World world, @NonNull Vector3f position, float maxRadius, float duration,
            Color.@NonNull Linear color) {
        super(world.getElementRoot());
        this.world = world;
        this.position = position;
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.color = color;
        this.time = 0;
        this.dead = false;

        setPosition(position.x, position.y);
        setPositionZ(position.z);
        updateBounds();
        register();
        world.getAnimationManagerGameTime().registerAnimation(this);
    }

    public Color.@NonNull Linear getColor() {
        return color;
    }

    public void update(float dt) {
        if (dead) return;
        time += dt;
        if (time >= duration) {
            dead = true;
            world.getAnimationManagerGameTime().removeAnimation(this);
            remove();
        }
    }

    public void abort() {
        if (!dead) {
            dead = true;
            world.getAnimationManagerGameTime().removeAnimation(this);
            remove();
        }
    }

    @Override
    public void animate(float t) {
        update(t);
    }

    private void updateBounds() {
        setBounds(position.x - maxRadius, position.x + maxRadius,
                position.y - maxRadius, position.y + maxRadius,
                position.z - 1, position.z + 1);
    }

    public @NonNull World getWorld() {
        return world;
    }

    public @NonNull Vector3f getPosition() {
        return position;
    }

    public float getTime() {
        return time;
    }

    public float getMaxRadius() {
        return maxRadius;
    }

    public boolean isDead() {
        return dead;
    }

    public float getDuration() {
        return duration;
    }

    @Override
    protected @NonNull SonicBlastEffect self() {
        return this;
    }
}
