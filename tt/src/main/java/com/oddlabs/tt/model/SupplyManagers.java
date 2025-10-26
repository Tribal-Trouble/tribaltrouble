package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class SupplyManagers {
	private final Map<Class<? extends Supply>,SupplyManager> supply_managers = new HashMap<>();

	public final void debugSpawn() {
            supply_managers.values().forEach(SupplyManager::debugSpawnSupply);
	}

	public SupplyManagers(@NonNull World world) {
		supply_managers.put(TreeSupply.class, new SupplyManager(world));
		supply_managers.put(RockSupply.class, new SupplyManager(world));
		supply_managers.put(IronSupply.class, new SupplyManager(world));
		supply_managers.put(RubberSupply.class, new RubberSupplyManager(world));
	}

	public final SupplyManager getSupplyManager(Class<? extends Supply> type) {
		return supply_managers.get(type);
	}
}
