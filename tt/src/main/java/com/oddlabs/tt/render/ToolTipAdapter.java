package com.oddlabs.tt.render;

import com.oddlabs.tt.gui.GUIIcons;
import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.ModelToolTip;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.Controller;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.gui.ToolTip;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Adapter that maps simulation-side ModelToolTip entities to the UI ToolTip representation.
 */
final class ToolTipAdapter implements ToolTip {
    private final @NonNull ModelToolTip model;
    private final @NonNull Player local_player;

    ToolTipAdapter(@NonNull ModelToolTip model, @NonNull Player local_player) {
        this.local_player = local_player;
        this.model = model;
    }

    private void visitPlayer(@NonNull ToolTipBox tool_tip, @NonNull Player player) {
        tool_tip.append(player.getPlayerInfo().getName());
        tool_tip.append(" - ");
        //      tool_tip.append(team_tip);
        //      tool_tip.append(" ");
        //      if (Settings.getSettings().inDeveloperMode()) {
        //          tool_tip.append("total_units=");
        //          tool_tip.append(unit_count.getNumSupplies());
        //          tool_tip.append(" ");
        //      }
    }

    private void visitSelectable(@NonNull ToolTipBox tool_tip, @NonNull Selectable<?> selectable) {
        assert !selectable.isDead();
        visitPlayer(tool_tip, selectable.getOwner());
        /*      if (Settings.getSettings().developer_mode) {
        		if (getCurrentBehaviour() instanceof WalkBehaviour)
        		((WalkBehaviour)getCurrentBehaviour()).appendToolTip(tool_tip);
        		else*/
        //tool_tip.append(getPrimaryController().getClass().getName());
        //}
    }

    @Override
    public void appendToolTip(ToolTipBox tool_tip) {
        switch (model) {
            case Unit unit -> visitUnit(tool_tip, unit);
            case Building building -> visitBuilding(tool_tip, building);
            case Supply supply -> visitSupply(tool_tip, supply);
            case SceneryModel scenery -> visitSceneryModel(tool_tip, scenery);
            default -> {
            }
        }
    }

    private void visitSceneryModel(@NonNull ToolTipBox tool_tip, @NonNull SceneryModel model) {
        String name = model.getName();
        if (name != null)
            tool_tip.append(name);
    }

    private void visitSupply(@NonNull ToolTipBox tool_tip, @NonNull Supply supply) {
        tool_tip.append(supply.getName());
        tool_tip.append(GUIIcons.getIcons().getToolTipIcon(supply.getClass()));
    }

    private void visitBuilding(@NonNull ToolTipBox tool_tip, @NonNull Building building) {
        visitSelectable(tool_tip, building);
        tool_tip.append(building.getTemplate().getName());
        var health = (float) building.getHitPoints() / building.getTemplate().getMaxHitPoints();
        var watch = List.of(GUIIcons.getIcons().getWatch(health));
        tool_tip.append(watch);
        //      if (getUnitContainer() != null && Settings.getSettings().developer_mode) {
        //          tool_tip.append(" units_in_building ");
        //          tool_tip.append(getUnitContainer().getNumSupplies());
        //      }

    }

    private void visitUnit(@NonNull ToolTipBox tool_tip, @NonNull Unit unit) {
        visitSelectable(tool_tip, unit);
        String name = unit.getName();
        tool_tip.append(Objects.requireNonNullElseGet(name, () -> unit.getTemplate().getName()));
        Controller c = unit.getPrimaryController();
        if (unit.getAbilities().hasAbilities(Abilities.MAGIC)) {
            var health = (float) unit.getHitPoints() / unit.getTemplate().getMaxHitPoints();
            tool_tip.append(List.of(GUIIcons.getIcons().getWatch(health)));
        } else if (unit.getOwner() == local_player && c instanceof GatherController<?> gc) {
            tool_tip.append(GUIIcons.getIcons().getToolTipIcon(gc.getSupplyType()));
        }
        /*      if (getCurrentBehaviour() instanceof WalkBehaviour)
        		((WalkBehaviour)getCurrentBehaviour()).appendToolTip(tool_tip);*/

    }
}
