package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class NativeIsland7 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(NativeIsland7.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public NativeIsland7(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6).mapToObj(i -> i18n("name" + i)).toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(network, gui_root, 512, Landscape.TerrainType.VIKING, .75f, 1f, .75f,
                925, 7, NativeCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                RacesResources.RACE_NATIVES,
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0,
                new UnitInfo(false, false, 0, true,
                        getCampaign().getState().getNumPeons(),
                        getCampaign().getState().getNumRockWarriors(),
                        getCampaign().getState().getNumIronWarriors(),
                        getCampaign().getState().getNumRubberWarriors()));
        int ai_peons = switch (getCampaign().getState().getDifficulty()) {
            case CampaignState.DIFFICULTY_EASY -> 10;
            case CampaignState.DIFFICULTY_NORMAL -> 20;
            case CampaignState.DIFFICULTY_HARD -> 40;
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                RacesResources.RACE_VIKINGS,
                1,
                true,
                PlayerSlot.AI_HARD);
        game_network.getClient().setUnitInfo(2, new UnitInfo(false, false, 0, false, ai_peons, 0, 0, 0));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player enemy = getViewer().getWorld().getPlayers()[1];

        // Introduction
        final Runnable dialog1 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                    i18n("dialog1"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        };
        final Runnable dialog0 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[5],
                    Origin.AT_END,
                    dialog1);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), dialog0);

        // Winner prize
        runnable = () -> {
            getCampaign().getState().setIslandState(7, CampaignState.ISLAND_COMPLETED);
            getCampaign().victory(getViewer());
        };

        // Winning condition
        new VictoryTrigger(getViewer(), runnable);

        // Insert vikings
        ResourceBundle player_bundle = ResourceBundle.getBundle(Player.class.getName());
        enemy.setActiveChieftain(new Unit(enemy, 97 * 2, 60 * 2, null, enemy.getRace().getUnitTemplate(
                Race.UNIT_CHIEFTAIN), Utils.getBundleString(player_bundle, "chieftain_name"), false));
        enemy.buildBuilding(Race.BUILDING_QUARTERS, 105, 56);
        enemy.buildBuilding(Race.BUILDING_ARMORY, 108, 79);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 101, 64);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 90, 59);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 87, 70);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 93, 81);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 109, 89);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 115, 90);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 123, 93);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 132, 78);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 106, 118);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 112, 119);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 126, 117);

        // Insert treasures
        float shadow_diameter = 2.6f;

        float dir = (float) Math.sin(Math.PI / 4);
        Random r = new Random(42);
        float w = HeightMap.METERS_PER_UNIT_GRID;
        // From VikingIsland3
        new SceneryModel(getViewer().getWorld(), 98 * 2 + w * r.nextFloat(), 62 * 2 + w * r.nextFloat(), 0, -1,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 94 * 2 + w * r.nextFloat(), 67 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 83 * 2 + w * r.nextFloat(), 58 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 93 * 2 + w * r.nextFloat(), 49 * 2 + w * r.nextFloat(), -1, 0,
                getViewer().getWorld().getRacesResources().getTreasures()[5], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 97 * 2 + w * r.nextFloat(), 59 * 2 + w * r.nextFloat(), -dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 84 * 2 + w * r.nextFloat(), 61 * 2 + w * r.nextFloat(), dir, dir,
                getViewer().getWorld().getRacesResources().getTreasures()[4], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 96 * 2 + w * r.nextFloat(), 49 * 2 + w * r.nextFloat(), 1, 0,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 100 * 2 + w * r.nextFloat(), 49 * 2 + w * r.nextFloat(), dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[4], shadow_diameter, true, i18n("statue"));

        // From VikingIsland7
        new SceneryModel(getViewer().getWorld(), 84 * 2 + w * r.nextFloat(), 67 * 2 + w * r.nextFloat(), -1, 0,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 83 * 2 + w * r.nextFloat(), 64 * 2 + w * r.nextFloat(), -1, 0,
                getViewer().getWorld().getRacesResources().getTreasures()[4], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 95 * 2 + w * r.nextFloat(), 50 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 91 * 2 + w * r.nextFloat(), 63 * 2 + w * r.nextFloat(), dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 97 * 2 + w * r.nextFloat(), 50 * 2 + w * r.nextFloat(), dir, dir,
                getViewer().getWorld().getRacesResources().getTreasures()[4], shadow_diameter, true, i18n("statue"));

        new SceneryModel(getViewer().getWorld(), 93 * 2 + w * r.nextFloat(), 51 * 2 + w * r.nextFloat(), dir, dir,
                getViewer().getWorld().getRacesResources().getTreasures()[5], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 93 * 2 + w * r.nextFloat(), 65 * 2 + w * r.nextFloat(), dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 98 * 2 + w * r.nextFloat(), 54 * 2 + w * r.nextFloat(), dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 96 * 2 + w * r.nextFloat(), 51 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 96 * 2 + w * r.nextFloat(), 54 * 2 + w * r.nextFloat(), -1, 0,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 94 * 2 + w * r.nextFloat(), 52 * 2 + w * r.nextFloat(), -dir, dir,
                getViewer().getWorld().getRacesResources().getTreasures()[4], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 97 * 2 + w * r.nextFloat(), 56 * 2 + w * r.nextFloat(), 0, -1,
                getViewer().getWorld().getRacesResources().getTreasures()[5], shadow_diameter, true, i18n("statue"));

        // From VikingIsland14
        new SceneryModel(getViewer().getWorld(), 94 * 2 + w * r.nextFloat(), 59 * 2 + w * r.nextFloat(), -dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 91 * 2 + w * r.nextFloat(), 53 * 2 + w * r.nextFloat(), dir, dir,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 88 * 2 + w * r.nextFloat(), 57 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 93 * 2 + w * r.nextFloat(), 53 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[4], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 94 * 2 + w * r.nextFloat(), 53 * 2 + w * r.nextFloat(), 0, -1,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 90 * 2 + w * r.nextFloat(), 54 * 2 + w * r.nextFloat(), dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[5], shadow_diameter, true, i18n("statue"));

        new SceneryModel(getViewer().getWorld(), 91 * 2 + w * r.nextFloat(), 54 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 93 * 2 + w * r.nextFloat(), 61 * 2 + w * r.nextFloat(), -1, 0,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 93 * 2 + w * r.nextFloat(), 54 * 2 + w * r.nextFloat(), dir, -dir,
                getViewer().getWorld().getRacesResources().getTreasures()[5], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 94 * 2 + w * r.nextFloat(), 63 * 2 + w * r.nextFloat(), 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[1], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 90 * 2 + w * r.nextFloat(), 55 * 2 + w * r.nextFloat(), 1, 0,
                getViewer().getWorld().getRacesResources().getTreasures()[3], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 92 * 2 + w * r.nextFloat(), 55 * 2 + w * r.nextFloat(), -dir, dir,
                getViewer().getWorld().getRacesResources().getTreasures()[4], shadow_diameter, true, i18n("statue"));
        new SceneryModel(getViewer().getWorld(), 94 * 2 + w * r.nextFloat(), 55 * 2 + w * r.nextFloat(), 0, -1,
                getViewer().getWorld().getRacesResources().getTreasures()[5], shadow_diameter, true, i18n("statue"));

        shadow_diameter = 4.5f;
        float offset = HeightMap.METERS_PER_UNIT_GRID / 2f;
        new SceneryModel(getViewer().getWorld(), 91 * 2 + offset, 51 * 2 + offset, 0, 1,
                getViewer().getWorld().getRacesResources().getTreasures()[0], shadow_diameter, true, i18n("statue"));
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
