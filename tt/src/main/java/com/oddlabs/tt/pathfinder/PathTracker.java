package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.util.BezierPath;
import com.oddlabs.tt.util.DebugRender;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Tracks and manages unit pathfinding through the game world.
/// Combines high-level region pathfinding with low-level grid navigation
/// and smooth Bézier curve movement.
///
/// Debug visualization (`UNIT_GRID` mode) shows:
/// * Red line: Grid-based path (step-by-step navigation)
/// * Blue line: Region-based path (high-level waypoints)
/// * White line: Bézier curve (smooth movement)
public final class PathTracker {
    public enum State {
        OK,OK_INTERRUPTIBLE,DONE,SOFTBLOCKED,BLOCKED
    }

    private static final int REGION_SEARCH_TRIES = 4;

	private final BezierPath bezier_path = new BezierPath();
	private final @NonNull UnitGrid unit_grid;
	private @NonNull final Movable unit;

	private @Nullable RegionNode region_path;
	private @Nullable Region target_region;
	private @Nullable GridPathNode grid_path;

	private @Nullable Occupant current_blocker;
	private boolean deadlock_mark = false;
	private int next_unit_grid_x;
	private int next_unit_grid_y;

	private @Nullable TrackerAlgorithm tracker_algorithm;

	private boolean initial_path;
	private @NonNull State state = State.DONE;

	public PathTracker(@NonNull UnitGrid unit_grid, @NonNull Movable unit) {
		this.unit_grid = unit_grid;
		this.unit = unit;
	}

