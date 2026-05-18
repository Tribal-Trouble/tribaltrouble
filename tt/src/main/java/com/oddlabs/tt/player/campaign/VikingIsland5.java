package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
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

public final class VikingIsland5 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland5.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public VikingIsland5(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6).mapToObj(i -> i18n("name" + i)).toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(network, gui_root, 512, Landscape.TerrainType.NATIVE, .85f, 1f, .9f,
                89864, 5, VikingCampaign.MAX_UNITS, ai_names);
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
                PlayerSlot.AI_HARD);
        game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 25, 5, 0, 0));

        int ai_peons = switch (getCampaign().getState().getDifficulty()) {
            case CampaignState.DIFFICULTY_EASY -> 5;
            case CampaignState.DIFFICULTY_NORMAL -> 10;
            case CampaignState.DIFFICULTY_HARD -> 25;
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                RacesResources.RACE_NATIVES,
                1,
                true,
                PlayerSlot.AI_HARD);
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, false, 1, false, ai_peons, 0, 0, 1));
        game_network.getClient().getServerInterface().setPlayerSlot(3,
                PlayerSlot.AI,
                RacesResources.RACE_NATIVES,
                1,
                true,
                PlayerSlot.AI_HARD);
        game_network.getClient().setUnitInfo(3, new UnitInfo(true, false, 1, false, ai_peons, 0, 0, 1));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player enemy0 = getViewer().getWorld().getPlayers()[2];
        final Player enemy1 = getViewer().getWorld().getPlayers()[3];

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
                    getCampaign().getIcons().getFaces()[5],
                    Origin.AT_END,
                    answer);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), runnable);

        // Winner prize
        final Runnable prize = () -> {
            getCampaign().getState().setIslandState(5, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(4, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(6, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setNumRockWarriors(getCampaign().getState().getNumRockWarriors() + 5);
            getCampaign().victory(getViewer());
        };

        // Winning condition
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header2"),
                    i18n("dialog2"),
                    getCampaign().getIcons().getFaces()[5],
                    Origin.AT_END,
                    prize);
            addModalForm(dialog);
        };
        new VictoryTrigger(getViewer(), runnable);

        // Put warrior in tower
        enemy0.getAI().manTowers(1); // TODO: replace with insertGuardTower()
        enemy1.getAI().manTowers(1); // TODO: replace with insertGuardTower()

        // Defeat if friends eleminated
        runnable = () -> getCampaign().defeated(getViewer(), i18n("game_over"));
        new PlayerEleminatedTrigger(runnable, getViewer().getWorld().getPlayers()[1]);
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
