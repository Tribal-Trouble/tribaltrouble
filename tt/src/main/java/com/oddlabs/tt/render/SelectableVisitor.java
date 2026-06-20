package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * ModelVisitor implementation that resolves visual properties and sprite keys for selectable entities
 * (units/buildings).
 */
class SelectableVisitor<S extends Selectable<?>> extends ModelVisitor<S> {

    @Override
    public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<S> render_state) {
        Selectable<?> selectable = render_state.getModel();
        Race race = selectable.getOwnerNoCheck().getRaceInfo().getRaceType();
        if (selectable instanceof Unit unit) {
            return Optional.of(VisualRegistry.getInstance().getUnitSprite(race, unit.getTemplate()
                    .getVisualType()));
        } else if (selectable instanceof Building building) {
            BuildingType bvt = building.getTemplate().getBuildingType();
            var visuals = VisualRegistry.getInstance().getBuildingVisuals(race, bvt);
            return Optional.ofNullable(switch (building.getBuildStage()) {
                case UNPLACED -> null;
                case START -> visuals.start();
                case HALFBUILT -> visuals.halfbuilt();
                case BUILT -> visuals.built();
            });
        }
        return Optional.empty();
    }

    @Override
    public void getTransform(@NonNull ElementRenderState<S> render_state, @NonNull Matrix4f dest) {
        Model model = render_state.model;
        float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
        dest.translation(model.getPositionX(), model.getPositionY(), render_state.f).rotate(angle, 0f, 0f, 1f);
    }

    static Color.@NonNull Linear getTeamColor(@NonNull Selectable<?> model) {
        return model.getOwner().getColor();
    }

    @Override
    public final Color.@NonNull Linear getTeamColor(@NonNull ElementRenderState<S> render_state) {
        return getTeamColor(render_state.getModel());
    }

    @Override
    public final @NonNull Color getSelectionColor(@NonNull ElementRenderState<S> render_state) {
        Player local_player = render_state.render_state.getLocalPlayer();
        S model = render_state.getModel();
        return model.getSelectionColor(local_player, render_state.render_state.isSelected(model),
                render_state.render_state.isHovered(model));
    }

    @Override
    public final Selectable.@NonNull VisualPattern getPattern(@NonNull ElementRenderState<S> render_state) {
        Player local_player = render_state.render_state.getLocalPlayer();
        return render_state.getModel().getVisualPattern(local_player);
    }

    @Override
    public final void markDetailPoint(@NonNull ElementRenderState<S> render_state) {
        S selectable = render_state.model;
        if (!selectable.isDead())
            super.markDetailPoint(render_state);
    }
}
