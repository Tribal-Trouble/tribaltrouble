package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.util.DebugRender;
import com.oddlabs.tt.util.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public final class ShipTrajectory {

    private final Ship ship;
    private final UnitGrid grid;
    private final boolean DEBUG = false;

    private final List<ShipTrajectorySegment> trajectory;

    private boolean isComplete = true;

    private int currentSegmentIndex = 0;
    private float segmentProgress = 0.0f;
    private float totalProgress = 0.0f;

    public ShipTrajectory(Ship ship, Target t) {
        this.ship = ship;

        grid = ship.getUnitGrid();

        ShipTrajectoryPoint p0 = new ShipTrajectoryPoint(ship);
        ShipTrajectoryPoint p1 = p0.moved(10);

        boolean waterTarget = grid.isDeepWater(t.getGridX(), t.getGridY());
        boolean waterStart = grid.isDeepWater(ship.getGridX(), ship.getGridY());

        ShipTrajectoryPoint p2 = null;

        if (waterTarget) {
            p2 = new ShipTrajectoryPoint(t);
        } else {
            p2 = pickTargetPosition(grid, ship, t);
        }

        var regionPath = findRegionPath(p1, p2);

        if (regionPath != null) {
            optimizePath(regionPath);
            regionPath.add(0, p0);
            trajectory = createTrajectory(regionPath);
        } else {
            trajectory = null;
        }
    }

    private List<ShipTrajectoryPoint> findRegionPath(ShipTrajectoryPoint from, ShipTrajectoryPoint to) {
        List<ShipTrajectoryPoint> points = new ArrayList<>();
        if (from == null || to == null) {
            return null;
        }

        Region src_region = findRegion(from.gridX, from.gridY);
        Region dst_region = findRegion(to.gridX, to.gridY);
        if (src_region == null || dst_region == null) {
            return null;
        }

        Region path_end = PathFinder.findPathRegion(grid, src_region, dst_region);
        if (path_end == null) {
            return null;
        }

        RegionNode node = (RegionNode) path_end.newPath();
        points.add(from);
        while (node != null) {
            ShipTrajectoryPoint pt = new ShipTrajectoryPoint(
                    node.getRegion().getGridX(),
                    node.getRegion().getGridY());
            points.add(pt);
            node = (RegionNode) node.getParent();
        }
        points.add(to);

        return points;
    }

    private Region findRegion(int grid_x, int grid_y) {
        return grid.getRegion(grid_x, grid_y, UnitGrid.SEA);
    }

    public void debugRender(HeightMap heightmap) {
        if (trajectory == null) {
            return;
        }
        final float OFFSET = 0.1f;
        float z = heightmap.getSeaLevelMeters() + OFFSET;
        for (ShipTrajectorySegment segment : trajectory) {
            if (segment.isStraight) {
                DebugRender.drawLine(
                        segment.p0.positionX, segment.p0.positionY, z,
                        segment.p1.positionX, segment.p1.positionY, z,
                        0.0f, 1.0f, 0.0f);
            } else {
                drawArc(segment, z);
            }
        }
    }

    private void drawArc(ShipTrajectorySegment segment, float z) {
        float prevX = segment.p0.positionX;
        float prevY = segment.p0.positionY;
        for (int i = 1; i <= 16; i++) {
            float percent = (float) i / 16;
            ShipTrajectoryPoint pt = segment.center.clone();
            pt.setDirectionTo(segment.p0);
            float deltaAngle = (float) StrictMath.toDegrees(segment.length * percent / segment.radius);
            pt.rotate(deltaAngle * segment.angle_sign);
            pt.move(segment.radius);
            DebugRender.drawLine(prevX, prevY, z, pt.positionX, pt.positionY, z, 0.0f, 1.0f, 0.0f);
            prevX = pt.positionX;
            prevY = pt.positionY;
        }
    }

    public boolean exists() {
        return trajectory != null && trajectory.size() > 0;
    }

    public boolean isComplete() {
        return isComplete;
    }

    private ShipTrajectorySegment get(int index) {
        return trajectory.get(index);
    }

    public ShipTrajectoryPoint advance(float distance) {
        if (distance <= 0.001f) {
            return new ShipTrajectoryPoint(ship);
        }

        if (currentSegmentIndex >= trajectory.size()) {
            return null;
        }

        ShipTrajectoryPoint pt = new ShipTrajectoryPoint();
        while (distance > 0.0f && currentSegmentIndex < trajectory.size()) {
            distance = trajectory.get(currentSegmentIndex).advance(distance, pt);
            if (distance > 0.0f) {
                currentSegmentIndex++;
            }
        }
        return pt;
    }

    public boolean reachedGoal() {
        if (currentSegmentIndex >= trajectory.size()) {
            return true;
        }
        return false;
    }

    public boolean almostReachedGoal() {
        if (currentSegmentIndex >= trajectory.size()) {
            return true;
        }
        float d = trajectory.get(trajectory.size() - 1).p1.gridDistanceTo(new ShipTrajectoryPoint(ship));
        if (d <= 4) {
            return true;
        }
        return false;
    }

    private final List<ShipTrajectorySegment> createTrajectory(List<ShipTrajectoryPoint> path) {
        List result = new ArrayList<ShipTrajectorySegment>();
        if (path == null || path.size() < 2) {
            return result;
        }

        int n = path.size();

        ShipTrajectoryPoint prev = path.get(0);

        for (int i = 1; i < n - 1; i++) {
            ShipTrajectoryPoint a = path.get(i - 1);
            ShipTrajectoryPoint b = path.get(i);
            ShipTrajectoryPoint c = path.get(i + 1);

            float clip0 = a.distanceTo(b) * 0.5f;
            float clip1 = b.distanceTo(c) * 0.5f;
            float clip = (float) StrictMath.min(clip0, clip1);
            clip = (float) StrictMath.min(clip, 20.0f);

            ShipTrajectoryPoint b_a = b.clone();
            b_a.setDirectionTo(a);
            b_a.move(clip);

            ShipTrajectoryPoint b_c = b.clone();
            b_c.setDirectionTo(c);
            b_c.move(clip);

            ShipTrajectoryPoint p0 = prev.clone();
            p0.setDirectionTo(b);
            ShipTrajectoryPoint p1 = b_a.clone();
            p1.copyDirection(p0);
            result.add(makeStraightSegment(p0, p1));

            ShipTrajectoryPoint center = b_a.rotated(90).intersection(b_c.rotated(90));
            if (center != null) {
                float radius = center.distanceTo(b_a);
                center.setDirectionTo(b_c);
                center.rotate(-90.0f);
                b_c.copyDirection(center);
                b_a.setDirectionTo(b);
                result.add(makeArcSegment(b_a, b_c, radius, center));
                prev = b_c;
            } else {
                // If there's no intersection, that's not a realistic turn the ship
                // could make. So we'll assume the path is incomplete and stop here.
                isComplete = false;
                return result;
            }
        }

        ShipTrajectoryPoint p1 = path.get(n - 1);
        prev.setDirectionTo(p1);
        p1.copyDirection(prev);
        result.add(makeStraightSegment(prev, p1));

        return result;
    }

    private ShipTrajectorySegment makeStraightSegment(ShipTrajectoryPoint p0, ShipTrajectoryPoint p1) {
        return new ShipTrajectorySegment(p0, p1);
    }

    private ShipTrajectorySegment makeArcSegment(
            ShipTrajectoryPoint p0,
            ShipTrajectoryPoint p1,
            float radius,
            ShipTrajectoryPoint center) {
        return new ShipTrajectorySegment(p0, p1, radius, center);
    }

    private final void optimizePath(List<ShipTrajectoryPoint> path) {
        if (path == null || path.size() < 3) {
            return;
        }

        boolean changed = true;
        while (changed && path.size() >= 3) {
            changed = false;
            int i = 1;
            while (i < path.size() - 1) {
                ShipTrajectoryPoint prev = path.get(i - 1);
                ShipTrajectoryPoint next = path.get(i + 1);

                if (!checkLandCollision(grid, prev, next)) {
                    path.remove(i);
                    changed = true;
                } else {
                    i++;
                }
            }
        }
    }

    static class DeepWaterFinder implements ScanFilter {
        private final UnitGrid grid;
        private ShipTrajectoryPoint pt = null;

        public DeepWaterFinder(UnitGrid grid) {
            this.grid = grid;
        }

        public int getMinRadius() {
            return 0;
        }

        public int getMaxRadius() {
            return grid.getGridSize();
        }

        public boolean filter(int grid_x, int grid_y, Occupant occ) {
            if (grid.isDeepWater(grid_x, grid_y)) {
                pt = new ShipTrajectoryPoint(grid_x, grid_y);
                return true;
            }
            return false;
        }

        public ShipTrajectoryPoint result() {
            return pt;
        }
    }

    public static List<ShipTrajectoryPoint> pickTargetArray(UnitGrid grid, Target target, int numTargets) {
        List<ShipTrajectoryPoint> targets = new ArrayList<ShipTrajectoryPoint>();
        if (numTargets <= 0) {
            return targets;
        }

        ShipTrajectoryPoint midPt = pickTargetPosition(grid, null, target);
        if (midPt == null) {
            return targets;
        }
        targets.add(midPt);

        int numLeft = (numTargets - 1) / 2;
        int numRight = numTargets - numLeft - 1;
        for (int i = 1; i <= numLeft; i++) {
            ShipTrajectoryPoint pt = midPt.moved(10 * i);
            targets.add(pt);
        }
        for (int i = 1; i <= numRight; i++) {
            ShipTrajectoryPoint pt = midPt.moved(10 * i);
            targets.add(pt);
        }
        return targets;
    }

    public static ShipTrajectoryPoint pickTargetPosition(UnitGrid grid, Occupant self, Target target) {
        DeepWaterFinder finder = new DeepWaterFinder(grid);
        grid.scan(finder, target.getGridX(), target.getGridY(), UnitGrid.SEA);
        return finder.result();
    }

    public static boolean checkLandCollision(UnitGrid grid, ShipTrajectoryPoint p0, ShipTrajectoryPoint p1) {
        if (p0.gridX == p1.gridX && p0.gridY == p1.gridY) {
            return !grid.isDeepWater(p0.gridX, p0.gridY);
        }
        int dx = Math.abs(p1.gridX - p0.gridX);
        int dy = Math.abs(p1.gridY - p0.gridY);
        float total = p0.distanceTo(p1);
        float step = total / Math.max(dx, dy);
        int s = grid.getGridSize();
        int n = (int) StrictMath.round(total / step);
        var origin = p0.clone();
        origin.setDirectionTo(p1);
        for (int i = 0; i <= n; i++) {
            ShipTrajectoryPoint sample = origin.moved(i * step);
            if (sample.gridX < 0 || sample.gridX >= s || sample.gridY < 0 || sample.gridY >= s) {
                return true;
            }
            if (!grid.isDeepWater(sample.gridX, sample.gridY)) {
                return true;
            }
        }
        return false;
    }

    public static boolean polygonCollision(ShipTrajectoryPoint[] poly0, ShipTrajectoryPoint[] poly1) {
        for (int i = 0; i < poly0.length; i++) {
            var p0_a = poly0[i];
            var p0_b = poly0[(i + 1) % poly0.length];
            var min_x0 = Math.min(p0_a.positionX, p0_b.positionX);
            var max_x0 = Math.max(p0_a.positionX, p0_b.positionX);
            var min_y0 = Math.min(p0_a.positionY, p0_b.positionY);
            var max_y0 = Math.max(p0_a.positionY, p0_b.positionY);
            for (int j = 0; j < poly1.length; j++) {
                var p1_a = poly1[j];
                var p1_b = poly1[(j + 1) % poly1.length];
                var min_x1 = Math.min(p1_a.positionX, p1_b.positionX);
                var max_x1 = Math.max(p1_a.positionX, p1_b.positionX);
                var min_y1 = Math.min(p1_a.positionY, p1_b.positionY);
                var max_y1 = Math.max(p1_a.positionY, p1_b.positionY);
                var inter = p0_a.intersection(p1_a);
                if (inter != null) {
                    if (inter.positionX >= min_x0 && inter.positionX <= max_x0
                            && inter.positionY >= min_y0 && inter.positionY <= max_y0
                            && inter.positionX >= min_x1 && inter.positionX <= max_x1
                            && inter.positionY >= min_y1 && inter.positionY <= max_y1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ShipTrajectoryPoint[] shipToPolygon(Ship s) {
        ShipTrajectoryPoint[] poly = new ShipTrajectoryPoint[4];

        ShipTrajectoryPoint center = new ShipTrajectoryPoint(s);
        poly[0] = center.moved(-7).rotated(90).moved(3);
        poly[1] = poly[0].moved(-6);
        poly[2] = center.moved(7).rotated(-90).moved(3);
        poly[3] = poly[2].moved(-6);

        poly[0].setDirectionTo(poly[1]);
        poly[1].setDirectionTo(poly[2]);
        poly[2].setDirectionTo(poly[3]);
        poly[3].setDirectionTo(poly[0]);

        return poly;
    }

    static class ShipFinder implements ScanFilter {
        private final int radius;
        private Set<Ship> ships = new HashSet<>();
        private Ship self;

        public ShipFinder(int radius, Ship self) {
            this.radius = radius;
            this.self = self;
        }

        public int getMinRadius() {
            return 0;
        }

        public int getMaxRadius() {
            return radius;
        }

        public boolean filter(int grid_x, int grid_y, Occupant occ) {
            if (occ instanceof Ship s && s != self) {
                ships.add(s);
            }
            return false;
        }

        public Set<Ship> results() {
            return ships;
        }
    }

    public static boolean checkShipsCollision(UnitGrid grid, Ship ship, ShipTrajectoryPoint p0,
            ShipTrajectoryPoint p1) {
        ShipTrajectoryPoint[] poly = new ShipTrajectoryPoint[4];
        ShipTrajectoryPoint center0 = p0.clone();
        center0.setDirectionTo(p1);
        poly[0] = center0.rotated(90).moved(3);
        poly[1] = poly[0].moved(-6);
        ShipTrajectoryPoint center1 = p1.clone();
        center1.copyDirection(center0);
        poly[2] = center1.rotated(-90).moved(3);
        poly[3] = poly[2].moved(-6);
        poly[0].setDirectionTo(poly[1]);
        poly[1].setDirectionTo(poly[2]);
        poly[2].setDirectionTo(poly[3]);
        poly[3].setDirectionTo(poly[0]);

        int center_x = (p0.gridX + p1.gridX) / 2;
        int center_y = (p0.gridY + p1.gridY) / 2;
        int radius = (int) StrictMath.round(poly[0].gridDistanceTo(poly[2])) + 10;
        ShipFinder finder = new ShipFinder(radius, ship);
        grid.scan(finder, center_x, center_y, UnitGrid.SEA);
        for (Ship otherShip : finder.results()) {
            int dist_dx = ship.getGridX() - otherShip.getGridX();
            int dist_dy = ship.getGridY() - otherShip.getGridY();
            if (dist_dx * dist_dx + dist_dy * dist_dy < 14 * 14) {
                var otherPoly = shipToPolygon(otherShip);
                if (polygonCollision(poly, otherPoly)) {
                    return true;
                }
            }
        }

        return false;
    }
}
