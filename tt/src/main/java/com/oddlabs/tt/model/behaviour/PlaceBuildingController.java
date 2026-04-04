package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Unit;

public final class PlaceBuildingController extends Controller {
    private final Building building;
    private final Unit unit;

    public PlaceBuildingController(Unit unit, Building building) {
        super(1);
        this.unit = unit;
        this.building = building;
    }

    public Building getBuilding() {
        return building;
    }

    private boolean canPlaceBuilding() {
        return unit.isCloseEnough(building.getSize(), building);
    }

    @Override
    public void decide() {
        if (building.isDead()) {
            unit.popController();
        } else if (building.isPlaced()) {
            if (building.isDamaged())
                unit.swapController(new RepairController(unit, building));
            else
                unit.popController();
        } else if (canPlaceBuilding()) {
            if (building.isPlacingLegal()) {
                building.place();
//				unit.removeNow();
                unit.swapController(new RepairController(unit, building));
            } else
                unit.popController();
        } else if (!shouldGiveUp(0)) {
            unit.setBehaviour(new WalkBehaviour(unit, building, building.getSize(), false));
        } else
            unit.popController();
    }
}
