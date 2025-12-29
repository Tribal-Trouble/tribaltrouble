package com.oddlabs.tt.viewer;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Army;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SelectionArmy extends Army {

    private final @NonNull Player local_player;
    private int num_units;
    private int num_builders;
    private @Nullable Unit chieftain;
    private @Nullable Building building;

    SelectionArmy(@NonNull Player local_player) {
        this.local_player = local_player;
    }

    public int getNumBuilders() {
        return num_builders;
    }

    public int getNumUnits() {
        return num_units;
    }

    public @Nullable Unit getChieftain() {
        return chieftain;
    }

    public @Nullable Building getBuilding() {
        return building;
    }

    private void update() {
        num_units = 0;
        num_builders = 0;
        chieftain = null;
        building = null;
        for (Selectable s : getSet()) {
            if (s.getOwner() != local_player)
                continue;
            Abilities abilities = s.getAbilities();
            if (abilities.hasAbilities(Abilities.BUILD))
                num_builders++;
            else if (abilities.hasAbilities(Abilities.MAGIC))
                    chieftain = (Unit) s;
            if (s instanceof Building building1) {
                building = building1;
            } else {
                num_units++;
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        update();
    }

    @Override
    public void remove(@NonNull Selectable<?> selectable) {
        super.remove(selectable);
        update();
    }

    @Override
    public void add(@NonNull Selectable<?> selectable) {
        super.add(selectable);
        update();
    }
}
