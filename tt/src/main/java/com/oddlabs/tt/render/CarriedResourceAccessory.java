package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitSupplyContainer;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An accessory representing resources (wood, rock, iron, rubber) currently being carried by a unit.
 */
public final class CarriedResourceAccessory implements StaticAccessory {
    private final @NonNull Unit unit;

    public CarriedResourceAccessory(@NonNull Unit unit) {
        this.unit = unit;
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        UnitSupplyContainer supply_container = unit.getSupplyContainer();
        if (supply_container != null) {
            return supply_container.getSupplyType().map(type -> VisualRegistry.getInstance().getCarriedSupplySprite(unit
                    .getOwner().getPlayerInfo().getRace(), type)
            ).orElse(null);
        }
        return null;
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        UnitSupplyContainer supply_container = unit.getSupplyContainer();
        return unit.getAbilities().hasAbilities(Abilities.BUILD) &&
                supply_container != null &&
                supply_container.getSupplyType() != null &&
                supply_container.getNumSupplies() > 0;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
        // Carried resources currently use the parent's exact transform in the old code.
        // We can add offsets here later if needed.
    }

    @Override
    public int getAnimation() {
        return unit.getAnimation();
    }
}
