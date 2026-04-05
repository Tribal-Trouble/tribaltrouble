package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.StaticOccupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Vector2f;
import com.oddlabs.util.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;

public final strictfp class SailBehaviour implements Behaviour {
    private static final float SHIP_SPEED = 0.2f;
    private static final float DESTINATION_THRESHOLD = 2.0f;
    private static final int NO_COLLISION = 0;
    private static final int RESOLVABLE_COLLISION = 1;
    private static final int UNRESOLVABLE_COLLISION = 2;

    private final Ship ship;
    private final Target target;

    private final Vector2f p0 = new Vector2f();
    private final Vector2f p1 = new Vector2f();
    private final Vector2f p2 = new Vector2f();
    private int trajectorySegmentIndex = 0;
    private float trajectorySegmentDistance = 0.0f;

    private final ArrayList brokenDownPath;
    private final ArrayList trajectory;

    private boolean blocked = false;
    private boolean left_shore = false;

    class TrajectoryPoint {
        public int gridX;
        public int gridY;
        public float positionX;
        public float positionY;
        public float directionX;
        public float directionY;

        public TrajectoryPoint() {}

        public TrajectoryPoint(int x, int y) {
            gridX = x;
            gridY = y;
            positionX = UnitGrid.coordinateFromGrid(gridX);
            positionY = UnitGrid.coordinateFromGrid(gridY);
            directionX = 0.0f;
            directionY = 1.0f;
        }

        public TrajectoryPoint(Target t) {
            gridX = t.getGridX();
            gridY = t.getGridY();
            positionX = t.getPositionX();
            positionY = t.getPositionY();

            if (t instanceof Selectable) {
                Selectable selectable = (Selectable) t;
                directionX = selectable.getDirectionX();
                directionY = selectable.getDirectionY();
            } else {
                directionX = 0.0f;
                directionY = 1.0f;
            }
        }

        public TrajectoryPoint(Ship s) {
            gridX = s.getGridX();
            gridY = s.getGridY();
            positionX = s.getPositionX();
            positionY = s.getPositionY();
            directionX = s.getDirectionX();
            directionY = s.getDirectionY();
        }

        public TrajectoryPoint moved(int distance) {
            TrajectoryPoint moved = new TrajectoryPoint();
            moved.gridX = gridX + (int) StrictMath.round(directionX * distance);
            moved.gridY = gridY + (int) StrictMath.round(directionY * distance);
            moved.positionX = UnitGrid.coordinateFromGrid(moved.gridX);
            moved.positionY = UnitGrid.coordinateFromGrid(moved.gridY);
            moved.directionX = directionX;
            moved.directionY = directionY;
            return moved;
        }

        public void move(int distance) {
            TrajectoryPoint moved = moved(distance);
            gridX = moved.gridX;
            gridY = moved.gridY;
            positionX = moved.positionX;
            positionY = moved.positionY;
            directionX = moved.directionX;
            directionY = moved.directionY;
        }

        public TrajectoryPoint rotated(float angle) {
            TrajectoryPoint rotated = new TrajectoryPoint();
            rotated.gridX = gridX;
            rotated.gridY = gridY;
            rotated.positionX = positionX;
            rotated.positionY = positionY;

            float radians = (float) StrictMath.toRadians(angle);
            float sin = (float) StrictMath.sin(radians);
            float cos = (float) StrictMath.cos(radians);

            rotated.directionX = directionX * cos - directionY * sin;
            rotated.directionY = directionX * sin + directionY * cos;

            float len =
                    (float)
                            StrictMath.sqrt(
                                    rotated.directionX * rotated.directionX
                                            + rotated.directionY * rotated.directionY);
            if (len > 0.0001f) {
                rotated.directionX /= len;
                rotated.directionY /= len;
            } else {
                rotated.directionX = directionX;
                rotated.directionY = directionY;
            }

            return rotated;
        }

        public void rotate(float angle) {
            TrajectoryPoint rotated = rotated(angle);
            gridX = rotated.gridX;
            gridY = rotated.gridY;
            positionX = rotated.positionX;
            positionY = rotated.positionY;
            directionX = rotated.directionX;
            directionY = rotated.directionY;
        }

        public float gridDistanceTo(TrajectoryPoint p) {
            int dx = p.gridX - gridX;
            int dy = p.gridY - gridY;
            return (float) StrictMath.sqrt(dx * dx + dy * dy);
        }

        public float distanceTo(TrajectoryPoint p) {
            float dx = p.positionX - positionX;
            float dy = p.positionY - positionY;
            return (float) StrictMath.sqrt(dx * dx + dy * dy);
        }
    }
    ;

    class TrajectorySegment {
        public TrajectoryPoint p0;
        public TrajectoryPoint p1;
        public boolean isStraight;
        public float radius;
        public float centerX;
        public float centerY;
        public float angle0;
        public float angle1;
        public float length;

        public TrajectorySegment(TrajectoryPoint p0, TrajectoryPoint p1) {
            this.p0 = p0;
            this.p1 = p1;
            this.isStraight = true;
            this.radius = 0.0f;
            this.centerX = 0.0f;
            this.centerY = 0.0f;
            this.angle0 = 0.0f;
            this.angle1 = 0.0f;
            this.length = p0.distanceTo(p1);
        }

        public TrajectorySegment(
                TrajectoryPoint p0,
                TrajectoryPoint p1,
                float radius,
                float centerX,
                float centerY,
                float angle0,
                float angle1) {
            this.p0 = p0;
            this.p1 = p1;
            this.isStraight = false;
            this.radius = radius;
            this.centerX = centerX;
            this.centerY = centerY;
            this.angle0 = angle0;
            this.angle1 = angle1;

            float delta = angle1 - angle0;
            while (delta > (float) StrictMath.PI) {
                delta -= (float) (2.0 * StrictMath.PI);
            }
            while (delta < (float) -StrictMath.PI) {
                delta += (float) (2.0 * StrictMath.PI);
            }
            this.length = StrictMath.abs(delta) * radius;
        }
    }
    ;

    public SailBehaviour(Ship ship, Target t, float range) {
        this.ship = ship;
        this.target = t;

        TrajectoryPoint p0 = new TrajectoryPoint(ship);
        TrajectoryPoint p1 = new TrajectoryPoint(t);

        float minDist = 10000.0f;
        TrajectoryPoint firstGap = null;
        for (int i = 0; i < 8; i++) {
            float angle = i * 45.0f;
            TrajectoryPoint gap = getNearestGap(p0, p0.rotated(angle).moved(1000), 10, 50, 6);
            float dist = gap.gridDistanceTo(p0);
            if (minDist > dist) {
                minDist = dist;
                firstGap = gap;
            }
        }

        minDist = 10000.0f;
        TrajectoryPoint lastGap = null;
        for (int i = 0; i < 8; i++) {
            float angle = i * 45.0f;
            TrajectoryPoint gap = getNearestGap(p1, p1.rotated(angle).moved(1000), 10, 50, 6);
            float dist = gap.gridDistanceTo(p1);
            if (minDist > dist) {
                minDist = dist;
                lastGap = gap;
            }
        }

        if (firstGap != null && lastGap != null) {
            brokenDownPath = breakDownPath(firstGap, lastGap, 0);
        } else {
            brokenDownPath = null;
        }

        if (brokenDownPath != null) {
            brokenDownPath.add(0, p0);
            brokenDownPath.add(p1);
            optimizePath(brokenDownPath);
            trajectory = createTrajectory(brokenDownPath);
        } else {
            trajectory = null;
        }
    }

    private Vector4f sampleTrajectoryPose(TrajectorySegment segment, float distance_on_segment) {
        if (segment.isStraight) {
            float sx = segment.p1.positionX - segment.p0.positionX;
            float sy = segment.p1.positionY - segment.p0.positionY;
            float length = segment.length;
            float t = 1.0f;
            if (length > 0.0001f) {
                t = distance_on_segment / length;
                t = StrictMath.max(0.0f, StrictMath.min(1.0f, t));
            }

            float x = segment.p0.positionX + sx * t;
            float y = segment.p0.positionY + sy * t;
            float dir_x = sx;
            float dir_y = sy;
            float dir_len = (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y);
            if (dir_len > 0.0001f) {
                dir_x /= dir_len;
                dir_y /= dir_len;
            } else {
                dir_x = segment.p0.directionX;
                dir_y = segment.p0.directionY;
            }
            return new Vector4f(x, y, dir_x, dir_y);
        }

        float delta = segment.angle1 - segment.angle0;
        while (delta > (float) StrictMath.PI) {
            delta -= (float) (2.0 * StrictMath.PI);
        }
        while (delta < (float) -StrictMath.PI) {
            delta += (float) (2.0 * StrictMath.PI);
        }

        float t = 1.0f;
        if (segment.length > 0.0001f) {
            t = distance_on_segment / segment.length;
            t = StrictMath.max(0.0f, StrictMath.min(1.0f, t));
        }

        float angle = segment.angle0 + delta * t;
        float x = segment.centerX + (float) StrictMath.cos(angle) * segment.radius;
        float y = segment.centerY + (float) StrictMath.sin(angle) * segment.radius;

        float dir_x;
        float dir_y;
        if (delta >= 0.0f) {
            dir_x = -(float) StrictMath.sin(angle);
            dir_y = (float) StrictMath.cos(angle);
        } else {
            dir_x = (float) StrictMath.sin(angle);
            dir_y = -(float) StrictMath.cos(angle);
        }

        float dir_len = (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y);
        if (dir_len > 0.0001f) {
            dir_x /= dir_len;
            dir_y /= dir_len;
        }

        return new Vector4f(x, y, dir_x, dir_y);
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

    private final ArrayList breakDownPath(TrajectoryPoint p0, TrajectoryPoint p1, int depth) {
        if (depth > 50) {
            return null;
        }

        // Avoid infinite recursion on tiny segments that cannot be split further on the grid.
        if (StrictMath.abs(p1.gridX - p0.gridX) <= 1 && StrictMath.abs(p1.gridY - p0.gridY) <= 1) {
            ArrayList result = new ArrayList();
            result.add(p0);
            result.add(p1);
            return result;
        }

        if (!checkCollisionOnLine(p0, p1, 6)) {
            ArrayList result = new ArrayList();
            result.add(p0);
            result.add(p1);
            return result;
        } else {
            // Midpoint of the segment and perpendicular probes toward each side of the grid.
            TrajectoryPoint pmid =
                    new TrajectoryPoint(
                            (int) StrictMath.round((p0.gridX + p1.gridX) * 0.5f),
                            (int) StrictMath.round((p0.gridY + p1.gridY) * 0.5f));

            float dir_x = p1.gridX - p0.gridX;
            float dir_y = p1.gridY - p0.gridY;
            float len = (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y);
            if (len < 0.001f) {
                ArrayList result = new ArrayList();
                result.add(p0);
                result.add(p1);
                return result;
            }

            dir_x /= len;
            dir_y /= len;
            float perp_x = -dir_y;
            float perp_y = dir_x;
            int grid_size = ship.getUnitGrid().getGridSize();
            int reach = grid_size;
            int pleftx =
                    (int)
                            StrictMath.max(
                                    0,
                                    StrictMath.min(
                                            grid_size - 1,
                                            StrictMath.round(pmid.gridX + perp_x * reach)));
            int plefty =
                    (int)
                            StrictMath.max(
                                    0,
                                    StrictMath.min(
                                            grid_size - 1,
                                            StrictMath.round(pmid.gridY + perp_y * reach)));
            int prightx =
                    (int)
                            StrictMath.max(
                                    0,
                                    StrictMath.min(
                                            grid_size - 1,
                                            StrictMath.round(pmid.gridX - perp_x * reach)));
            int prighty =
                    (int)
                            StrictMath.max(
                                    0,
                                    StrictMath.min(
                                            grid_size - 1,
                                            StrictMath.round(pmid.gridY - perp_y * reach)));

            TrajectoryPoint pleft = new TrajectoryPoint(pleftx, plefty);
            TrajectoryPoint pright = new TrajectoryPoint(prightx, prighty);
            TrajectoryPoint gap1 = getNearestGap(pmid, pleft, 20, 50, 6);
            TrajectoryPoint gap2 = getNearestGap(pmid, pright, 20, 50, 6);

            TrajectoryPoint closest;
            if (gap1 != null && gap2 != null) {
                float gap1dist2 = gap1.gridDistanceTo(pmid);
                float gap2dist2 = gap2.gridDistanceTo(pmid);
                if (gap1dist2 <= gap2dist2) {
                    closest = gap1;
                } else {
                    closest = gap2;
                }
            } else if (gap1 != null) {
                closest = gap1;
            } else if (gap2 != null) {
                closest = gap2;
            } else {
                return null;
            }

            // If the split point does not move us forward, avoid recursive loop and return
            // fallback.
            if ((closest.gridX == p0.gridX && closest.gridX == p0.gridY)
                    || (closest.gridX == p1.gridX && closest.gridY == p1.gridY)) {
                return null;
            }

            ArrayList firstHalf = breakDownPath(p0, closest, depth + 1);
            ArrayList secondHalf = breakDownPath(closest, p1, depth + 1);
            if (secondHalf != null && firstHalf != null) {
                ArrayList combined = new ArrayList();
                combined.addAll(firstHalf);
                combined.addAll(secondHalf);
                return combined;
            } else if (firstHalf != null) {
                return firstHalf;
            } else {
                return null;
            }
        }
    }

    private final ArrayList createTrajectory(ArrayList path) {
        ArrayList result = new ArrayList();
        if (path == null || path.size() < 2) {
            return result;
        }

        final float CLIP_LENGTH = 20.0f;
        final float EPSILON = 0.01f;

        int n = path.size();
        TrajectoryPoint[] arc_in = new TrajectoryPoint[n];
        TrajectoryPoint[] arc_out = new TrajectoryPoint[n];
        float[] arc_radius = new float[n];
        float[] arc_center_x = new float[n];
        float[] arc_center_y = new float[n];
        float[] arc_angle0 = new float[n];
        float[] arc_angle1 = new float[n];
        boolean[] has_arc = new boolean[n];

        // Build one arc candidate per interior corner by clipping a fixed length from each side.
        for (int i = 1; i < n - 1; i++) {
            TrajectoryPoint a = (TrajectoryPoint) path.get(i - 1);
            TrajectoryPoint b = (TrajectoryPoint) path.get(i);
            TrajectoryPoint c = (TrajectoryPoint) path.get(i + 1);

            float in_x = b.positionX - a.positionX;
            float in_y = b.positionY - a.positionY;
            float out_x = c.positionX - b.positionX;
            float out_y = c.positionY - b.positionY;
            float len_in = (float) StrictMath.sqrt(in_x * in_x + in_y * in_y);
            float len_out = (float) StrictMath.sqrt(out_x * out_x + out_y * out_y);

            float clip_length = CLIP_LENGTH;

            if (len_in < clip_length || len_out < clip_length) {
                clip_length = CLIP_LENGTH / 2.0f;
            }

            if (len_in < clip_length || len_out < clip_length) {
                continue;
            }

            if (len_in < EPSILON || len_out < EPSILON) {
                continue;
            }

            float dir_in_x = in_x / len_in;
            float dir_in_y = in_y / len_in;
            float dir_out_x = out_x / len_out;
            float dir_out_y = out_y / len_out;

            float dot = dir_in_x * dir_out_x + dir_in_y * dir_out_y;
            if (dot > 1.0f) {
                dot = 1.0f;
            } else if (dot < -1.0f) {
                dot = -1.0f;
            }

            float cross = dir_in_x * dir_out_y - dir_in_y * dir_out_x;
            if (StrictMath.abs(cross) < 0.0001f || dot > 0.9999f) {
                continue; // straight (or almost straight), no arc needed
            }

            float tin_x = b.positionX - dir_in_x * clip_length;
            float tin_y = b.positionY - dir_in_y * clip_length;
            float tout_x = b.positionX + dir_out_x * clip_length;
            float tout_y = b.positionY + dir_out_y * clip_length;

            // Intersect perpendicular lines from the clipped points to get the arc center.
            float n1_x = -dir_in_y;
            float n1_y = dir_in_x;
            float n2_x = -dir_out_y;
            float n2_y = dir_out_x;

            float denom = n1_x * n2_y - n1_y * n2_x;
            if (StrictMath.abs(denom) < 0.0001f) {
                continue;
            }

            float dx = tout_x - tin_x;
            float dy = tout_y - tin_y;
            float t_param = (dx * n2_y - dy * n2_x) / denom;
            float center_x = tin_x + n1_x * t_param;
            float center_y = tin_y + n1_y * t_param;

            float radius_dx = center_x - tin_x;
            float radius_dy = center_y - tin_y;
            float radius = (float) StrictMath.sqrt(radius_dx * radius_dx + radius_dy * radius_dy);
            if (radius < EPSILON) {
                continue;
            }

            TrajectoryPoint tin = makeTrajectoryPoint(tin_x, tin_y, dir_in_x, dir_in_y);
            TrajectoryPoint tout = makeTrajectoryPoint(tout_x, tout_y, dir_out_x, dir_out_y);

            has_arc[i] = true;
            arc_in[i] = tin;
            arc_out[i] = tout;
            arc_radius[i] = radius;
            arc_center_x[i] = center_x;
            arc_center_y[i] = center_y;
            arc_angle0[i] = (float) StrictMath.atan2(tin_y - center_y, tin_x - center_x);
            arc_angle1[i] = (float) StrictMath.atan2(tout_y - center_y, tout_x - center_x);
        }

        // Guard against over-clipping if both neighboring corners clip the same short segment.
        for (int i = 0; i < n - 1; i++) {
            if (!has_arc[i] || !has_arc[i + 1]) {
                continue;
            }
            TrajectoryPoint seg_p0 = (TrajectoryPoint) path.get(i);
            TrajectoryPoint seg_p1 = (TrajectoryPoint) path.get(i + 1);
            if (seg_p0.distanceTo(seg_p1) <= CLIP_LENGTH * 2.0f + EPSILON) {
                has_arc[i + 1] = false;
            }
        }

        // Emit alternating straight and arc segments.
        for (int i = 0; i < n - 1; i++) {
            TrajectoryPoint segment_start = has_arc[i] ? arc_out[i] : (TrajectoryPoint) path.get(i);
            TrajectoryPoint segment_end =
                    has_arc[i + 1] ? arc_in[i + 1] : (TrajectoryPoint) path.get(i + 1);

            if (segment_start.distanceTo(segment_end) > EPSILON) {
                result.add(makeStraightSegment(segment_start, segment_end));
            } else {
                TrajectoryPoint fallback_start = (TrajectoryPoint) path.get(i);
                TrajectoryPoint fallback_end = (TrajectoryPoint) path.get(i + 1);
                if (fallback_start.distanceTo(fallback_end) > EPSILON) {
                    result.add(makeStraightSegment(fallback_start, fallback_end));
                }
            }

            if (has_arc[i + 1]) {
                result.add(
                        makeArcSegment(
                                arc_in[i + 1],
                                arc_out[i + 1],
                                arc_radius[i + 1],
                                arc_center_x[i + 1],
                                arc_center_y[i + 1],
                                arc_angle0[i + 1],
                                arc_angle1[i + 1]));
            }
        }

        return result;
    }

    private TrajectoryPoint makeTrajectoryPoint(float x, float y, float dir_x, float dir_y) {
        TrajectoryPoint point = new TrajectoryPoint();
        point.positionX = x;
        point.positionY = y;
        point.gridX = UnitGrid.toGridCoordinate(x);
        point.gridY = UnitGrid.toGridCoordinate(y);
        float len = (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y);
        if (len > 0.0001f) {
            point.directionX = dir_x / len;
            point.directionY = dir_y / len;
        } else {
            point.directionX = 0.0f;
            point.directionY = 1.0f;
        }
        return point;
    }

    private TrajectorySegment makeStraightSegment(TrajectoryPoint p0, TrajectoryPoint p1) {
        return new TrajectorySegment(p0, p1);
    }

    private TrajectorySegment makeArcSegment(
            TrajectoryPoint p0,
            TrajectoryPoint p1,
            float radius,
            float center_x,
            float center_y,
            float angle0,
            float angle1) {
        return new TrajectorySegment(p0, p1, radius, center_x, center_y, angle0, angle1);
    }

    private final void optimizePath(ArrayList path) {
        if (path == null || path.size() < 3) {
            return;
        }

        boolean changed = true;
        while (changed && path.size() >= 3) {
            changed = false;
            int i = 1;
            while (i < path.size() - 1) {
                TrajectoryPoint prev = (TrajectoryPoint) path.get(i - 1);
                TrajectoryPoint next = (TrajectoryPoint) path.get(i + 1);

                // If prev and next are directly navigable, the middle point is redundant.
                if (!checkCollisionOnLine(prev, next, 6)) {
                    path.remove(i);
                    changed = true;
                    // Stay on same index to test newly adjacent points as well.
                } else {
                    i++;
                }
            }
        }
    }

    private final TrajectoryPoint getNearestGap(
            TrajectoryPoint p0, TrajectoryPoint p1, int minSize, int maxSize, int thickness) {
        UnitGrid grid = ship.getUnitGrid();
        int grid_size = grid.getGridSize();

        int dx = p1.gridX - p0.gridX;
        int dy = p1.gridY - p0.gridY;
        float line_len = (float) StrictMath.sqrt(dx * dx + dy * dy);

        float perp_x;
        float perp_y;
        if (line_len < 0.001f) {
            perp_x = 1.0f;
            perp_y = 0.0f;
        } else {
            float dir_x = dx / line_len;
            float dir_y = dy / line_len;
            perp_x = -dir_y;
            perp_y = dir_x;
        }

        int current_x = p0.gridX;
        int current_y = p0.gridY;
        int step_x = p0.gridX < p1.gridX ? 1 : -1;
        int step_y = p0.gridY < p1.gridY ? 1 : -1;
        int abs_dx = StrictMath.abs(dx);
        int abs_dy = StrictMath.abs(dy);
        int err = abs_dx - abs_dy;

        int run_length = 0;
        int run_start_x = p0.gridX;
        int run_start_y = p0.gridY;

        TrajectoryPoint result = null;

        while (true) {
            boolean is_water_strip = true;
            float half_span = (thickness - 1) * 0.5f;
            for (int i = 0; i < thickness; i++) {
                float offset = i - half_span;
                int check_x = (int) StrictMath.round(current_x + perp_x * offset);
                int check_y = (int) StrictMath.round(current_y + perp_y * offset);
                if (check_x < 0
                        || check_x >= grid_size
                        || check_y < 0
                        || check_y >= grid_size
                        || !grid.isWater(check_x, check_y)
                        || grid.isDockable(check_x, check_y)) {
                    is_water_strip = false;
                    break;
                }
            }

            if (is_water_strip) {
                if (run_length == 0) {
                    run_start_x = current_x;
                    run_start_y = current_y;
                }
                run_length++;
                if (run_length >= minSize) {
                    result =
                            new TrajectoryPoint(
                                    (int) StrictMath.round((run_start_x + current_x) * 0.5f),
                                    (int) StrictMath.round((run_start_y + current_y) * 0.5f));
                    if (run_length >= maxSize) {
                        return result;
                    }
                }
            } else {
                run_length = 0;
            }

            if (current_x == p1.gridX && current_y == p1.gridY) {
                break;
            }

            int e2 = err * 2;
            if (e2 > -abs_dy) {
                err -= abs_dy;
                current_x += step_x;
            }
            if (e2 < abs_dx) {
                err += abs_dx;
                current_y += step_y;
            }
        }

        return result;
    }

    private final boolean checkCollisionOnLine(
            TrajectoryPoint p0, TrajectoryPoint p1, int thickness) {
        UnitGrid grid = ship.getUnitGrid();
        int grid_size = grid.getGridSize();
        final int half_thickness = thickness / 2;

        float dir_x = p1.gridX - p0.gridX;
        float dir_y = p1.gridY - p0.gridY;
        float length = (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y);

        // Degenerate line: treat it as a small disk with radius 2.
        if (length < 0.001f) {
            for (int ox = -half_thickness; ox <= half_thickness; ox++) {
                for (int oy = -half_thickness; oy <= half_thickness; oy++) {
                    if (ox * ox + oy * oy > half_thickness * half_thickness) {
                        continue;
                    }
                    int check_x = p0.gridX + ox;
                    int check_y = p0.gridY + oy;
                    if (check_x < 0
                            || check_x >= grid_size
                            || check_y < 0
                            || check_y >= grid_size
                            || !grid.isWater(check_x, check_y)) {
                        return true;
                    }
                }
            }
            return false;
        }

        dir_x /= length;
        dir_y /= length;
        float perp_x = -dir_y;
        float perp_y = dir_x;

        // Sample densely along the segment so all grid cells of the thick line are covered.
        for (float distance = 0.0f; distance <= length; distance += 0.5f) {
            float center_x = p0.gridX + dir_x * distance;
            float center_y = p0.gridY + dir_y * distance;
            for (int offset = -half_thickness; offset <= half_thickness; offset++) {
                int check_x = (int) StrictMath.round(center_x + perp_x * offset);
                int check_y = (int) StrictMath.round(center_y + perp_y * offset);
                if (check_x < 0
                        || check_x >= grid_size
                        || check_y < 0
                        || check_y >= grid_size
                        || !grid.isWater(check_x, check_y)) {
                    return true;
                }
            }
        }

        // Ensure the exact endpoint strip is checked even with floating-point accumulation.
        for (int offset = -half_thickness; offset <= half_thickness; offset++) {
            int check_x = (int) StrictMath.round(p1.gridX + perp_x * offset);
            int check_y = (int) StrictMath.round(p1.gridY + perp_y * offset);
            if (check_x < 0
                    || check_x >= grid_size
                    || check_y < 0
                    || check_y >= grid_size
                    || !grid.isWater(check_x, check_y)) {
                return true;
            }
        }

        return false;
    }

    private final int checkCollisionAhead(float dir_x, float dir_y) {
        UnitGrid grid = ship.getUnitGrid();
        int ship_grid_x = ship.getGridX();
        int ship_grid_y = ship.getGridY();
        float ship_x = ship.getPositionX();
        float ship_y = ship.getPositionY();
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
                        ship_grid_x + (int) StrictMath.round(forward_offset_x + perp_offset_x);
                int check_grid_y =
                        ship_grid_y + (int) StrictMath.round(forward_offset_y + perp_offset_y);
                if (check_grid_x >= 0
                        && check_grid_x < grid.getGridSize()
                        && check_grid_y >= 0
                        && check_grid_y < grid.getGridSize()) {
                    Occupant object = grid.getOccupant(check_grid_x, check_grid_y, UnitGrid.SEA);
                    if (object != null
                            && object instanceof Selectable
                            && !(object instanceof StaticOccupant)) {
                        Selectable selectable = (Selectable) object;
                        if (selectable != null && selectable != ship) {
                            obstacleSet.add(selectable);
                        }
                    }

                    object = grid.getOccupant(check_grid_x, check_grid_y, UnitGrid.LAND);
                    if (object != null
                            && object instanceof Selectable
                            && !(object instanceof StaticOccupant)) {
                        Selectable selectable = (Selectable) object;
                        if (selectable != null && selectable != ship) {
                            obstacleSet.add(selectable);
                        }
                    }
                }
            }
        }

        /*   int check_grid_x = (int) StrictMath.round(ship_grid_x + dir_x * 2);
        int check_grid_y = (int) StrictMath.round(ship_grid_y + dir_y * 2);

        Region regionSea = grid.getRegion(check_grid_x, check_grid_y, UnitGrid.SEA);
        Region regionLand = grid.getRegion(check_grid_x, check_grid_y, UnitGrid.LAND);
        if (regionSea == null && regionLand == null) {
            System.out.println("Regionless area I'm afraid");
            return UNRESOLVABLE_COLLISION;
        }*/

        Object[] objects = obstacleSet.toArray();
        for (int i = 0; i < objects.length; i++) {
            if (checkLineIntersectionWithOccupant(
                    ship_x, ship_y, target_x, target_y, (Selectable) objects[i])) {
                return RESOLVABLE_COLLISION;
            }
        }

        return NO_COLLISION;
    }

    // Check if ship's path line intersects with occupant's bounding box (30m x 7m)
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
        float ship_x = ship.getPositionX();
        float ship_y = ship.getPositionY();
        float target_x = target.getPositionX();
        float target_y = target.getPositionY();

        float dx = target_x - ship_x;
        float dy = target_y - ship_y;
        float distance = (float) StrictMath.sqrt(dx * dx + dy * dy);

        return distance <= DESTINATION_THRESHOLD;
    }

    public final int animate(float t) {
        if (ship.isDead()) {
            return Selectable.DONE;
        }

        ship.setLayer(UnitGrid.SEA);

        // Check if destination is reached
        if (isDestinationReached()) {
            ship.endTrip();
            return Selectable.DONE;
        }

        UnitGrid grid = ship.getUnitGrid();
        TrajectoryPoint shipPoint = new TrajectoryPoint(ship);
        shipPoint.move(-20);
        TrajectoryPoint aheadPoint = shipPoint.moved(40);
        boolean inTheClear = !checkCollisionOnLine(shipPoint, aheadPoint, 6);
        if (!left_shore) {
            if (!checkCollisionOnLine(shipPoint, aheadPoint, 6)) {
                left_shore = true;
            }
        } else if (left_shore) {
            shipPoint = new TrajectoryPoint(ship);
            if (checkCollisionOnLine(shipPoint.moved(-2), shipPoint.moved(2), 2)) {
                ship.endTrip();
                return Selectable.INTERRUPTIBLE;
            }
        }

        if (trajectory == null || trajectory.size() == 0) {
            ship.endTrip();
            return Selectable.INTERRUPTIBLE;
        }

        // Check for collision ahead based on current heading.
        int collision = checkCollisionAhead(ship.getDirectionX(), ship.getDirectionY());
        if (collision == RESOLVABLE_COLLISION) {
            blocked = true;
            return Selectable.INTERRUPTIBLE;
        } else if (collision == UNRESOLVABLE_COLLISION) {
            ship.endTrip();
            return Selectable.INTERRUPTIBLE;
        }

        int rowers = ship.getShipHR().countRowers();
        if (rowers == 0) {
            ship.endTrip();
            return Selectable.INTERRUPTIBLE;
        }
        float speed = rowers * SHIP_SPEED;
        float remaining_distance = speed * t;

        while (remaining_distance > 0.0f && trajectorySegmentIndex < trajectory.size()) {
            TrajectorySegment segment = (TrajectorySegment) trajectory.get(trajectorySegmentIndex);
            float segment_remaining = segment.length - trajectorySegmentDistance;
            if (segment_remaining <= 0.0001f) {
                trajectorySegmentIndex++;
                trajectorySegmentDistance = 0.0f;
                continue;
            }

            float advance = StrictMath.min(remaining_distance, segment_remaining);
            trajectorySegmentDistance += advance;
            remaining_distance -= advance;

            if (trajectorySegmentDistance >= segment.length - 0.0001f
                    && remaining_distance > 0.0f) {
                trajectorySegmentIndex++;
                trajectorySegmentDistance = 0.0f;
            }
        }

        if (trajectorySegmentIndex >= trajectory.size()) {
            ship.endTrip();
            return Selectable.DONE;
        }

        TrajectorySegment current_segment =
                (TrajectorySegment) trajectory.get(trajectorySegmentIndex);
        Vector4f new_pose = sampleTrajectoryPose(current_segment, trajectorySegmentDistance);

        ship.free();
        ship.setPosition(new_pose.x, new_pose.y);
        ship.setGridPosition(
                UnitGrid.toGridCoordinate(new_pose.x), UnitGrid.toGridCoordinate(new_pose.y));
        ship.setDirection(new_pose.z, new_pose.w);
        ship.occupy();

        return Selectable.UNINTERRUPTIBLE;
    }

    public final void forceInterrupted() {}
}
