package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.StaticOccupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Vector2f;
import com.oddlabs.util.Vector4f;

import java.util.HashSet;

public final strictfp class SailBehaviour implements Behaviour {
    private static final float BOAT_SPEED = 20.0f;
    private static final float DESTINATION_THRESHOLD = 2.0f;
    private static final int NO_COLLISION = 0;
    private static final int RESOLVABLE_COLLISION = 1;
    private static final int UNRESOLVABLE_COLLISION = 2;

    private final Building boat;
    private final Target target;

    private final Vector2f p0 = new Vector2f();
    private final Vector2f p1 = new Vector2f();
    private final Vector2f p2 = new Vector2f();
    private final float curveDt;
    private final float direction;
    private float curveT;

    private boolean blocked = false;
    private boolean left_shore = false;

    public SailBehaviour(Building boat, Target t, float range) {
        this.boat = boat;
        this.target = t;

        int collision_fwd = checkCollisionAhead(boat.getDirectionX(), boat.getDirectionY(), true);
        int collision_bwd = checkCollisionAhead(-boat.getDirectionX(), -boat.getDirectionY(), true);

        if (collision_fwd == NO_COLLISION) {
            direction = 1.0f;
        } else if (collision_bwd == NO_COLLISION) {
            direction = -1.0f;
        } else if (collision_fwd == RESOLVABLE_COLLISION) {
            direction = 1.0f;
        } else if (collision_bwd == RESOLVABLE_COLLISION) {
            direction = -1.0f;
        } else {
            // It'll be blocked and abort anyway
            direction = 1.0f;
        }

        float dx = t.getPositionX() - boat.getPositionX();
        float dy = t.getPositionY() - boat.getPositionY();
        double d = StrictMath.sqrt(dx * dx + dy * dy);
        double L = d * 0.3f;
        p0.x = boat.getPositionX();
        p0.y = boat.getPositionY();
        p1.x = (float) (p0.x + L * direction * boat.getDirectionX());
        p1.y = (float) (p0.y + L * direction * boat.getDirectionY());
        p2.x = t.getPositionX();
        p2.y = t.getPositionY();

        float length = 0.0f;
        Vector4f prev = new Vector4f(p0.x, p0.y, 0.0f, 0.0f);
        for (int i = 1; i <= 100; i++) {
            Vector4f pt = calcCurve(i / 100.0f);
            float sx = pt.x - prev.x;
            float sy = pt.y - prev.y;
            length += StrictMath.sqrt(sx * sx + sy * sy);
        }

        curveDt = 1.0f / length;
        curveT = 0.0f;
    }

    private Vector4f calcCurve(float t) {
        float omt = 1 - t;
        float omt2 = omt * omt;
        float t2 = t * t;
        float x = omt2 * p0.x + 2 * omt * t * p1.x + t2 * p2.x;
        float y = omt2 * p0.y + 2 * omt * t * p1.y + t2 * p2.y;
        float dx = 2 * omt * (p1.x - p0.x) + 2 * t * (p2.x - p1.x);
        float dy = 2 * omt * (p1.y - p0.y) + 2 * t * (p2.y - p1.y);
        float lv = (float) StrictMath.sqrt(dx * dx + dy * dy);
        dx = dx / lv;
        dy = dy / lv;
        return new Vector4f(x, y, dx, dy);
    }

    public final boolean isBlocking() {
        return blocked;
    }

    public void appendToolTip(ToolTipBox tool_tip_box) {
        tool_tip_box.append("SailBehaviour: ");
        if (blocked) {
            tool_tip_box.append("BLOCKED");
        } else {
            tool_tip_box.append("MOVING");
        }
    }

    private final int checkCollisionAhead(float dir_x, float dir_y, boolean check_land) {
        UnitGrid grid = boat.getUnitGrid();
        int boat_grid_x = boat.getGridX();
        int boat_grid_y = boat.getGridY();
        float boat_x = boat.getPositionX();
        float boat_y = boat.getPositionY();
        float target_x = target.getPositionX();
        float target_y = target.getPositionY();
        float perp_x = -dir_y;
        float perp_y = dir_x;
        HashSet obstacleSet = new HashSet();
        for (int i = 2; i <= 6; i++) {
            for (int j = -2; j <= 2; j++) {
                // Calculate grid position: i cells forward, j cells perpendicular
                float forward_offset_x = dir_x * i;
                float forward_offset_y = dir_y * i;
                float perp_offset_x = perp_x * j;
                float perp_offset_y = perp_y * j;
                int check_grid_x =
                        boat_grid_x + (int) StrictMath.round(forward_offset_x + perp_offset_x);
                int check_grid_y =
                        boat_grid_y + (int) StrictMath.round(forward_offset_y + perp_offset_y);
                if (check_grid_x >= 0
                        && check_grid_x < grid.getGridSize()
                        && check_grid_y >= 0
                        && check_grid_y < grid.getGridSize()) {
                    Occupant object = grid.getOccupant(check_grid_x, check_grid_y, UnitGrid.SEA);
                    if (object != null && object instanceof Selectable) {
                        Selectable selectable = (Selectable) object;
                        if (selectable != null && selectable != boat) {
                            obstacleSet.add(selectable);
                        }
                    }

                    object = grid.getOccupant(check_grid_x, check_grid_y, UnitGrid.LAND);
                    if (object != null
                            && object instanceof Selectable
                            && !(object instanceof StaticOccupant)) {
                        Selectable selectable = (Selectable) object;
                        if (selectable != null && selectable != boat) {
                            obstacleSet.add(selectable);
                        }
                    }
                }
            }
        }

        int check_grid_x = (int) StrictMath.round(boat_grid_x + dir_x * 2);
        int check_grid_y = (int) StrictMath.round(boat_grid_y + dir_y * 2);
        if (!grid.isWater(check_grid_x, check_grid_y) && check_land) {
            return UNRESOLVABLE_COLLISION;
        }

        Region regionSea = grid.getRegion(check_grid_x, check_grid_y, UnitGrid.SEA);
        Region regionLand = grid.getRegion(check_grid_x, check_grid_y, UnitGrid.LAND);
        if (regionSea == null && regionLand == null) {
            return UNRESOLVABLE_COLLISION;
        }

        Object[] objects = obstacleSet.toArray();
        for (int i = 0; i < objects.length; i++) {
            if (checkLineIntersectionWithOccupant(
                    boat_x, boat_y, target_x, target_y, (Selectable) objects[i])) {
                return RESOLVABLE_COLLISION;
            }
        }

        return NO_COLLISION;
    }

    // Check if boat's path line intersects with occupant's bounding box (30m x 7m)
    private final boolean checkLineIntersectionWithOccupant(
            float line_x1, float line_y1, float line_x2, float line_y2, Selectable occupant) {
        // Get occupant's position and direction
        float occ_x = occupant.getPositionX();
        float occ_y = occupant.getPositionY();
        float occ_dir_x = occupant.getDirectionX();
        float occ_dir_y = occupant.getDirectionY();

        // Bounding box dimensions: 30m x 7m
        final float BOX_LENGTH = 30.0f; // along direction
        final float BOX_WIDTH = 7.0f; // perpendicular to direction

        // Calculate perpendicular direction (for width)
        float perp_x = -occ_dir_y; // 90 degree rotation
        float perp_y = occ_dir_x;

        // Calculate the 4 corners of the bounding box
        // Front-left, front-right, back-right, back-left
        float half_length = BOX_LENGTH * 0.5f;
        float half_width = BOX_WIDTH * 0.5f;

        // Front-left corner
        float corner1_x = occ_x + occ_dir_x * half_length + perp_x * half_width;
        float corner1_y = occ_y + occ_dir_y * half_length + perp_y * half_width;

        // Front-right corner
        float corner2_x = occ_x + occ_dir_x * half_length - perp_x * half_width;
        float corner2_y = occ_y + occ_dir_y * half_length - perp_y * half_width;

        // Back-right corner
        float corner3_x = occ_x - occ_dir_x * half_length - perp_x * half_width;
        float corner3_y = occ_y - occ_dir_y * half_length - perp_y * half_width;

        // Back-left corner
        float corner4_x = occ_x - occ_dir_x * half_length + perp_x * half_width;
        float corner4_y = occ_y - occ_dir_y * half_length + perp_y * half_width;

        // Check intersection with each of the 4 edges of the bounding box
        // Edge 1: corner1 to corner2 (front edge)
        if (lineSegmentIntersection(
                line_x1, line_y1, line_x2, line_y2, corner1_x, corner1_y, corner2_x, corner2_y)) {
            return true;
        }

        // Edge 2: corner2 to corner3 (right edge)
        if (lineSegmentIntersection(
                line_x1, line_y1, line_x2, line_y2, corner2_x, corner2_y, corner3_x, corner3_y)) {
            return true;
        }

        // Edge 3: corner3 to corner4 (back edge)
        if (lineSegmentIntersection(
                line_x1, line_y1, line_x2, line_y2, corner3_x, corner3_y, corner4_x, corner4_y)) {
            return true;
        }

        // Edge 4: corner4 to corner1 (left edge)
        if (lineSegmentIntersection(
                line_x1, line_y1, line_x2, line_y2, corner4_x, corner4_y, corner1_x, corner1_y)) {
            return true;
        }

        return false;
    }

    // Check if two line segments intersect
    private final boolean lineSegmentIntersection(
            float x1,
            float y1,
            float x2,
            float y2, // Line segment 1
            float x3,
            float y3,
            float x4,
            float y4) { // Line segment 2

        // Calculate direction vectors
        float dx1 = x2 - x1;
        float dy1 = y2 - y1;
        float dx2 = x4 - x3;
        float dy2 = y4 - y3;

        // Calculate denominator for intersection test
        float denominator = dx1 * dy2 - dy1 * dx2;

        // Lines are parallel if denominator is close to zero
        if (StrictMath.abs(denominator) < 0.0001f) {
            return false;
        }

        // Calculate parameters for intersection point
        float t1 = ((x3 - x1) * dy2 - (y3 - y1) * dx2) / denominator;
        float t2 = ((x3 - x1) * dy1 - (y3 - y1) * dx1) / denominator;

        // Check if intersection point is within both line segments
        return t1 >= 0.0f && t1 <= 1.0f && t2 >= 0.0f && t2 <= 1.0f;
    }

    private final boolean isDestinationReached() {
        float boat_x = boat.getPositionX();
        float boat_y = boat.getPositionY();
        float target_x = target.getPositionX();
        float target_y = target.getPositionY();

        float dx = target_x - boat_x;
        float dy = target_y - boat_y;
        float distance = (float) StrictMath.sqrt(dx * dx + dy * dy);

        return distance <= DESTINATION_THRESHOLD;
    }

    public final int animate(float t) {
        boat.setLayer(UnitGrid.SEA);

        // Check if destination is reached
        if (isDestinationReached()) {
            boat.endTrip();
            return Selectable.DONE;
        }

        UnitGrid grid = boat.getUnitGrid();
        boolean dockable = grid.isDockable(boat.getGridX(), boat.getGridY());
        boolean water = grid.isWater(boat.getGridX(), boat.getGridY());
        if (!left_shore && !dockable && water) {
            left_shore = true;
        }

        // Calculate direction to target
        float boat_x = boat.getPositionX();
        float boat_y = boat.getPositionY();
        float target_x = target.getPositionX();
        float target_y = target.getPositionY();

        float dx = target_x - boat_x;
        float dy = target_y - boat_y;
        float distance = (float) StrictMath.sqrt(dx * dx + dy * dy);

        if (distance < 0.001f || curveT >= 1.0f) {
            // Already at destination
            boat.endTrip();
            return Selectable.DONE;
        }

        // Normalize direction
        float dir_x = dx / distance;
        float dir_y = dy / distance;

        // Check for collision ahead
        int collision =
                checkCollisionAhead(
                        direction * boat.getDirectionX(),
                        direction * boat.getDirectionY(),
                        left_shore);
        if (collision == RESOLVABLE_COLLISION) {
            blocked = true;
            return Selectable.INTERRUPTIBLE;
        } else if (collision == UNRESOLVABLE_COLLISION) {
            boat.endTrip();
            return Selectable.INTERRUPTIBLE;
        }

        curveT += curveDt * BOAT_SPEED;

        Vector4f new_pose = calcCurve(curveT);

        boat.free();
        boat.setPosition(new_pose.x, new_pose.y);
        boat.setGridPosition(
                UnitGrid.toGridCoordinate(new_pose.x), UnitGrid.toGridCoordinate(new_pose.y));
        boat.setDirection(direction * new_pose.z, direction * new_pose.w);
        boat.occupy();

        return Selectable.UNINTERRUPTIBLE;
    }

    public final void forceInterrupted() {}
}
