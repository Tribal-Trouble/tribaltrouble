package com.oddlabs.tt.model;

import com.oddlabs.tt.gui.Icons;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;

public final class Cost {
	private final Class<? extends Supply>[] supply_types;
	private final int[] supply_amounts;

	public Cost(Class<? extends Supply> @NonNull [] supply_types, int @NonNull [] supply_amounts) {
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

	public Quad @NonNull [] toIconArray() {
		int size = 0;
        for (int supplyAmount : supply_amounts) {
            size += supplyAmount;
        }
		Quad[] result = new Quad[size];
		int index = 0;
		for (int i = 0; i < supply_types.length; i++) {
			Class<? extends Supply> type = supply_types[i];
			Quad icon;
			if (type == TreeSupply.class) {
				icon = Icons.getIcons().getTreeStatusIcon();
			} else if (type == RockSupply.class) {
				icon = Icons.getIcons().getRockStatusIcon();
			} else if (type == IronSupply.class) {
				icon = Icons.getIcons().getIronStatusIcon();
			} else if (type == RubberSupply.class) {
				icon = Icons.getIcons().getRubberStatusIcon();
			} else {
				throw new RuntimeException("Wrong supply_type");
			}
			for (int j = 0; j < supply_amounts[i]; j++) {
                result[index++] = icon;
            }
		}
		assert index == result.length;
		return result;
	}
}
