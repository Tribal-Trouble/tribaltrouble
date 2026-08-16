package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.util.Target;

public final class SailController extends Controller {
    private final Ship ship;
    private final Target target;
    private boolean backwards = false;
    private int trials = 0;
    private int tolerance = 16;

    private static final int MAX_TRIALS = 4;

    private boolean has_last_pose = false;
    private float last_position_x;
    private float last_position_y;
    private float last_direction_x;
    private float last_direction_y;

    public SailController(Ship ship, Target t) {
        super(1);
        this.ship = ship;
        this.target = t;
    }

    public final void decide() {
        if (ship.isDead()) {
            return;
        }
        if (moved()) {
            trials = 0;
            resetGiveUpCounter(0);
        }
        savePose();
        if (shouldGiveUp(0)) {
            if (trials == MAX_TRIALS || arrived()) {
                ship.popController();
            } else {
                trials++;
                setBehaviour();
                backwards = !backwards;
            }
        } else {
            if (!ship.slid()) {
                ship.setBehaviour(new ShipSlideBehaviour(ship));
            } else {
                setBehaviour();
                backwards = !backwards;
            }
        }
    }

    private boolean arrived() {
        int dx = ship.getGridX() - target.getGridX();
        int dy = ship.getGridY() - target.getGridY();
        return dx * dx + dy * dy < tolerance;
    }

    private boolean moved() {
        if (!has_last_pose) {
            return false;
        }
        float dx = ship.getPositionX() - last_position_x;
        float dy = ship.getPositionY() - last_position_y;
        if (dx * dx + dy * dy > 0.01f) {
            return true;
        }
        if (Math.abs(ship.getDirectionX() - last_direction_x) > 0.01f) {
            return true;
        }
        if (Math.abs(ship.getDirectionY() - last_direction_y) > 0.01f) {
            return true;
        }
        return false;
    }

    private void savePose() {
        last_position_x = ship.getPositionX();
        last_position_y = ship.getPositionY();
        last_direction_x = ship.getDirectionX();
        last_direction_y = ship.getDirectionY();
        has_last_pose = true;
    }

    private void setBehaviour() {
        if (backwards) {
            ship.setBehaviour(new ReverseSailBehaviour(ship));
        } else {
            ship.setBehaviour(new SailBehaviour(ship, target));
        }
    }
}
