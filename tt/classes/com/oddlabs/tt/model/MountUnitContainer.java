package com.oddlabs.tt.model;

public final class MountUnitContainer extends UnitContainer {
	public static final float ATTACK_RANGE_INCREASE = 8f;

	private final Building building;
	private Unit unit;

	public MountUnitContainer(Building building) {
		super(1);
		this.building = building;
	}

    @Override
	public void enter(Unit unit) {
		this.unit = unit;
		unit.mount(building);
		unit.increaseRange(ATTACK_RANGE_INCREASE);
		increaseSupply(1);
		building.getAbilities().addAbilities(Abilities.TARGET);
	}

    @Override
	public Unit exit() {
		assert unit != null;
		unit.unmount();
		unit.increaseRange(-ATTACK_RANGE_INCREASE);
		Unit result = unit;
		unit = null;
		increaseSupply(-1);
		building.getAbilities().removeAbilities(Abilities.TARGET);
		return result;
	}

    @Override
	public boolean canEnter(Unit unit) {
		return !isSupplyFull() && unit.getAbilities().hasAbilities(Abilities.THROW);
	}

    @Override
	public void animate(float t) {
	}

	public Unit getUnit() {
		return unit;
	}
}
