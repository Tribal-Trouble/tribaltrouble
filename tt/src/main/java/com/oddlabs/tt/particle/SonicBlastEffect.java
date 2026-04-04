package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.ElementVisitor;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

public final class SonicBlastEffect extends Element<SonicBlastEffect> {
    private final @NonNull Vector3f position;
    private final float maxRadius;
    private final float duration;
    private float time;
    private boolean dead;

    @SuppressWarnings("unchecked")
    public SonicBlastEffect(@NonNull World world, @NonNull Vector3f position, float maxRadius, float duration) {
        super(world.getElementRoot());
        this.position = position;
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.time = 0;
        this.dead = false;

        setPosition(position.x, position.y);
        setPositionZ(position.z);
        updateBounds();
        register();
    }

    public void update(float dt) {
        time += dt;
        if (time >= duration) {
            dead = true;
            remove();
        }
    }

    public void abort() {
        if (!dead) {
            dead = true;
            remove();
        }
    }

    private void updateBounds() {
        setBounds(position.x - maxRadius, position.x + maxRadius,
                position.y - maxRadius, position.y + maxRadius,
                position.z - 1, position.z + 1);
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

    public float getRadius() {
        return maxRadius;
    }

    public float getDuration() {
        return duration;
    }

    @Override
    public void visit(@NonNull ElementVisitor visitor) {
        visitor.visitSonicBlastEffect(this);
    }

    @Override
    protected @NonNull SonicBlastEffect self() {
        return this;
    }

    @Override
    public void remove() {
        super.remove();
    }
}
