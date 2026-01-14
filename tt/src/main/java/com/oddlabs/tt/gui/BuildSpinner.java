package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.BuildProductionContainer;
import com.oddlabs.tt.model.BuildSupplyContainer;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.ThrowingWeapon;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BuildSpinner extends IconSpinner {
	public static final int INFINITE_LIMIT = 30;

	private final @NonNull PlayerInterface player_interface;

	private Building current_building;
	private Class<? extends ThrowingWeapon> type;
	private int num_orders;
	private int order_size;
	private boolean infinite;

	BuildSpinner(@NonNull WorldViewer viewer, @NonNull PlayerInterface player_interface,
                 @NonNull ModeIconQuads icon_quad, @NonNull String tool_tip, @NonNull IconQuad @Nullable [] tool_tip_icons,
                 @NonNull String shortcut_key) {
		super(viewer, icon_quad, tool_tip, tool_tip_icons, shortcut_key);
		this.player_interface = player_interface;
	}

	void setBuildSupplyContainer(@NonNull Building current_building, @NonNull Class<? extends ThrowingWeapon> type) {
		this.current_building = current_building;
		this.type = type;
		if (!current_building.isDead())
			num_orders = current_building.getBuildSupplyContainer(type).getNumOrders();
	}

	@Override
	public int computeCount() {
		if (!current_building.isDead()) {
			BuildSupplyContainer build_container = current_building.getBuildSupplyContainer(type);
			int count = Math.min(build_container.getMaxSupplyCount(),
					Math.max(0, build_container.getNumSupplies() + getOrderDiff()));
			infinite = count >= INFINITE_LIMIT;
			return count;
		} else
			return 0;
	}

	@Override
	public boolean renderInfinite() {
		return infinite;
	}

	private void order(int num) {
		if (!current_building.isDead()) {
			if (type == RockAxeWeapon.class) {
				player_interface.buildRockWeapons(current_building, num, infinite);
			} else if (type == IronAxeWeapon.class) {
				player_interface.buildIronWeapons(current_building, num, infinite);
			} else if (type == RubberAxeWeapon.class) {
				player_interface.buildRubberWeapons(current_building, num, infinite);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
		if (!isDisabled())
			super.appendToolTip(tool_tip_box);
	}

	@Override
	protected float getProgress() {
        return current_building.isDead()
                ? 0
                : ((BuildProductionContainer) current_building.getBuildSupplyContainer(type)).getBuildProgress();
	}

	Building getBuilding() {
		return current_building;
	}

	private int getOrderDiff() {
        return current_building.isDead()
                ? 0
                : num_orders - current_building.getBuildSupplyContainer(type).getNumOrders();
	}

	@Override
	protected void increase(int amount) {
		order_size += amount;
		num_orders += amount;
	}

	@Override
	protected void decrease(int amount) {
		order_size -= amount;
		num_orders -= amount;
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
}
