package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.campaign.Campaign;
import com.oddlabs.tt.trigger.IntervalTrigger;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class DefeatTrigger extends IntervalTrigger {
	private static final ResourceBundle bundle = ResourceBundle.getBundle(DefeatTrigger.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	private final Campaign campaign;
	private final Unit chieftain;
	private final Runnable runnable;
	private final @NonNull WorldViewer viewer;

	private boolean triggered_by_chieftain_death = false;

	public DefeatTrigger(@NonNull WorldViewer viewer, Campaign campaign, Unit chieftain) {
		this(viewer, campaign, chieftain, null);
	}

	public DefeatTrigger(@NonNull WorldViewer viewer, Campaign campaign, Unit chieftain, Runnable runnable) {
		super(viewer.getWorld(), .5f, 0f);
		this.viewer = viewer;
		this.campaign = campaign;
		this.chieftain = chieftain;
		this.runnable = runnable;
	}

	@Override
	protected void check() {
		Player current = viewer.getLocalPlayer();
		if (chieftain != current.getChieftain()) {
			triggered_by_chieftain_death = true;
			triggered();
		}

		int units = current.getUnitCountContainer().getNumSupplies();
		if (units == 0 && !current.hasActiveChieftain()) {
			triggered();
		}
	}

	@Override
	protected void done() {
		if (runnable == null) {
			String game_over_message = i18n(triggered_by_chieftain_death ? "defeat_by_chieftain" : "defeat");
            campaign.defeated(viewer, game_over_message);
		} else {
			runnable.run();
		}
	}
}
