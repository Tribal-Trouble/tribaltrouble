package com.oddlabs.tt.pathfinder;

final class RegionBuilderNode {
	private final int x;
	private final int y;
	private int total_cost;
	private boolean visited;

	public RegionBuilderNode(int x, int y) {
		this.x = x;
		this.y = y;
		total_cost = Integer.MAX_VALUE;
	}

	public Region getRegion(UnitGrid unit_grid) {
		return unit_grid.getRegion(x, y);
	}

/*	public final void setRegion(UnitGrid unit_grid, Region region) {
		unit_grid.setRegion(x, y, region);
	}
*/
	public int getGridX() {
		return x;
	}

/*	public final boolean isOccupied(UnitGrid unit_grid) {
		return unit_grid.isGridOccupied(x, y);
	}

	public final void occupy(Occupant occupant) {
		unit_grid.occupyGrid(x, y, occupant);
	}
*/
	public int getGridY() {
		return y;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public void setTotalCost(int total_cost) {
		this.total_cost = total_cost;
	}

	public int getTotalCost() {
		return total_cost;
	}

	public boolean isVisited() {
		return visited;
	}
}
