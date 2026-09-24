package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.UnitTemplate;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * What one player's units and buildings are drawn with, for every unit and building they own. Render-only, never part
 * of the sim: two players in the same game may hold different skins without their simulations diverging.
 */
public final class PlayerSkins {
    public static final PlayerSkins NONE = new PlayerSkins(Map.of(), Map.of(), Map.of());

    private final @NonNull Map<UnitTemplate, SpriteKey> units;
    private final @NonNull Map<BuildingTemplate, Map<Building.BuildState, SpriteKey>> buildings;
    private final @NonNull Map<BuildingTemplate, Map<Building.BuildState, List<SpriteKey>>> props;

    public PlayerSkins(
            @NonNull Map<UnitTemplate, SpriteKey> units,
            @NonNull Map<BuildingTemplate, Map<Building.BuildState, SpriteKey>> buildings,
            @NonNull Map<BuildingTemplate, Map<Building.BuildState, List<SpriteKey>>> props) {
        this.units = units;
        this.buildings = buildings;
        this.props = props;
    }

    public @NonNull SpriteKey rendererFor(@NonNull UnitTemplate template) {
        SpriteKey skin = units.get(template);
        return skin != null ? skin : template.getSpriteRenderer();
    }

    public @NonNull SpriteKey rendererFor(@NonNull BuildingTemplate template, Building.@NonNull BuildState stage) {
        SpriteKey skin = buildings.getOrDefault(template, Map.of()).get(stage);
        return skin != null ? skin : template.getRenderer(stage);
    }

    /** Props follow the mesh: a stage the skin replaces draws the skin's props, any other stage the stock ones. */
    public @NonNull List<SpriteKey> propsFor(@NonNull BuildingTemplate template, Building.@NonNull BuildState stage) {
        List<SpriteKey> skin = props.getOrDefault(template, Map.of()).get(stage);
        return skin != null ? skin : template.getProps(stage);
    }
}
