package com.oddlabs.tt.model;

import com.oddlabs.tt.render.SpriteKey;

import java.util.Map;

public final class UnitSupplyContainer extends SupplyContainer {
	private final Map<Class<? extends Supply>,SpriteKey> supply_sprite_renderers;

	private Class<? extends Supply> type;

	public UnitSupplyContainer(int max_resource_count, Map<Class<? extends Supply>,SpriteKey> supply_sprite_renderers) {
		super(max_resource_count);
		this.supply_sprite_renderers = supply_sprite_renderers;
	}

        @Override
	public int increaseSupply(int amount) {
		throw new RuntimeException();
	}

	public int increaseSupply(int amount, Class<? extends Supply> type) {
		if (this.type != type) {
			this.type = type;
			super.increaseSupply(-super.getNumSupplies());
		}
		return super.increaseSupply(amount);
	}

	public Class<? extends Supply> getSupplyType() {
		return type;
	}

	public SpriteKey getSupplySpriteRenderer(Class<? extends Supply> key) {
		return supply_sprite_renderers.get(key);
	}
}
