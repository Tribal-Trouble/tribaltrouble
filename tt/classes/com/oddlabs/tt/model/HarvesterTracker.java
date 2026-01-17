package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the number of peons currently assigned to gathering each resource type. This allows the UI
 * to display the count of active harvesters per resource.
 */
public final strictfp class HarvesterTracker {
    // Maps resource type (Class) to set of units gathering that resource
    private final Map harvestersByType = new HashMap();

    public HarvesterTracker() {
        // Initialize sets for each resource type
        harvestersByType.put(TreeSupply.class, new HashSet());
        harvestersByType.put(RockSupply.class, new HashSet());
        harvestersByType.put(IronSupply.class, new HashSet());
        harvestersByType.put(RubberSupply.class, new HashSet());
    }

    /**
     * Register a unit as actively gathering a specific resource type.
     *
     * @param unit The unit to register
     * @param supplyType The resource type being gathered
     */
    public void registerHarvester(Unit unit, Class supplyType) {
        // First remove from any other type it might be registered with
        unregisterHarvester(unit);

        Set harvesters = (Set) harvestersByType.get(supplyType);
        if (harvesters != null) {
            harvesters.add(unit);
        }
    }

    /**
     * Unregister a unit from all resource types. Called when the unit dies, is interrupted, or
     * stops gathering.
     *
     * @param unit The unit to unregister
     */
    public void unregisterHarvester(Unit unit) {
        Iterator it = harvestersByType.values().iterator();
        while (it.hasNext()) {
            Set harvesters = (Set) it.next();
            harvesters.remove(unit);
        }
    }

    /**
     * Get the count of harvesters for a specific resource type.
     *
     * @param supplyType The resource type
     * @return The number of active harvesters
     */
    public int getHarvesterCount(Class supplyType) {
        Set harvesters = (Set) harvestersByType.get(supplyType);
        if (harvesters != null) {
            // Clean up dead units from the set
            cleanupDeadUnits(harvesters);
            return harvesters.size();
        }
        return 0;
    }

    /**
     * Get all harvesters of a specific resource type.
     *
     * @param supplyType The resource type
     * @return Set of units gathering that resource
     */
    public Set getHarvesters(Class supplyType) {
        Set harvesters = (Set) harvestersByType.get(supplyType);
        if (harvesters != null) {
            cleanupDeadUnits(harvesters);
            return harvesters;
        }
        return new HashSet();
    }

    /**
     * Find the harvester nearest to a given position.
     *
     * @param supplyType The resource type
     * @param targetX Target X position
     * @param targetY Target Y position
     * @return The nearest unit, or null if none found
     */
    public Unit findNearestHarvester(Class supplyType, float targetX, float targetY) {
        Set harvesters = (Set) harvestersByType.get(supplyType);
        if (harvesters == null || harvesters.isEmpty()) {
            return null;
        }

        cleanupDeadUnits(harvesters);

        Unit nearest = null;
        float nearestDistSq = Float.MAX_VALUE;

        Iterator it = harvesters.iterator();
        while (it.hasNext()) {
            Unit unit = (Unit) it.next();
            if (!unit.isDead()) {
                float dx = unit.getPositionX() - targetX;
                float dy = unit.getPositionY() - targetY;
                float distSq = dx * dx + dy * dy;
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = unit;
                }
            }
        }

        return nearest;
    }

    /** Remove dead units from a set. */
    private void cleanupDeadUnits(Set harvesters) {
        Iterator it = harvesters.iterator();
        while (it.hasNext()) {
            Unit unit = (Unit) it.next();
            if (unit.isDead()) {
                it.remove();
            }
        }
    }
}
