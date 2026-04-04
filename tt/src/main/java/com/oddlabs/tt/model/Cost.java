package com.oddlabs.tt.model;

import com.oddlabs.tt.gui.GUIIcons;
import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.landscape.TreeSupply;
import org.jspecify.annotations.NonNull;

public final class Cost {
    private final @NonNull Class<? extends Supply> @NonNull [] supply_types;
    private final int @NonNull [] supply_amounts;

    public Cost(@NonNull Class<? extends Supply> @NonNull [] supply_types, int @NonNull [] supply_amounts) {
        this.supply_types = supply_types;
        this.supply_amounts = supply_amounts;
        assert supply_types.length == supply_amounts.length;
    }

    public Class<? extends Supply> @NonNull [] getSupplyTypes() {
        return supply_types;
    }

    public int[] getSupplyAmounts() {
        return supply_amounts;
    }

    public @NonNull IconQuad @NonNull [] toIconArray() {
        int size = 0;
        for (int supplyAmount : supply_amounts) {
            size += supplyAmount;
        }
        IconQuad[] result = new IconQuad[size];
        int index = 0;
        for (int i = 0; i < supply_types.length; i++) {
            IconQuad icon = getIconQuad(supply_types[i]);
            for (int j = 0; j < supply_amounts[i]; j++) {
                result[index++] = icon;
            }
        }
        assert index == result.length;
        return result;
    }

    private @NonNull IconQuad getIconQuad(@NonNull Class<? extends Supply> supply_type) {
        IconQuad icon;
        if (supply_type == TreeSupply.class) {
            icon = GUIIcons.getIcons().getTreeStatusIcon();
        } else if (supply_type == RockSupply.class) {
            icon = GUIIcons.getIcons().getRockStatusIcon();
        } else if (supply_type == IronSupply.class) {
            icon = GUIIcons.getIcons().getIronStatusIcon();
        } else if (supply_type == RubberSupply.class) {
            icon = GUIIcons.getIcons().getRubberStatusIcon();
        } else {
            throw new RuntimeException("Wrong supply_type");
        }
        return icon;
    }
}
