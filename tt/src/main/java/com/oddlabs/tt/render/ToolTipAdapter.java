package com.oddlabs.tt.render;

import com.oddlabs.tt.gui.GUIIcons;
import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.ModelToolTip;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.ToolTipVisitor;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.Controller;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

final class ToolTipAdapter implements ToolTipVisitor, ToolTip {
    private final ModelToolTip model;
    private final Player local_player;
    private ToolTipBox tool_tip_box;

    ToolTipAdapter(ModelToolTip model, Player local_player) {
        this.local_player = local_player;
        this.model = model;
    }

    private void visitPlayer(@NonNull Player player) {
        tool_tip_box.append(player.getPlayerInfo().getName());
        tool_tip_box.append(" - ");
        //      tool_tip_box.append(team_tip);
        //      tool_tip_box.append(" ");
        //      if (Settings.getSettings().inDeveloperMode()) {
        //          tool_tip_box.append("total_units=");
        //          tool_tip_box.append(unit_count.getNumSupplies());
        //          tool_tip_box.append(" ");
        //      }
    }

    private void visitSelectable(@NonNull Selectable<?> selectable) {
        assert !selectable.isDead();
        visitPlayer(selectable.getOwner());
        /*      if (Settings.getSettings().developer_mode) {
        		if (getCurrentBehaviour() instanceof WalkBehaviour)
        		((WalkBehaviour)getCurrentBehaviour()).appendToolTip(tool_tip_box);
        		else*/
        //tool_tip_box.append(getPrimaryController().getClass().getName());
        //}
    }

    @Override
    public void appendToolTip(ToolTipBox tool_tip) {
        tool_tip_box = tool_tip;
        model.visit(this);
    }

    @Override
    public void visitSceneryModel(@NonNull SceneryModel model) {
        String name = model.getName();
        if (name != null)
            tool_tip_box.append(name);
    }

    @Override
    public void visitSupply(@NonNull Supply model) {
        tool_tip_box.append(Utils.getBundleString(ResourceBundle.getBundle(model.getClass().getName()), "name"));
        tool_tip_box.append(GUIIcons.getIcons().getToolTipIcon(model.getClass()));
    }

    @Override
    public void visitBuilding(@NonNull Building building) {
        visitSelectable(building);
        tool_tip_box.append(building.getTemplate().getName());
        IconQuad[] watch = GUIIcons.getIcons().getWatch();
        tool_tip_box.append(
                watch[((watch.length - 1) * building.getHitPoints() / building.getTemplate().getMaxHitPoints())]);
        //      if (getUnitContainer() != null && Settings.getSettings().developer_mode) {
        //          tool_tip_box.append(" units_in_building ");
        //          tool_tip_box.append(getUnitContainer().getNumSupplies());
        //      }

    }

    @Override
    public void visitUnit(@NonNull Unit unit) {
        visitSelectable(unit);
        String name = unit.getName();
        if (name != null)
            tool_tip_box.append(name);
        else
            tool_tip_box.append(unit.getTemplate().getName());
        Controller c = unit.getPrimaryController();
        if (unit.getAbilities().hasAbilities(Abilities.MAGIC)) {
            IconQuad[] watch = GUIIcons.getIcons().getWatch();
            int hit_points = unit.getHitPoints();
            int index = ((watch.length - 1) * hit_points / unit.getTemplate().getMaxHitPoints());
            assert hit_points > 0 && hit_points <= unit.getTemplate().getMaxHitPoints() : "Invalid hit points";
            tool_tip_box.append(watch[index]);
        } else if (unit.getOwner() == local_player && c instanceof GatherController<?> gc) {
            tool_tip_box.append(GUIIcons.getIcons().getToolTipIcon(gc.getSupplyType()));
        }
        /*      if (getCurrentBehaviour() instanceof WalkBehaviour)
        		((WalkBehaviour)getCurrentBehaviour()).appendToolTip(tool_tip_box);*/

    }
}
