package com.oddlabs.tt.model;

import com.oddlabs.tt.gui.GUIIcons;
import com.oddlabs.tt.gui.IconQuad;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Captures the supply types and amounts needed for a production. */
public record Cost(@NonNull Map<@NonNull SupplyType, @NonNull Integer> costs) {

    public Cost {
        costs = new EnumMap<>(costs);
    }

    public int getCost(@NonNull SupplyType supplyType) {
        return costs.getOrDefault(supplyType, 0);
    }

    public List<@NonNull IconQuad> iconList() {
        return costs.entrySet().stream()
                .flatMap(entry -> Stream.generate(() -> getIconQuad(entry.getKey())).limit(entry.getValue()))
                .toList();
    }

    private @NonNull IconQuad getIconQuad(@NonNull SupplyType supply_type) {
        var icons = GUIIcons.getIcons();
        return switch (supply_type) {
            case WOOD -> icons.getTreeStatusIcon();
            case ROCK -> icons.getRockStatusIcon();
            case IRON -> icons.getIronStatusIcon();
            case RUBBER -> icons.getRubberStatusIcon();
        };
    }
}