	public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
		tool_tip_box.append(" next_x=");
		tool_tip_box.append(next_unit_grid_x);
		tool_tip_box.append(" next_y=");
		tool_tip_box.append(next_unit_grid_y);
	}

	public @NonNull State animate(float speed) {
		doAnimate(speed);
		if (state != State.SOFTBLOCKED && state != State.BLOCKED) {
			current_blocker = null;
		}
		return state;
	}

	private void doAnimate(float speed) {
		assert !deadlock_mark;
		if (bezier_path.isDone()) {
			if (initial_path) {
				state = initPath();
				if (state != State.OK) {
					return;
				}
				initial_path = false;
			}
			if (done(unit.getGridX(), unit.getGridY())) {
				state = State.DONE;
				return;
			}
			state = lookAhead();
			if (state != State.OK) {
				if (state == State.SOFTBLOCKED || state == State.BLOCKED)
					if (checkDeadlock()) {
						state = State.OK;
						return;
					}
				return;
			} else {
				unit.free();
				advance();
			}
		}
		bezier_path.computeCurvePoint(speed);
		update();
		state = bezier_path.isDone()
                ? State.OK_INTERRUPTIBLE
                : State.OK;
	}

	public @Nullable Occupant getBlocker() {
		return current_blocker;
	}

	private boolean checkDeadlock() {
		PathTracker start = findDeadlock();
		if (start != null) {
			start.solveDeadlock();
			return true;
		} else
			return false;
	}

	private @Nullable PathTracker getNextDeadlocked() {
		Occupant occupant = getNextOccupantUnchecked();
		if (occupant != unit && occupant instanceof Movable next) {
            if (next.isMoving() && (next.getTracker().state == State.SOFTBLOCKED || next.getTracker().state == State.BLOCKED)) {
				return next.getTracker();
			}
		}
		return null;
	}


	private @Nullable PathTracker findDeadlock() {
		PathTracker current = this;
		PathTracker result = null;
		while (current != null) {
			if (current.deadlock_mark) {
				result = current;
				break;
			}
			current.deadlock_mark = true;
			current = current.getNextDeadlocked();
		}

		current = this;
		while (current != null && current.deadlock_mark) {
			current.deadlock_mark = false;
			current = current.getNextDeadlocked();
		}

		return result;
	}

	private void solveDeadlock() {
		Movable current = unit;
		current.free();
		while (current != null) {
			Movable next = (Movable)current.getTracker().getNextOccupant();
			if (next != null)
				next.free();
			current.getTracker().advance();
//			current.moveNextAnimate();
			current = next;
		}
	}

	private void advance() {
		unit.setGridPosition(next_unit_grid_x, next_unit_grid_y);
		unit.occupy();
		findNextDirection();
	}

	private @Nullable Occupant getNextOccupantUnchecked() {
		return unit_grid.getOccupant(next_unit_grid_x, next_unit_grid_y);
	}

	private Occupant getNextOccupant() {
		Occupant occ = getNextOccupantUnchecked();
		assert occ != unit: unit.getGridX() + " " + unit.getGridY() + " " + next_unit_grid_x + " " + next_unit_grid_y;
		return occ;
	}

	private void update() {
		unit.setPosition(bezier_path.getCurrentX(), bezier_path.getCurrentY());
		unit.setDirection(bezier_path.getCurrentDirectionX(), bezier_path.getCurrentDirectionY());
	}

	private boolean done(int x, int y) {
		return tracker_algorithm.isDone(x, y);
	}

	private void findNextDirection() {
		if (grid_path == null || done(next_unit_grid_x, next_unit_grid_y)) {
			bezier_path.endPath();
			initial_path = true;
			return;
		}
		DirectionNode dir_node = grid_path.getDirection();
		grid_path = (GridPathNode)grid_path.getParent();
		next_unit_grid_x += dir_node.getDirectionX();
		next_unit_grid_y += dir_node.getDirectionY();
		float next_node_x = UnitGrid.coordinateFromGrid(next_unit_grid_x);
		float next_node_y = UnitGrid.coordinateFromGrid(next_unit_grid_y);
		bezier_path.nextPoint(dir_node.getInvLength(), next_node_x, next_node_y);
	}

	private void checkRegionPath(int src_x, int src_y) {
		Region current_region = unit_grid.getRegion(src_x, src_y);
		if (target_region != null && tracker_algorithm.acceptRegion(target_region)) {
			while (region_path != null) {
				Region region_path_region = region_path.getRegion();
				if (current_region == region_path_region)
					return;
				region_path = (RegionNode)region_path.getParent();
			}
		}
		target_region = tracker_algorithm.findPathRegion(src_x, src_y);
		if (target_region != null)
			region_path = (RegionNode)target_region.newPath();
		else
			region_path = null;
	}

	private @Nullable GridPathNode findPathToNextRegion(int src_x, int src_y, @Nullable RegionNode next_region_node, boolean allow_secondary_targets) {
		Region next_region = null;
		Region next_next_region;
		if (next_region_node != null) {
			next_region = next_region_node.getRegion();
			RegionNode next_next_region_node = (RegionNode)next_region_node.getParent();
			if (next_next_region_node != null) {
				next_next_region = next_next_region_node.getRegion();
				int region_x = next_next_region.getGridX();
				int region_y = next_next_region.getGridY();
				return PathFinder.findPathGrid(unit_grid, next_region, next_next_region, src_x, src_y, region_x, region_y, null, 0, allow_secondary_targets);
			}
		}
		return tracker_algorithm.findPathGrid(target_region, next_region, src_x, src_y, allow_secondary_targets);
	}

	private @NonNull State lookAhead() {
		checkRegionPath(next_unit_grid_x, next_unit_grid_y);
		if (region_path == null) {
			return State.DONE;
		}
		RegionNode next_region_node = (RegionNode)region_path.getParent();
		Occupant occupant = getNextOccupant();
		if (occupant != null) {
			GridPathNode patch_path = null;
			RegionNode search_next_region_node = next_region_node;
			for (int i = 0; i < REGION_SEARCH_TRIES; i++) {
				patch_path = findPathToNextRegion(unit.getGridX(), unit.getGridY(), search_next_region_node, false);
				if (done(unit.getGridX(), unit.getGridY()))
					return State.DONE;
				if (patch_path != null || search_next_region_node == null)
					break;
				search_next_region_node = (RegionNode)search_next_region_node.getParent();
			}

			if (patch_path != null) {
				initBezierPath(patch_path.getDirection());
				grid_path = (GridPathNode)patch_path.getParent();
				occupant = getNextOccupant();
			}

			if (occupant != null) {
				current_blocker = occupant;
				if (occupant.getPenalty() < Occupant.STATIC)
					return State.SOFTBLOCKED;
				else
					return State.BLOCKED;
			}
		} else if (grid_path == null && !done(next_unit_grid_x, next_unit_grid_y)) {
			grid_path = findPathToNextRegion(next_unit_grid_x, next_unit_grid_y, next_region_node, true);
		}
		return State.OK;
	}

	private void initBezierPath(@NonNull DirectionNode dir_node) {
		next_unit_grid_x = unit.getGridX() + dir_node.getDirectionX();
		next_unit_grid_y = unit.getGridY() + dir_node.getDirectionY();
		float next_node_x = UnitGrid.coordinateFromGrid(next_unit_grid_x);
		float next_node_y = UnitGrid.coordinateFromGrid(next_unit_grid_y);
		bezier_path.init(dir_node.getInvLength(), unit.getPositionX(), unit.getPositionY(), next_node_x, next_node_y);
	}

	public void setTarget(@NonNull TrackerAlgorithm tracker_algorithm) {
		this.tracker_algorithm = tracker_algorithm;
		initial_path = true;
		region_path = null;
		grid_path = null;
		target_region = null;
	}

	private @NonNull State initPath() {
		checkRegionPath(unit.getGridX(), unit.getGridY());
		if (region_path == null) {
			return State.DONE;
		}
		RegionNode next_region_node = (RegionNode)region_path.getParent();
		GridPathNode init_path = findPathToNextRegion(unit.getGridX(), unit.getGridY(), next_region_node, true);
		if (done(unit.getGridX(), unit.getGridY()))
			return State.DONE;
		if (init_path == null) {
			return State.BLOCKED;
		}
		initBezierPath(init_path.getDirection());
		grid_path = (GridPathNode)init_path.getParent();
		return State.OK;
	}

	public void debugRender() {
		HeightMap heightmap = unit_grid.getHeightMap();
		bezier_path.debugRender(heightmap);
		final float OFFSET = 2f;
		float next_node_x = UnitGrid.coordinateFromGrid(next_unit_grid_x);
		float next_node_y = UnitGrid.coordinateFromGrid(next_unit_grid_y);
		float prev_x = next_node_x;
		float prev_y = next_node_y;
		float prev_z = heightmap.getNearestHeight(prev_x, prev_y) + OFFSET;
		GridPathNode node = grid_path;
		while (node != null) {
			float next_x = prev_x + HeightMap.METERS_PER_UNIT_GRID*node.getDirection().getDirectionX();
			float next_y = prev_y + HeightMap.METERS_PER_UNIT_GRID*node.getDirection().getDirectionY();
			float z = heightmap.getNearestHeight(next_x, next_y) + OFFSET;
			DebugRender.drawLine(prev_x, prev_y, prev_z, next_x, next_y, z, 1f, 0f, 0f);
			prev_x = next_x;
			prev_y = next_y;
			prev_z = z;
			node = (GridPathNode)node.getParent();
		}
		RegionNode region_node = region_path;
		boolean first = true;
		while (region_node != null) {
			float x = UnitGrid.coordinateFromGrid(region_node.getRegion().getGridX());
			float y = UnitGrid.coordinateFromGrid(region_node.getRegion().getGridY());
			float z = heightmap.getNearestHeight(x, y) + OFFSET;
			if (!first) {
				DebugRender.drawLine(prev_x, prev_y, prev_z, x, y, z, 0f, 0f, 1f);
			}
			prev_x = x;
			prev_y = y;
			prev_z = z;
			first = false;
			region_node = (RegionNode)region_node.getParent();
		}
	}
}
