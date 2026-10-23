package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.UnitTemplate;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * What one player's units, buildings and held items are drawn with, for everything they own. Render-only, never part
 * of the sim: two players in the same game may hold different skins without their simulations diverging.
 */
public final class PlayerSkins {
    public static final PlayerSkins NONE = new PlayerSkins(Map.of(), Map.of(), Map.of(), Map.of());

    private final @NonNull Map<UnitTemplate, SpriteKey> units;
    private final @NonNull Map<BuildingTemplate, Map<Building.BuildState, SpriteKey>> buildings;
    private final @NonNull Map<BuildingTemplate, Map<Building.BuildState, List<SpriteKey>>> props;
    private final @NonNull Map<SpriteKey, SpriteKey> items;

    public PlayerSkins(
            @NonNull Map<UnitTemplate, SpriteKey> units,
            @NonNull Map<BuildingTemplate, Map<Building.BuildState, SpriteKey>> buildings,
            @NonNull Map<BuildingTemplate, Map<Building.BuildState, List<SpriteKey>>> props,
            @NonNull Map<SpriteKey, SpriteKey> items) {
        this.units = units;
        this.buildings = buildings;
        this.props = props;
        this.items = items;
    }

    public @NonNull SpriteKey rendererFor(@NonNull UnitTemplate template) {
        SpriteKey skin = units.get(template);
        return skin != null ? skin : template.getSpriteRenderer();
    }

    public @NonNull SpriteKey rendererFor(@NonNull BuildingTemplate template, Building.@NonNull BuildState stage) {
        SpriteKey skin = buildings.getOrDefault(template, Map.of()).get(stage);
        return skin != null ? skin : template.getRenderer(stage);
    }

    /** A stage the skin replaces draws the building's own props plus the skin's; any other stage the stock ones. */
    public @NonNull List<SpriteKey> propsFor(@NonNull BuildingTemplate template, Building.@NonNull BuildState stage) {
        List<SpriteKey> skin = props.getOrDefault(template, Map.of()).get(stage);
        return skin != null ? skin : template.getProps(stage);
    }

    /** An item a unit holds or carries, as its template or supply container names it. */
    public @NonNull SpriteKey itemFor(@NonNull SpriteKey item) {
        return items.getOrDefault(item, item);
    }
}
