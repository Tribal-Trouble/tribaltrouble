package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.ModelClient;
import com.oddlabs.tt.model.MountUnitContainer;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.model.BoundingBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Logic controller for the Stun magic effect.
 */
public final class Stun extends Model implements Magic {

    private final @NonNull Unit src;
    private final float offset_x;
    private final float offset_y;
    private final float offset_z;
    private final float hit_radius;
    private final float stun_time_closest;
    private final float stun_time_farthest;
    private final @NonNull Player owner;

    private final @NonNull Iterable<? extends Selectable<?>> target_list;

    public Stun(float offset_x, float offset_y, float offset_z, float hit_radius, float stun_time_closest,
            float stun_time_farthest, @NonNull Unit src) {
        super(src.getOwner().getWorld());
        this.src = src;
        this.offset_x = offset_x;
        this.offset_y = offset_y;
        this.offset_z = offset_z;
        this.hit_radius = hit_radius;
        this.stun_time_closest = stun_time_closest;
        this.stun_time_farthest = stun_time_farthest;
        this.owner = src.getOwner();

        float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        float z = src.getPositionZ() + offset_z;
        setPosition(start_x, start_y, z);
        register();
        owner.getWorld().getAnimationManagerGameTime().registerAnimation(this);

        var filter = new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), hit_radius, src, Selectable
                .genericClass());
        UnitGrid unit_grid = owner.getWorld().getUnitGrid();
        unit_grid.scan(filter, UnitGrid.toGridCoordinate(src.getPositionX()), UnitGrid.toGridCoordinate(src
                .getPositionY()));
        target_list = filter.getResult();
    }

    @Override
    public void remove() {
        super.remove();
        owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
        getClientState(ModelClient.class).ifPresent(ModelClient::close);
    }

    @Override
    protected void onReinsert() {
        float x = getPositionX();
        float y = getPositionY();
        float z = getPositionZ();
        setBounds(x, x, y, y, z, z);
    }

    @Override
    protected @NonNull BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    private float logic_timer = 0f;
    private boolean logic_done = false;

    @Override
    public void animate(float t) {
        if (!src.isDead()) {
            float x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
            float y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
            float z = src.getPositionZ() + offset_z;
            setPosition(x, y, z);
        }

        if (!logic_done) {
            for (Selectable<?> selectable : target_list) {
                Unit unit = null;
                if (selectable instanceof Unit unit1) {
                    unit = unit1;
                } else if (selectable instanceof Building building) {
                    if (!building.isDead() && building.getAbilities().hasAbilities(Abilities.ATTACK)) {
                        unit = building.getUnitContainer().map(c -> (MountUnitContainer) c)
                                .filter(muc -> muc.getNumSupplies() > 0)
                                .map(MountUnitContainer::getUnit)
                                .orElse(null);
                    }
                }

                if (unit == null || unit.isDead())
                    continue;

                float dx = unit.getPositionX() - getPositionX();
                float dy = unit.getPositionY() - getPositionY();
                float squared_dist = dx * dx + dy * dy;
                if (owner.isEnemy(unit.getOwner()) && squared_dist < hit_radius * hit_radius) {
                    float dist = (float) Math.sqrt(squared_dist);
                    float time = calculateValueFromCurrentRadius(dist, stun_time_closest, stun_time_farthest);
                    unit.stun(time);
                }
            }
            logic_done = true;
        }

        logic_timer += t;
        if (logic_timer > 5.5f) {
            remove();
        }

        animateClientState(t);
    }

    private float calculateValueFromCurrentRadius(float current_radius, float max, float min) {
        float base_factor = 6f / 7f;
        float error = (float) Math.pow(base_factor, hit_radius);
        float factor = (float) Math.pow(base_factor, current_radius);
        float result = (max - min + error) * factor + min - error;
        return result;
    }

    @Override
    public void interrupt() {
        remove();
    }
}
