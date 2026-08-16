package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployContainer;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class DeploySpinner extends IconSpinner {
    private final @NonNull PlayerInterface player_interface;
    private final @Nullable Player player;
    private final @Nullable Class<? extends Supply> gather_supply_type;
    private @Nullable Class<?> supply_type;
    private DeployType deploy_type;
    private Building current_building;
    private int num_orders = 0;
    private int order_size = 0;

    public DeploySpinner(@NonNull WorldViewer viewer, @NonNull PlayerInterface player_interface,
            @NonNull ModeIconQuads icon_quad, @NonNull String tool_tip,
            @Nullable List<@NonNull IconQuad> tool_tip_icons,
            @NonNull GameAction action, @NonNull GameAction dec_action, @Nullable Player player,
            @Nullable Class<? extends Supply> gather_supply_type) {
        super(viewer, icon_quad, tool_tip, tool_tip_icons, action, dec_action);
        this.player_interface = player_interface;
        this.player = player;
        this.gather_supply_type = gather_supply_type;
    }

    public void setContainers(@NonNull Building current_building, @NonNull DeployType deploy_type,
            @Nullable Class<?> supply_type) {
        this.current_building = current_building;
        this.deploy_type = deploy_type;
        this.supply_type = supply_type;
        if (!current_building.isDead())
            num_orders = current_building.getDeployContainer(deploy_type).getNumOrders();
    }

    @Override
    protected int getDisplayCount() {
        int count = computeCount();
        if (player != null && gather_supply_type != null) {
            count += player.getGathererCount(gather_supply_type, current_building);
        }
        return count;
    }

    @Override
    public int computeCount() {
        if (current_building != null && !current_building.isDead()) {
            DeployContainer deploy_container = current_building.getDeployContainer(deploy_type);
            return Math.min(deploy_container.getMaxSupplyCount(),
                    Math.max(0, deploy_container.getNumSupplies() + getOrderDiff()));
        } else
            return 0;
    }

    @Override
    public boolean renderInfinite() {
        return false;
    }

    public int getOrderDiff() {
        if (current_building != null && !current_building.isDead()) {
            return num_orders - current_building.getDeployContainer(deploy_type).getNumOrders();
        } else {
            return 0;
        }
    }

    private void order(int num) {
        if (!current_building.isDead())
            player_interface.deployUnits(current_building, deploy_type, num);
    }

    @Override
    protected void increase(int amount) {
        if (current_building.isDead()) {
            return;
        }

        if (current_building instanceof Ship ship) {
            // Handle ships case
            var hr = ship.getShipHR();
            int num_units;
            switch (deploy_type) {
                case DeployType.ROCK_WARRIOR, DeployType.IRON_WARRIOR, DeployType.RUBBER_WARRIOR -> {
                    num_units = hr.countUnitsOfType(supply_type);
                }
                case DeployType.PEON, DeployType.PEON_HARVEST_TREE, DeployType.PEON_HARVEST_ROCK,
                        DeployType.PEON_HARVEST_IRON, DeployType.PEON_HARVEST_RUBBER -> {
                    num_units = hr.countPeons();
                }
                default -> {
                    num_units = Math.min(hr.countPeons(), ship.getSupplyContainer(supply_type).getNumSupplies());
                }
            }
            num_units -= ship.getDeployContainer(deploy_type).getNumSupplies();
            if (order_size + amount > num_units) {
                amount = Math.max(0, num_units - order_size);
            }
            order_size += amount;
            num_orders += amount;
        } else {
            // Handle original building case
            int num_units = current_building.getUnitContainer().getNumSupplies();
            int num_supplies = Integer.MAX_VALUE;
            if (supply_type != null) {
                num_supplies = current_building.getSupplyContainer(supply_type).getNumSupplies();
            }

            if (num_units > getOrderDiff() && num_supplies > getOrderDiff()) {
                if (amount > num_units - getOrderDiff()) {
                    amount = num_units - getOrderDiff();
                }
                if (supply_type != null && amount > num_supplies - getOrderDiff()) {
                    amount = num_supplies - getOrderDiff();
                }
                order_size += amount;
                num_orders += amount;
            }
        }
    }

    @Override
    protected void decrease(int amount) {
        if (current_building.isDead()) return;

        if (computeCount() > 0) {
            int num_units = current_building.getDeployContainer(deploy_type).getNumSupplies();

            if (num_units > -getOrderDiff()) {
                if (amount > num_units + getOrderDiff()) {
                    amount = num_units + getOrderDiff();
                }
                order_size -= amount;
                num_orders -= amount;
            }
        } else if (player != null && gather_supply_type != null
                && player.getGathererCount(gather_supply_type, current_building) > 0) {
                    player_interface.recallGatherers(current_building, gather_supply_type, amount);
                }
    }

    @Override
    protected void release() {
        order(order_size);
        order_size = 0;
    }

    @Override
    protected int getOrderSize() {
        return order_size;
    }

    @Override
    protected float getProgress() {
        if (!current_building.isDead())
            return current_building.getDeployContainer(deploy_type).getBuildProgress();
        else
            return 0;
    }
}
