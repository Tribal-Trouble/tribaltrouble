package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;

public abstract strictfp class Building extends Selectable implements Occupant {

    private static final float REMOVE_DELAY = 1f / 10f;

    public static final int RENDER_START = 0;
    public static final int RENDER_HALFBUILT = 1;
    public static final int RENDER_BUILT = 2;

    private static final int PLACING_BORDER = 1;
    private static final int MAX_SUPPLY_COUNT = 200;

    public static final Cost COST_ROCK_WEAPON =
            new Cost(new Class[] {TreeSupply.class, RockSupply.class}, new int[] {2, 1});
    public static final Cost COST_IRON_WEAPON =
            new Cost(new Class[] {TreeSupply.class, IronSupply.class}, new int[] {2, 1});
    public static final Cost COST_RUBBER_WEAPON =
            new Cost(
                    new Class[] {
                        TreeSupply.class, RockSupply.class, IronSupply.class, RubberSupply.class
                    },
                    new int[] {2, 1, 1, 1});

    public static final int KEY_DEPLOY_ROCK_WARRIOR = 0;
    public static final int KEY_DEPLOY_IRON_WARRIOR = 1;
    public static final int KEY_DEPLOY_RUBBER_WARRIOR = 2;
    public static final int KEY_DEPLOY_PEON = 3;
    public static final int KEY_DEPLOY_PEON_HARVEST_TREE = 4;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_TREE = 5;
    public static final int KEY_DEPLOY_PEON_HARVEST_ROCK = 6;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_ROCK = 7;
    public static final int KEY_DEPLOY_PEON_HARVEST_IRON = 8;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_IRON = 9;
    public static final int KEY_DEPLOY_PEON_HARVEST_RUBBER = 10;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_RUBBER = 11;

    public static Class deployTypeToGatherSupply(int deploy_type) {
        switch (deploy_type) {
            case KEY_DEPLOY_PEON_HARVEST_TREE:
                return TreeSupply.class;
            case KEY_DEPLOY_PEON_HARVEST_ROCK:
                return RockSupply.class;
            case KEY_DEPLOY_PEON_HARVEST_IRON:
                return IronSupply.class;
            case KEY_DEPLOY_PEON_HARVEST_RUBBER:
                return RubberSupply.class;
            default:
                return null;
        }
    }

    private static final float DAMAGED_PARTICLE_ALPHA = 3f;

    public Building(Player owner, BuildingTemplate template) {
        super(owner, template);
    }

    public abstract float getOffsetZ();

    public abstract void visit(ElementVisitor visitor);

    public abstract BuildingTemplate getBuildingTemplate();

    public abstract boolean hasRallyPoint();

    public abstract Target getRallyPoint();

    public abstract UnitContainer getUnitContainer();

    public abstract SupplyContainer getSupplyContainer(Class key);

    public final int getUnitCount() {
        assert !isDead();
        return getUnitContainer().getNumSupplies();
    }

    public void hit(int damage, float dir_x, float dir_y, Player owner) {
        super.hit(damage, dir_x, dir_y, owner);
    }

    public abstract void deployUnits(int type, int num_units);

    public abstract void createHarvesters(int num_tree, int num_rock, int num_iron, int num_rubber);

    public abstract boolean canBuildChieftain();

    public abstract boolean canStopChieftain();

    public abstract void trainChieftain(boolean start);

    public abstract void deployChieftain();

    public abstract void createArmy(int num_peon, int num_rock, int num_iron, int num_rubber);

    public abstract void createTransporters(
            int num_tree, int num_rock, int num_iron, int num_rubber);

    public abstract boolean isDamaged();

    public abstract int getHitPoints();

    public abstract void repair(int amount);

    public abstract boolean isPlacingLegal();

    public abstract boolean isPlaced();

    public abstract boolean isComplete();

    public abstract float getHitOffsetZ();

    public abstract int getAttackPriority();

    public abstract void place();

    public abstract float getSize();

    public abstract int getPenalty();

    public abstract boolean isValidRallyPoint(Target t);

    public abstract void setRallyPoint(Target target);

    public abstract int getRenderLevel();

    public abstract SpriteKey getSpriteRenderer();

    public abstract void visit(ToolTipVisitor visitor);

    public abstract void occupy();

    public abstract void free();

    public abstract void fillSupplies(Class key, int max);

    public abstract void removeSupplies(Class key);

    public abstract int getStatusValue();

    public abstract void printDebugInfo();

    public abstract float getHealth();

    public abstract boolean canExitTower();

    public abstract void exitTower();

    public abstract ChieftainContainer getChieftainContainer();

    public abstract BuildSupplyContainer getBuildSupplyContainer(Class key);

    public abstract void buildWeapons(Class type, int num_weapons, boolean infinite);

    public abstract DeployContainer getDeployContainer(int key);
}
