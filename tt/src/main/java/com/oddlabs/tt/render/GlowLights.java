package com.oddlabs.tt.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** The glowing models drawn in a frame that light their surroundings; the next frame is lit by them. */
public final class GlowLights {
    /** A point light: where it is, how far it reaches and its color. */
    public record Light(float x, float y, float z, float radius, float r, float g, float b) {
    }

    // A set, since the water reflection draws the same models again.
    private Set<Light> drawn = new LinkedHashSet<>();
    private Set<Light> lit = new LinkedHashSet<>();
    private final Matrix4f transform = new Matrix4f();
    private final Vector3f position = new Vector3f();

    /** Adds light, given in the model's space, where model is drawn. */
    void add(@NonNull ModelState<?> model, @NonNull Light light) {
        model.getTransform(transform).transformPosition(light.x(), light.y(), light.z(), position);
        drawn.add(new Light(position.x, position.y, position.z, light.radius(), light.r(), light.g(), light.b()));
    }

    /** The lights drawn since the last call; drawing starts collecting anew. */
    public @NonNull Collection<Light> takeDrawn() {
        Set<Light> taken = drawn;
        drawn = lit;
        drawn.clear();
        lit = taken;
        return taken;
    }
}
