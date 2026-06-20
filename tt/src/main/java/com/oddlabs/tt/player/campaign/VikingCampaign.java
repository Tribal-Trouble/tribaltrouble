package com.oddlabs.tt.player.campaign;


import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.gui.CampaignIcons;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.VikingCampaignIcons;
import com.oddlabs.tt.steam.SteamManager;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.function.Function;
import java.util.stream.Stream;

public final class VikingCampaign extends Campaign {
    public static final int MAX_UNITS = 46;
    private static final int[] INITIAL_STATES = new int[]{
            /*
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE};
            */
            CampaignState.ISLAND_AVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_HIDDEN,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE};

    private final @NonNull Island[] islands = Stream.<Function<VikingCampaign, Island>>of(
            VikingIsland0::new, VikingIsland1::new, VikingIsland2::new, VikingIsland3::new, VikingIsland4::new,
            VikingIsland5::new, VikingIsland6::new, VikingIsland7::new, VikingIsland8::new, VikingIsland9::new,
            VikingIsland10::new, VikingIsland11::new, VikingIsland12::new, VikingIsland13::new, VikingIsland14::new)
            .map(c -> c.apply(this))
            .toArray(Island[]::new);

    public VikingCampaign(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        this(network, gui_root, new CampaignState(INITIAL_STATES));
    }

    public VikingCampaign(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root,
            @NonNull CampaignState campaign_state) {
        super(campaign_state);
        if (getState().getCurrentIsland() == -1) {
            startIsland(network, gui_root, 0);
        }
    }

    @Override
    public @NonNull CampaignIcons getIcons() {
        return VikingCampaignIcons.getIcons();
    }

    @Override
    public void islandChosen(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, int number) {
        Form dialog = new CampaignDialogForm(islands[number].getHeader(),
                islands[number].getDescription(),
                null,
                Origin.AT_START,
                () -> startIsland(network, gui_root, number), true);
        gui_root.addModalForm(dialog);
    }

    @Override
    public @NonNull CharSequence getCurrentObjective() {
        if (getState().getCurrentIsland() != -1) {
            return islands[getState().getCurrentIsland()].getCurrentObjective();
        }
        throw new IllegalStateException("No current island");
    }

    @Override
    public void defeated(@NonNull WorldViewer viewer, @NonNull String game_over_message) {
        if (getState().getCurrentIsland() == 13)
            ((VikingIsland13) islands[13]).removeCounter();
        super.defeated(viewer, game_over_message);
    }

    @Override
    public void startIsland(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, int number) {
        getState().setCurrentIsland(number);
        SteamManager.setCampaignRichPresence("Vikings");
        SteamManager.setInActiveWorld(true);
        islands[number].chosen(network, gui_root);
    }
}
