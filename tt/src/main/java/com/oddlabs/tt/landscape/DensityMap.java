package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.player.Player;


public final class DensityMap {
    private final World world;
    private final HeightMap map;
    private int last_tick = 0;
    private final int CHUNK_SIZE = 32;
    private final int[][] data;
    private final int size;
    private final int team;

    private final int TOWER_WEIGHT = 10;
    private final int PEON_WEIGHT = 1;
    private final int ROCK_WEIGHT = 3;
    private final int IRON_WEIGHT = 5;
    private final int RUBBER_WEIGHT = 8;
    private final int CHIEFTAIN_WEIGHT = 11;
    private final int OUTSIDE_SCALE = 2;

    public DensityMap(World world, int team) {
        this.world = world;
        this.map = world.getHeightMap();
        this.team = team;
        this.size = map.getGridUnitsPerWorld() >> 5;
        this.data = new int[size][size];
    }

    private int calcWeight(Selectable s) {
        if (s instanceof Unit unit) {
            Class type = unit.getWeaponFactory().getType();
            if (type == RockAxeWeapon.class || type == RockSpearWeapon.class) {
                return ROCK_WEIGHT * OUTSIDE_SCALE;
            } else if (type == IronAxeWeapon.class || type == IronSpearWeapon.class) {
                return IRON_WEIGHT * OUTSIDE_SCALE;
            } else if (type == RubberAxeWeapon.class || type == RubberSpearWeapon.class) {
                return RUBBER_WEIGHT * OUTSIDE_SCALE;
            } else {
                return PEON_WEIGHT * OUTSIDE_SCALE;
            }
        } else if (s instanceof LandBuilding building) {
            if (!building.isComplete()) {
                return 0;
            }

            float health = building.getHitPoints() / (float) building.getTemplate().getMaxHitPoints();
            int weight = 0;
            if (building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
                weight += building.getDeployContainer(DeployType.ROCK_WARRIOR).getNumSupplies() * ROCK_WEIGHT;
                weight += building.getDeployContainer(DeployType.IRON_WARRIOR).getNumSupplies() * IRON_WEIGHT;
                weight += building.getDeployContainer(DeployType.RUBBER_WARRIOR).getNumSupplies() * RUBBER_WEIGHT;
                weight += building.getDeployContainer(DeployType.PEON).getNumSupplies() * PEON_WEIGHT;
                int total = building.getUnitContainer().getNumSupplies();
                int rubber = building.getSupplyContainer(RubberAxeWeapon.class).getNumSupplies();
                rubber += building.getSupplyContainer(RubberSpearWeapon.class).getNumSupplies();
                rubber = Math.min(rubber, total);
                weight += rubber * RUBBER_WEIGHT;
                total -= rubber;
                int iron = building.getSupplyContainer(IronAxeWeapon.class).getNumSupplies();
                iron += building.getSupplyContainer(IronSpearWeapon.class).getNumSupplies();
                iron = Math.min(iron, total);
                weight += iron * IRON_WEIGHT;
                total -= iron;
                int rock = building.getSupplyContainer(RockAxeWeapon.class).getNumSupplies();
                rock += building.getSupplyContainer(RockSpearWeapon.class).getNumSupplies();
                rock = Math.min(rock, total);
                weight += rock * ROCK_WEIGHT;
                total -= rock;
                weight += total * PEON_WEIGHT;
                return StrictMath.round(weight * health);
            } else if (building.getAbilities().hasAbilities(Abilities.ATTACK)) {
                weight += building.getUnitContainer().getNumSupplies() * TOWER_WEIGHT;
            } else {
                weight += building.getUnitContainer().getNumSupplies() * PEON_WEIGHT;
            }

            return StrictMath.round(weight * health);
        } else if (s instanceof Ship ship) {
            if (!ship.isComplete()) {
                return 0;
            }
            var hr = ship.getShipHR();
            int weight = 0;
            int peons = hr.countPeons();
            weight += peons * PEON_WEIGHT;
            int rubber = hr.countUnitsOfType(RubberAxeWeapon.class);
            rubber += hr.countUnitsOfType(RubberSpearWeapon.class);
            int iron = hr.countUnitsOfType(IronAxeWeapon.class);
            iron += hr.countUnitsOfType(IronSpearWeapon.class);
            int rock = hr.countUnitsOfType(RockAxeWeapon.class);
            iron += hr.countUnitsOfType(RockSpearWeapon.class);
            weight += rubber * RUBBER_WEIGHT * OUTSIDE_SCALE;
            weight += iron * IRON_WEIGHT * OUTSIDE_SCALE;
            weight += rock * ROCK_WEIGHT * OUTSIDE_SCALE;
            float health = ship.getHitPoints() / (float) ship.getTemplate().getMaxHitPoints();
            return StrictMath.round(weight * health);
        } else {
            return 0;
        }
    }

    public void update() {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                data[y][x] = 0;
            }
        }
        for (Player player : world.getPlayers()) {
            if (player.getPlayerInfo().getTeam() != team) {
                for (var s : player.getUnits().getSet()) {
                    int weight = calcWeight(s);
                    int x = StrictMath.round(s.getGridX() / (float) CHUNK_SIZE);
                    int y = StrictMath.round(s.getGridY() / (float) CHUNK_SIZE);
                    if (x >= 0 && x < size && y >= 0 && y < size) {
                        data[y][x] += weight;
                    }
                }
            }
        }
    }

    public int sum(int gx0, int gy0, int gx1, int gy1) {
        int x0 = StrictMath.round(gx0 / (float) CHUNK_SIZE);
        int x1 = StrictMath.round(gx1 / (float) CHUNK_SIZE);
        int y0 = StrictMath.round(gy0 / (float) CHUNK_SIZE);
        int y1 = StrictMath.round(gy1 / (float) CHUNK_SIZE);
        int total = 0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                total += data[y][x];
            }
        }
        return total;
    }

    public int getWeight(int gx, int gy, int r) {
        int cx = StrictMath.round(gx / (float) CHUNK_SIZE);
        int cy = StrictMath.round(gy / (float) CHUNK_SIZE);
        int total = 0;
        for (int y = Math.max(cy - r, 0); y <= Math.min(cy + r, size - 1); y++) {
            for (int x = Math.max(cx - r, 0); x <= Math.min(cx + r, size - 1); x++) {
                total += data[y][x];
            }
        }
        return total;
    }
}
