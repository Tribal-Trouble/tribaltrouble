package com.oddlabs.tt.model;

import com.oddlabs.tt.model.behaviour.NullController;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;

public final strictfp class ShipProxy extends Building {
    private final Ship ship;
    private Region last_region = null;

    public ShipProxy(int grid_x, int grid_y, Ship ship) {
        super(ship.getOwner(), ship.getBuildingTemplate());
        this.ship = ship;
        setGridPosition(grid_x, grid_y);
        UnitGrid unit_grid = getUnitGrid();
        float x = UnitGrid.coordinateFromGrid(grid_x);
        float y = UnitGrid.coordinateFromGrid(grid_y);
        super.setPosition(x, y);
        pushController(new NullController(this));
    }

    public final boolean isEnabled() {
        return ship.isEnabled();
    }

    protected final void setTarget(Target target, int action, boolean aggressive) {}

    public final void visit(ElementVisitor visitor) {}

    public final void visit(ToolTipVisitor visitor) {}

    public final void occupy() {
        UnitGrid grid = getUnitGrid();
        last_region = grid.getRegion(getGridX(), getGridY(), UnitGrid.LAND);
        if (last_region != null) last_region.registerObject(Building.class, this);
        grid.occupyGrid(getGridX(), getGridY(), this, UnitGrid.LAND);
    }

    public final void free() {
        if (last_region != null) {
            last_region.unregisterObject(Building.class, this);
            last_region = null;
        }
        getUnitGrid().freeGrid(getGridX(), getGridY(), this, UnitGrid.LAND);
    }

    public final void place() {
        register();
        occupy();
        getAbilities().addAbilities(getTemplate().getAbilities());
        reinsert();
    }

    protected final void removeDying() {
        free();
        super.removeDying();
        remove();
    }

    public final void hit(int damage, float dir_x, float dir_y, Player owner) {
        ship.hit(damage, dir_x, dir_y, owner);
    }

    public final UnitContainer getUnitContainer() {
        return ship.getUnitContainer();
    }

    public final SupplyContainer getSupplyContainer(Class key) {
        return ship.getSupplyContainer(key);
    }

    public final float getOffsetZ() {
        return 0f;
    }

    public final BuildingTemplate getBuildingTemplate() {
        return (BuildingTemplate) getTemplate();
    }

    public final float getSize() {
        return 0f;
    }

    public final int getPenalty() {
        return Occupant.STATIC;
    }

    public final int getRenderLevel() {
        return RENDER_BUILT;
    }

    public final SpriteKey getSpriteRenderer() {
        return getOwner().getRace().getRallyPoint();
    }

    public final float getAnimationTicks() {
        return 0f;
    }

    public final int getAnimation() {
        return 0;
    }

    public final boolean isPlaced() {
        return ship.isPlaced();
    }

    public final boolean isComplete() {
        return ship.isComplete();
    }

    public final boolean isDamaged() {
        return ship.isDamaged();
    }

    public final int getHitPoints() {
        return ship.getHitPoints();
    }

    public final float getHealth() {
        return ship.getHealth();
    }

    public final void repair(int amount) {
        ship.repair(amount);
    }

    public final float getHitOffsetZ() {
        return 0f;
    }

    public final int getAttackPriority() {
        return AttackScanFilter.PRIORITY_QUARTERS;
    }

    public final boolean isPlacingLegal() {
        return !isDead();
    }

    public final boolean hasRallyPoint() {
        return ship.hasRallyPoint();
    }

    public final Target getRallyPoint() {
        return ship.getRallyPoint();
    }

    public final boolean isValidRallyPoint(Target t) {
        return ship.isValidRallyPoint(t);
    }

    public final void setRallyPoint(Target target) {
        ship.setRallyPoint(target);
    }

    public final DeployContainer getDeployContainer(int key) {
        return ship.getDeployContainer(key);
    }

    public final ChieftainContainer getChieftainContainer() {
        return null;
    }

    public final BuildSupplyContainer getBuildSupplyContainer(Class key) {
        return ship.getBuildSupplyContainer(key);
    }

    public final void deployUnits(int type, int num_units) {
        ship.deployUnits(type, num_units);
    }

    public final void createHarvesters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        ship.createHarvesters(num_tree, num_rock, num_iron, num_rubber);
    }

    public final void createArmy(int num_peon, int num_rock, int num_iron, int num_rubber) {
        ship.createArmy(num_peon, num_rock, num_iron, num_rubber);
    }

    public final void createTransporters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        ship.createTransporters(num_tree, num_rock, num_iron, num_rubber);
    }

    public final void buildWeapons(Class type, int num_weapons, boolean infinite) {}

    public final boolean canBuildChieftain() {
        return false;
    }

    public final boolean canStopChieftain() {
        return false;
    }

    public final void trainChieftain(boolean start) {}

    public final void deployChieftain() {}

    public final boolean canExitTower() {
        return false;
    }

    public final void exitTower() {}

    public final void fillSupplies(Class key, int max) {
        ship.fillSupplies(key, max);
    }

    public final void removeSupplies(Class key) {
        ship.removeSupplies(key);
    }

    public final int getStatusValue() {
        return ship.getStatusValue();
    }

    public final void printDebugInfo() {}
}
