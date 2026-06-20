package com.oddlabs.tt.player.campaign;

import com.oddlabs.tt.model.Race;

import com.oddlabs.tt.model.Difficulty;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import com.oddlabs.tt.render.VisualRegistry;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

/**
 * Campaign level logic for Viking Island 3, containing objectives and triggers.
 */
public final class VikingIsland3 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland3.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public VikingIsland3(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6)
                .mapToObj(i -> i18n("name" + i))
                .toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(network, gui_root, 512, Terrain.NATIVE, .75f, 1f, .5f,
                96443, 3, VikingCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                Race.VIKINGS.getValue(),
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0,
                new UnitInfo(false, false, 0, true,
                        getCampaign().getState().getNumPeons(),
                        getCampaign().getState().getNumRockWarriors(),
                        getCampaign().getState().getNumIronWarriors(),
                        getCampaign().getState().getNumRubberWarriors()));
        int ai_difficulty;
        int ai_peons = switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> {
                ai_difficulty = PlayerSlot.AI_NORMAL;
                yield 2;
            }
            case Difficulty.NORMAL -> {
                ai_difficulty = PlayerSlot.AI_HARD;
                yield 5;
            }
            case Difficulty.HARD -> {
                ai_difficulty = PlayerSlot.AI_HARD;
                yield 15;
            }
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                Race.NATIVES.getValue(),
                1,
                true,
                ai_difficulty);
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 1, false, ai_peons, 0, 5, 0));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player enemy = getViewer().getWorld().getPlayers().get(1);

        // Introduction
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), runnable);

        // Winner prize
        runnable = () -> {
            getCampaign().getState().setIslandState(3, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(2, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(4, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(8, CampaignState.ISLAND_AVAILABLE);
            getCampaign().victory(getViewer());
        };
        // Winning condition
        new VictoryTrigger(getViewer(), runnable);

        // Put warrior in tower
        enemy.getAI().ifPresent(ai -> ai.manTowers(1)); // TODO: exchange with insertGuardTower()

        // Insert treasures
        var treasures = VisualRegistry.getInstance().getTreasures();
        float shadow_diameter = 2.6f;

        float offset = HeightMap.METERS_PER_UNIT_GRID / 2f;
        float dir = (float) Math.sin(Math.PI / 4);
        new SceneryModel(getViewer().getWorld(), 134 * 2 + offset, 29 * 2 + offset, dir, -dir, treasures[4],
                shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 130 * 2 + offset, 28 * 2 + offset, 0, -1, treasures[1],
                shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 130 * 2 + offset, 34 * 2 + offset, 0, 1, treasures[3], shadow_diameter,
                true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 125 * 2 + offset, 37 * 2 + offset, 0, 1, treasures[1], shadow_diameter,
                true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 121 * 2 + offset, 32 * 2 + offset, -1, 0, treasures[5],
                shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 124 * 2 + offset, 28 * 2 + offset, -dir, -dir, treasures[3],
                shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 136 * 2 + offset, 38 * 2 + offset, dir, dir, treasures[4],
                shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 139 * 2 + offset, 33 * 2 + offset, 1, 0, treasures[1], shadow_diameter,
                true, i18n("statue"));
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
