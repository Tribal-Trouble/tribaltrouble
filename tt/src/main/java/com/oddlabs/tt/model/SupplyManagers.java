package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class SupplyManagers {
	private final @NonNull Map<@NonNull Class<? extends Supply>, @NonNull SupplyManager> supply_managers;

	public final void debugSpawn() {
            supply_managers.values().forEach(SupplyManager::debugSpawnSupply);
	}

	public SupplyManagers(@NonNull World world) {
		supply_managers = Map.of(
                TreeSupply.class, new SupplyManager(world),
                RockSupply.class, new SupplyManager(world),
                IronSupply.class, new SupplyManager(world),
                RubberSupply.class, new RubberSupplyManager(world)
        );
	}

	public final @Nullable SupplyManager getSupplyManager(@NonNull Class<? extends Supply> type) {
		return supply_managers.get(type);
	}
}
