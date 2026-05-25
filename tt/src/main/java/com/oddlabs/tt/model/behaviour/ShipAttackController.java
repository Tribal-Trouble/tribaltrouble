package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class ShipAttackController extends Controller {
    private static final float MIN_SCAN_DELAY = 0.1f;
    private static final float MAX_SCAN_DELAY = 0.2f;

    private final Unit unit;
    private final Ship ship;
    private final ShipAllocation allocation;
    private final AttackScanFilter scan_filter;
    private final ShipAttackBehaviour ship_attack_behaviour;
    private float redecide_time;

    public ShipAttackController(
            Unit unit, Ship ship, AttackScanFilter filter, ShipAllocation allocation) {
        super(0);
        this.unit = unit;
        this.ship = ship;
        this.allocation = allocation;
        this.scan_filter = filter;
        this.ship_attack_behaviour = new ShipAttackBehaviour(this, unit, ship, allocation);
    }

    public final boolean shouldSleep(float t) {
        redecide_time -= t;
        return redecide_time > 0;
    }

    public final void decide() {
        unit.setBehaviour(ship_attack_behaviour);
        if (shouldSleep(0f)) return;
        redecide_time =
                MIN_SCAN_DELAY
                        + unit.getOwner().getWorld().getRandom().nextFloat()
                                * (MAX_SCAN_DELAY - MIN_SCAN_DELAY);
        if (unit.getAbilities().hasAbilities(Abilities.ATTACK)) unit.scanVicinity(scan_filter);
        Selectable s = scan_filter.removeTarget();
        if (s != null) {
            unit.pushController(new AttackController(unit, s, allocation, ship));
        }
    }

    public String getKey() {
        return super.getKey()
                + unit.getAbilities().hasAbilities(Abilities.BUILD)
                + unit.getAbilities().hasAbilities(Abilities.MAGIC);
    }
}
