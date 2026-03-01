package com.oddlabs.tt.net;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.util.Target;

public final strictfp class NoOpPlayerInterface implements PlayerInterface {
    public void deployUnits(Building building, int type, int num_units) {}

    public void createHarvesters(
            Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {}

    public void buildRockWeapons(Building building, int num_weapons, boolean infinite) {}

    public void buildIronWeapons(Building building, int num_weapons, boolean infinite) {}

    public void buildRubberWeapons(Building building, int num_weapons, boolean infinite) {}

    public void doMagic(Unit chieftain, int magic) {}

    public void exitTower(Building building) {}

    public void trainChieftain(Building building, boolean start) {}

    public void placeBuilding(
            Selectable[] selection, int template_id, int placing_grid_x, int placing_grid_y) {}

    public void setRallyPoint(Building building, Target target) {}

    public void setTarget(Selectable[] selection, Target target, int action, boolean aggressive) {}

    public void setRallyPoint(Building building, int grid_x, int grid_y) {}

    public void setLandscapeTarget(
            Selectable[] selection, int grid_x, int grid_y, int action, boolean aggressive) {}

    public void setPreferredGamespeed(int speed) {}

    public void changePreferredGamespeed(int delta) {}
}
