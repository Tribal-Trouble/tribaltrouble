package com.oddlabs.tt.player.campaign;


import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.gui.CampaignIcons;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.NativeCampaignIcons;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.steam.SteamManager;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.function.Function;
import java.util.stream.Stream;

public final class NativeCampaign extends Campaign {
    public static final int MAX_UNITS = 41;
    private static final int[] INITIAL_STATES = new int[]{
            /*
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
            CampaignState.ISLAND_HIDDEN};

    private final @NonNull Island[] islands = Stream.<Function<Campaign, Island>>of(
            NativeIsland0::new, NativeIsland1::new, NativeIsland2::new, NativeIsland3::new,
            NativeIsland4::new, NativeIsland5::new, NativeIsland6::new, NativeIsland7::new)
            .map(constructor -> constructor.apply(this))
            .toArray(Island[]::new);

    public NativeCampaign(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        this(network, gui_root, new CampaignState(INITIAL_STATES));
    }

    public NativeCampaign(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, CampaignState campaign_state) {
        super(campaign_state);

        if (getState().getCurrentIsland() == -1) {
            startIsland(network, gui_root, 0);
        }
        getState().setHasMagic0(true);
        getState().setHasRubberWeapons(true);
    }

    @Override
    public @NonNull CampaignIcons getIcons() {
        return NativeCampaignIcons.getIcons();
    }

    @Override
    public void islandChosen(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, int number) {
        if (Renderer.isRegistered()) {
            Form dialog = new CampaignDialogForm(islands[number].getHeader(),
                    islands[number].getDescription(),
                    null,
                    Origin.AT_START,
                    () -> startIsland(network, gui_root, number), true);
            gui_root.addModalForm(dialog);
        }
    }

    @Override
    public @NonNull CharSequence getCurrentObjective() {
        if (getState().getCurrentIsland() != -1) {
            return islands[getState().getCurrentIsland()].getCurrentObjective();
        }
        throw new IllegalArgumentException("No current island");
    }

    @Override
    public void defeated(@NonNull WorldViewer viewer, @NonNull String game_over_message) {
        if (getState().getCurrentIsland() == 4)
            ((NativeIsland4) islands[4]).removeCounter();
        super.defeated(viewer, game_over_message);
    }

    @Override
    public void startIsland(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, int number) {
        getState().setCurrentIsland(number);
        SteamManager.setCampaignRichPresence("Natives");
        SteamManager.setInActiveWorld(true);
        islands[number].chosen(network, gui_root);
    }
}
