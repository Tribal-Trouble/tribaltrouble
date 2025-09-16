package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.Target;

public final strictfp class SailController extends Controller {
    private final Building boat;
    private final Target target1;
    private final Target target2;
    private final Target target3;
    private final int LAND_TO_SEA = 0;
    private final int SEA_TO_SEA = 1;
    private final int SEA_TO_LAND = 2;
    private int state = LAND_TO_SEA;

    public SailController(Building boat, Target t) {
        super(1);
        this.boat = boat;
        UnitGrid grid = boat.getUnitGrid();
        int[] pt1 = grid.nearestSeaPoint(boat.getGridX(), boat.getGridY());
        int[] pt2 = grid.nearestSeaPoint(t.getGridX(), t.getGridY());
        this.target1 = new LandscapeTarget(pt1[0], pt1[1]);
        this.target2 = new LandscapeTarget(pt2[0], pt2[1]);
        this.target3 = t;
    }

    public final void decide() {
        if (shouldGiveUp(0)) {
            switch (state) {
                case LAND_TO_SEA:
                    boat.popController();
                    boat.setBehaviour(new SailBehaviour(boat, target2, 0f));
                    state = SEA_TO_SEA;
                    break;
                case SEA_TO_SEA:
                    boat.popController();
                    boat.setBehaviour(new SailBehaviour(boat, target3, 0f));
                    state = SEA_TO_LAND;
                    break;
                default:
                    boat.popController();
                    break;
            }
        } else {
            boat.setBehaviour(new SailBehaviour(boat, target1, 0f));
        }
    }

    public final boolean isAgressive() {
        return false;
    }

    public final Target getTarget() {
        switch (state) {
            case LAND_TO_SEA:
                return target1;
            case SEA_TO_SEA:
                return target2;
            case SEA_TO_LAND:
            default:
                return target3;
        }
    }
}
