package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;

import org.jspecify.annotations.NonNull;

public final class AttackBehaviour implements Behaviour {
    private static final float SECONDS_PER_ATTACK = 2f;

    enum AttackState {
        THROWING,
        RELEASED
    }

    private final @NonNull Selectable<?> target;
    private final @NonNull Unit unit;
    private final ShipAllocation allocation;
    private final Ship ship;
    private float anim_time;
    private @NonNull AttackState state = AttackState.THROWING;

    public AttackBehaviour(@NonNull Unit unit, @NonNull Selectable<?> target) {
        this.unit = unit;
        this.target = target;
        this.allocation = null;
        this.ship = null;
        anim_time = unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ATTACK);
        unit.switchAnimation(1f / SECONDS_PER_ATTACK, Unit.Animation.THROWING, 0);
    }

    public AttackBehaviour(@NonNull Unit unit, @NonNull Selectable target, ShipAllocation allocation, Ship ship) {
        this.unit = unit;
        this.target = target;
        this.ship = ship;
        this.allocation = allocation;
        anim_time = unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ATTACK);
        unit.switchAnimation(1f / SECONDS_PER_ATTACK, Unit.Animation.THROWING, 0);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @NonNull State animate(float t) {

        if (ship != null) {
            float x = ship.getPositionX();
            float y = ship.getPositionY();
            float dx = ship.getDirectionX();
            float dy = ship.getDirectionY();
            float ox = allocation.getOffset().x;
            float oy = allocation.getOffset().y;
            float gx = x + dx * ox - dy * oy;
            float gy = y + dy * ox + dx * oy;
            unit.setPosition(gx, gy);
            unit.setGridPosition(UnitGrid.toGridCoordinate(gx), UnitGrid.toGridCoordinate(gy));
            float rx = allocation.getRotation().x;
            float ry = allocation.getRotation().y;
            unit.setDirection(rx * dx - ry * dy, ry * dx + rx * dy);
        }

        return switch (state) {
            case THROWING -> {
                updateAttack(t);
                if (anim_time <= 0) {
                    if (unit.isMounted())
                        unit.getWeaponFactory().attack(unit, target, 3f);
                    else
                        unit.getWeaponFactory().attack(unit, target);

                    anim_time += SECONDS_PER_ATTACK - unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ATTACK);
                    state = AttackState.RELEASED;
                }
                yield State.UNINTERRUPTIBLE;
            }
            case RELEASED -> {
                updateAttack(t);
                yield anim_time > 0 ? State.UNINTERRUPTIBLE : State.DONE;
            }
        };
    }

    private void updateAttack(float t) {
        anim_time -= t;
        unit.aimAtTarget(target);
    }

    @Override
    public void forceInterrupted() {
    }
}
