package com.oddlabs.tt.model;

public class ShipDeployContainer extends DeployContainer {
    private final Ship ship;
    private final boolean is_transfer;

    public ShipDeployContainer(
            Ship ship,
            float seconds_per_deploy,
            DeployType deploy_type,
            Class supply_type,
            boolean is_transfer) {
        super(ship, seconds_per_deploy, deploy_type, supply_type);
        this.ship = ship;
        this.is_transfer = is_transfer;
    }

    @Override
    public void orderSupply(int orders) {
        // Can we order that much? Or there's an UPPER limit?
        int capped_amount = capAmount(orders);
        // Do we have enough people for that? Or there's a LOWER limit?
        int result = -ship.getUnitContainer().capAmount(-capped_amount);
        // Do we have enough resources for that?
        if (supply_type != null) {
            result = -ship.getSupplyContainer(supply_type).capAmount(-result);
        }
        // If it's a transfer operation from a ship, we have to make sure these
        // units are peons. Warriors cannot transfer resources. And they cannot
        // be turned into peons at this point.
        if (!is_transfer) {
            int num_peons = ship.getShipHR().countPeons();
            if (result > 0) {
                result = StrictMath.min(num_peons, result);
            } else {
                result = StrictMath.max(-num_peons, result);
            }
        }
        if (result > 0) {
            if (supply_type != null) ship.getSupplyContainer(supply_type).prepareDeploy(result);
            ship.getUnitContainer().prepareDeploy(result);
            orderSupply(result, orders);
        } else {
            orderSupply(result, orders);
            ship.getUnitContainer().prepareDeploy(result);
            if (supply_type != null) {
                SupplyContainer supply_container = ship.getSupplyContainer(supply_type);
                if (!supply_container.isSupplyFull()) supply_container.prepareDeploy(result);
            }
        }
    }

    @Override
    protected void orderSupply(int amount, int orders) {
        increaseSupply(amount);
        this.num_orders += orders;
    }

    @Override
    public int increaseSupply(int amount) {
        int capped_amount = capAmount(amount);
        this.supply_count += capped_amount;
        return capped_amount;
    }
}
