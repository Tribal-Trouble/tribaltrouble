package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a world entity with visual representation and world association.
 */
public abstract class Model extends Element<Model> implements Shadowable {
    private final @NonNull World world;
    private @Nullable ClientState clientState;
    /** ground height if {@link #groundBased} */
    private float baseZ;
    /** if true then the Model is positioned relative to the terrain at (x, y) */
    private boolean groundBased = true;
    private boolean cachedSlopeValid = false;
    /** radius used when {@link #cachedSlopeOffset} was calculated */
    private float cachedSlopeRadius = Float.NaN;
    /** Offset from the base Z position to account for terrain slope surrounding model */
    private float cachedSlopeOffset;

    protected Model(@NonNull World world) {
        super(world.getElementRoot());
        this.world = world;
    }

    @Override
    protected @NonNull Model self() {
        return this;
    }

    @Override
    public float getShadowDiameter() {
        // no shadow
        return 0f;
    }

    /** {@return rendering position offset to the base z of the model */
    public float getOffsetZ() {
        return 0f;
    }

    protected final float getSlopeOffset(float radius) {
        if (!groundBased) {
            return 0f;
        }
        if (!cachedSlopeValid || radius != cachedSlopeRadius) {
            cachedSlopeOffset = calculateSlopeOffset(radius);
            cachedSlopeRadius = radius;
            cachedSlopeValid = true;
        }
        return cachedSlopeOffset;
    }

    private float calculateSlopeOffset(float r) {
        float x = getPositionX();
        float y = getPositionY();
        var hm = world.getHeightMap();

        float h_center = hm.getNearestHeight(x, y);
        float h_max = h_center;

        // Axis-aligned
        h_max = Math.max(h_max, hm.getNearestHeight(x + r, y));
        h_max = Math.max(h_max, hm.getNearestHeight(x - r, y));
        h_max = Math.max(h_max, hm.getNearestHeight(x, y + r));
        h_max = Math.max(h_max, hm.getNearestHeight(x, y - r));

        // Diagonals
        float d = r * 0.707f;
        h_max = Math.max(h_max, hm.getNearestHeight(x + d, y + d));
        h_max = Math.max(h_max, hm.getNearestHeight(x - d, y + d));
        h_max = Math.max(h_max, hm.getNearestHeight(x + d, y - d));
        h_max = Math.max(h_max, hm.getNearestHeight(x - d, y - d));

        return Math.max(0f, h_max - h_center);
    }

    public int getAnimation() {
        return 0;
    }

    public float getAnimationTicks() {
        return 0f;
    }

    public float getNoDetailSize() {
        return 0f;
    }

    /** {@return the bounds of the model in the local coordinate system for each animation} */
    protected abstract @NonNull BoundingBox @Nullable [] getLocalBounds();

    protected void updateBounds() {
        var modelBounds = getLocalBounds();
        if (modelBounds != null) {
            BoundingBox unit_bounds = modelBounds[getAnimation()];
            float x = getPositionX();
            float y = getPositionY();
            float z = getPositionZ();
            float error = getZError();
            setBounds(unit_bounds.bmin_x + x, unit_bounds.bmax_x + x, unit_bounds.bmin_y + y, unit_bounds.bmax_y + y,
                    unit_bounds.bmin_z + z - error, unit_bounds.bmax_z + z + error);
        }
    }


    protected float getZError() {
        return 0f;
    }

    protected final float getLandscapeError() {
        return world.getHeightMap().getLeafFromCoordinates(getPositionX(), getPositionY()).getMaxError();
    }

    public final @NonNull World getWorld() {
        return world;
    }

    @Override
    public final void setPosition(float x, float y) {
        super.setPosition(x, y);
        groundBased = true;
        cachedSlopeValid = false;
        reinsert();
    }

    public final void setPosition(float x, float y, float z) {
        super.setPosition(x, y);
        baseZ = z;
        groundBased = false;
        cachedSlopeValid = false;
        updateModelInternal();
    }

    @Override
    public void register() {
        if (groundBased) {
            baseZ = world.getHeightMap().getNearestHeight(getPositionX(), getPositionY());
        }
        setPositionZ(baseZ + getOffsetZ());
        updateBounds();
        onReinsert();
        super.register();
    }

    /**
     * update positions related to model position
     */
    protected void onReinsert() {
        // No-op by default
    }

    public final void reinsert() {
        if (isRegistered()) {
            if (groundBased) {
                baseZ = world.getHeightMap().getNearestHeight(getPositionX(), getPositionY());
            }
            updateModelInternal();
        }
    }

    private void updateModelInternal() {
        setPositionZ(baseZ + getOffsetZ());
        updateBounds();
        onReinsert();
        if (isRegistered()) {
            reregister();
        }
    }

    public interface ClientStateFactory {
        @Nullable
        ClientState createClientState(@NonNull Model model);
    }

    private static @Nullable ClientStateFactory clientStateFactory;

    public static void setClientStateFactory(@Nullable ClientStateFactory factory) {
        clientStateFactory = factory;
    }

    /** {@return the client state of the specified class type, or empty if not set or of a different type} */
    public final <C extends ClientState> @NonNull Optional<C> getClientState(@NonNull Class<? extends C> type) {
        if (clientState == null && clientStateFactory != null) {
            clientState = clientStateFactory.createClientState(this);
        }
        return Optional.ofNullable(type.isInstance(clientState) ? type.cast(clientState) : null);
    }

    /**
     * Sets the client state associated with this model.
     *
     * @param clientState The new client state.
     */
    public final void setClientState(@Nullable ClientState clientState) {
        this.clientState = clientState;
    }

    protected final void animateClientState(float t) {
        if (clientState == null && clientStateFactory != null) {
            clientState = clientStateFactory.createClientState(this);
        }
        if (clientState != null) {
            clientState.update(t);
        }
    }
}
