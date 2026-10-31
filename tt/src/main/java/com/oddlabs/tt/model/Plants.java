package com.oddlabs.tt.model;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class Plants extends SceneryModel {

    private final boolean fades;

    public Plants(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull SpriteKey sprite_renderer, float shadow_diameter, float shadow_fade) {
        this(world, x, y, dir_x, dir_y, sprite_renderer, shadow_diameter, shadow_fade, true);
    }

    // A plant that does not fade is drawn at any distance, like a rock.
    public Plants(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull SpriteKey sprite_renderer, float shadow_diameter, float shadow_fade, boolean fades) {
        super(world, x, y, dir_x, dir_y, sprite_renderer, shadow_diameter, shadow_fade);
        this.fades = fades;
    }

    public boolean fades() {
        return fades;
    }

    @Override
    protected void doRegister() {
        if (Globals.INSERT_PLANTS[Settings.getSettings().graphic_detail]) {
            register();
            reinsert();
        }
    }

    @Override
    public void visit(@NonNull ElementVisitor visitor) {
        visitor.visitPlants(this);
    }
}
