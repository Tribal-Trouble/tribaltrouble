package com.oddlabs.tt.player;

public final class UnitInfo {
	private final boolean has_quarters;
	private final boolean has_armory;
	private final int num_towers;
	private final boolean has_chieftain;
	private final int num_peons;
	private final int num_rock_warriors;
	private final int num_iron_warriors;
	private final int num_rubber_warriors;

	public UnitInfo() {
		this(false, false, 0, false, 0, 0, 0, 0);
	}

	public UnitInfo(boolean has_quarters,
			boolean has_armory,
			int num_towers,
			boolean has_chieftain,
			int num_peons,
			int num_rock_warriors,
			int num_iron_warriors,
			int num_rubber_warriors) {
		this.has_quarters = has_quarters;
		this.has_armory = has_armory;
		this.num_towers = num_towers;
		this.has_chieftain = has_chieftain;
		this.num_peons = num_peons;
		this.num_rock_warriors = num_rock_warriors;
		this.num_iron_warriors = num_iron_warriors;
		this.num_rubber_warriors = num_rubber_warriors;
	}

	public boolean hasQuarters() {
		return has_quarters;
	}

	public boolean hasArmory() {
		return has_armory;
	}

	public int getNumTowers() {
		return num_towers;
	}

	public boolean hasChieftain() {
		return has_chieftain;
	}

	public int getNumPeons() {
		return num_peons;
	}

	public int getNumRockWarriors() {
		return num_rock_warriors;
	}

	public int getNumIronWarriors() {
		return num_iron_warriors;
	}

	public int getNumRubberWarriors() {
		return num_rubber_warriors;
	}
}
