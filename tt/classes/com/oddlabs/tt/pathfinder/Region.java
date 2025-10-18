package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.landscape.HeightMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public final class Region extends Node {
	private final Map<Class<?>,List<?>> object_lists = new HashMap<>();
	private final List<Region> neighbours = new ArrayList<>();

	private int center_x;
	private int center_y;

        @Override
	public int getGridX() {
		return center_x;
	}

        @Override
	public int getGridY() {
		return center_y;
	}

	public void setPosition(int center_x, int center_y) {
		this.center_x = center_x;
		this.center_y = center_y;
	}

        @Override
	public String toString() {
		return "Region: " + center_x + " " + center_y;
	}
        @Override
	public PathNode newPath() {
		Node graph_node = this;
		assert graph_node != null;
		RegionNode current_node = null;
		while (graph_node != null) {
			current_node = new RegionNode(current_node, (Region)graph_node);
			graph_node = graph_node.getParent();
		}
		return current_node;
	}

	public static void link(Region r1, Region r2) {
		if (r1 == null || r2 == null || r1 == r2 || r1.neighbours.contains(r2))
			return;
		r1.addNeighbour(r2);
		r2.addNeighbour(r1);
	}

	public <K> List<K> getObjects(Class<? super K> key) {
                @SuppressWarnings("unchecked")
		List<K> list = (List<K>) object_lists.get(key);
		if (list == null) {
			list = new ArrayList<>();
			object_lists.put(key, list);
		}
		return list;
	}

	public <K> void registerObject(Class<? super K> key, K object) {
		getObjects(key).add(object);
	}

	public <K> void unregisterObject(Class<? super K> key, K object) {
                @SuppressWarnings("unchecked")
		List<K> list = (List<K>) object_lists.get(key);
                assert list != null : "Unknown key";
		list.remove(object);
	}

	private void addNeighbour(Region n) {
		neighbours.add(n);
	}

        @Override
	public boolean addNeighbours(PathFinderAlgorithm finder, UnitGrid unit_grid) {
		for (int i = 0; i < neighbours.size(); i++) {
			Region neighbour = neighbours.get(i);
			if (!neighbour.isVisited())
				PathFinder.addToOpenList(finder, neighbour, this, estimateCost(neighbour.getGridX(), neighbour.getGridY()));
		}
		return false;
	}

	private void debugVertex(HeightMap heightmap) {
		float xf = UnitGrid.coordinateFromGrid(getGridX());
		float yf = UnitGrid.coordinateFromGrid(getGridY());
		GL11.glVertex3f(xf, yf, heightmap.getNearestHeight(xf, yf) + 2f);
	}

	public void debugRenderConnectionsReset() {
		if (!isVisited())
			return;
		setVisited(false);
		for (int i = 0; i < neighbours.size(); i++) {
			Region neighbour = neighbours.get(i);
			neighbour.debugRenderConnectionsReset();
		}
	}

	public void debugRenderConnections(HeightMap heightmap) {
		if (isVisited())
			return;
		setVisited(true);
		for (int i = 0; i < neighbours.size(); i++) {
			Region neighbour = neighbours.get(i);
			debugVertex(heightmap);
			neighbour.debugVertex(heightmap);
			neighbour.debugRenderConnections(heightmap);
		}
	}
}
