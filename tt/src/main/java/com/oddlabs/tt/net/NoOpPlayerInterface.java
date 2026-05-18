package com.oddlabs.tt.net;

import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

/**
 * A no-op player interface for spectators — all commands are ignored.
 */
public final class NoOpPlayerInterface implements PlayerInterface {
    @Override
    public void deployUnits(@NonNull Building building, @NonNull DeployType type, int num_units) {
    }

    @Override
    public void createHarvesters(@NonNull Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {
    }

    @Override
    public void buildRockWeapons(@NonNull Building building, int num_weapons, boolean infinite) {
    }

    @Override
    public void buildIronWeapons(@NonNull Building building, int num_weapons, boolean infinite) {
    }

    @Override
    public void buildRubberWeapons(@NonNull Building building, int num_weapons, boolean infinite) {
    }

    @Override
    public void doMagic(@NonNull Unit chieftain, int magic) {
    }

    @Override
    public void exitTower(@NonNull Building building) {
    }

    @Override
    public void trainChieftain(@NonNull Building building, boolean start) {
    }

    @Override
    public void placeBuilding(Selectable<?> @NonNull [] selection, int template_id, int placing_grid_x,
            int placing_grid_y) {
    }

    @Override
    public void setRallyPoint(@NonNull Building building, @NonNull Target target) {
    }

    @Override
    public void setTarget(Selectable<?> @NonNull [] selection, @NonNull Target target, @NonNull Action action,
            boolean aggressive) {
    }

    @Override
    public void setRallyPoint(@NonNull Building building, int grid_x, int grid_y) {
    }

    @Override
    public void setLandscapeTarget(Selectable<?> @NonNull [] selection, int grid_x, int grid_y, @NonNull Action action,
            boolean aggressive) {
    }

    @Override
    public void recallGatherers(@NonNull Building building, @NonNull Class<? extends Supply> supply_type, int amount) {
    }

    @Override
    public void setPreferredGamespeed(int speed) {
    }

    @Override
    public void changePreferredGamespeed(int delta) {
    }
}
