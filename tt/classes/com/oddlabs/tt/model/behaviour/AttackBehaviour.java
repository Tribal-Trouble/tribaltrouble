package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;

public final strictfp class AttackBehaviour implements Behaviour {
    private static final float SECONDS_PER_ATTACK = 2f;

    private static final int THROWING = 1;
    private static final int RELEASED = 2;

    private final Selectable target;
    private final Unit unit;
    private final ShipAllocation allocation;
    private final Ship boat;
    private float anim_time;
    private int state;

    public AttackBehaviour(Unit unit, Selectable target) {
        this.unit = unit;
        this.target = target;
        this.allocation = null;
        this.boat = null;
        init();
    }

    public AttackBehaviour(Unit unit, Selectable target, ShipAllocation allocation, Ship boat) {
        this.unit = unit;
        this.target = target;
        this.boat = boat;
        this.allocation = allocation;
        init();
    }

    public final boolean isBlocking() {
        return true;
    }

    public final int animate(float t) {

        if (boat != null) {
            float x = boat.getPositionX();
            float y = boat.getPositionY();
            float dx = boat.getDirectionX();
            float dy = boat.getDirectionY();
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

        switch (state) {
            case THROWING:
                updateAttack(t);
                if (anim_time <= 0) {
                    if (unit.isMounted()) unit.getWeaponFactory().attack(unit, target, 3f);
                    else unit.getWeaponFactory().attack(unit, target);

                    anim_time +=
                            SECONDS_PER_ATTACK
                                    - unit.getWeaponFactory()
                                            .getSecondsPerRelease(1f / SECONDS_PER_ATTACK);
                    state = RELEASED;
                }
                return Selectable.UNINTERRUPTIBLE;
            case RELEASED:
                updateAttack(t);
                if (anim_time > 0) return Selectable.UNINTERRUPTIBLE;
                else return Selectable.DONE;
            default:
                throw new RuntimeException("Invalid state: " + state);
        }
    }

    private final void updateAttack(float t) {
        anim_time -= t;
        unit.aimAtTarget(target);
    }

    private final void init() {
        state = THROWING;
        anim_time += unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ATTACK);
        unit.switchAnimation(1f / SECONDS_PER_ATTACK, Unit.ANIMATION_THROWING, 0);
    }

    public final void forceInterrupted() {}
}
