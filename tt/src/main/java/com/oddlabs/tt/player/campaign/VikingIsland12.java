package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.PlayerEleminatedTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class VikingIsland12 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland12.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public VikingIsland12(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6).mapToObj(i -> i18n("name" + i)).toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(network, gui_root, 256, Landscape.TerrainType.NATIVE, .5f, 1f, .57f,
                67625656, 12, VikingCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                RacesResources.RACE_VIKINGS,
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0,
                new UnitInfo(false, false, 0, true,
                        getCampaign().getState().getNumPeons(),
                        getCampaign().getState().getNumRockWarriors(),
                        getCampaign().getState().getNumIronWarriors(),
                        getCampaign().getState().getNumRubberWarriors()));
        game_network.getClient().getServerInterface().setPlayerSlot(1,
                PlayerSlot.AI,
                RacesResources.RACE_VIKINGS,
                0,
                true,
                PlayerSlot.AI_NEUTRAL_CAMPAIGN);
        game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
        int ai_peons = switch (getCampaign().getState().getDifficulty()) {
            case CampaignState.DIFFICULTY_EASY -> 10;
            case CampaignState.DIFFICULTY_NORMAL -> 20;
            case CampaignState.DIFFICULTY_HARD -> 40;
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                RacesResources.RACE_NATIVES,
                1,
                true,
                PlayerSlot.AI_HARD);
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 0, true, ai_peons, 0, 0, 0));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player local_player = getViewer().getLocalPlayer();
        final Player stranded = getViewer().getWorld().getPlayers()[1];
        final Player enemy = getViewer().getWorld().getPlayers()[2];

        // Introduction
        final Runnable answer = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        };
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                    i18n("dialog1"),
                    getCampaign().getIcons().getFaces()[2],
                    Origin.AT_END,
                    answer);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), runnable);

        // Place prisoners
        placePrisoners(stranded, local_player, 10, 0, 0, 0, false);

        // Defeat if netrauls eleminated
        runnable = () -> getCampaign().defeated(getViewer(), i18n("game_over"));
        new PlayerEleminatedTrigger(runnable, stranded);

        // Put warrior in tower
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 39, 43);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 35, 53);

        // Winner prize
        final Runnable prize = () -> {
            getCampaign().getState().setIslandState(12, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(11, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(13, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setNumPeons(
                    getCampaign().getState().getNumPeons() + stranded.getUnitCountContainer().getNumSupplies());
            getCampaign().victory(getViewer());
        };
        runnable = () -> {
            String new_units = i18n("new_units", stranded.getUnitCountContainer().getNumSupplies());
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("new_units_header"),
                    new_units,
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START,
                    prize);
            addModalForm(dialog);
        };

        // Winning condition
        new VictoryTrigger(getViewer(), runnable);
    }

    @Override
    public @NonNull CharSequence getHeader() {
        return i18n("header");
    }

    @Override
    public @NonNull CharSequence getDescription() {
        return i18n("description");
    }

    @Override
    public @NonNull CharSequence getCurrentObjective() {
        return i18n("objective");
    }
}
