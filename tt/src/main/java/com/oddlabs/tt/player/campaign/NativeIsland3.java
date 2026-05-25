package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.JumpDelegate;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Action;
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
import com.oddlabs.tt.trigger.campaign.MagicUsedTrigger;
import com.oddlabs.tt.trigger.campaign.NearPointTrigger;
import com.oddlabs.tt.trigger.campaign.TimeTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class NativeIsland3 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(NativeIsland3.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private int objective = 0;

    public NativeIsland3(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6).mapToObj(i -> i18n("name" + i)).toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(network, gui_root, 512, Landscape.TerrainType.VIKING, 1f, 1f, 0f,
                808208041, 3, NativeCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                RacesResources.RACE_NATIVES,
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                RacesResources.RACE_VIKINGS,
                1,
                true,
                PlayerSlot.AI_NEUTRAL_CAMPAIGN);
        game_network.getClient().setUnitInfo(2, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
        game_network.getClient().getServerInterface().setPlayerSlot(4,
                PlayerSlot.AI,
                RacesResources.RACE_VIKINGS,
                1,
                true,
                PlayerSlot.AI_PASSIVE_CAMPAIGN);
        game_network.getClient().setUnitInfo(4, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player local_player = getViewer().getLocalPlayer();
        final Player enemy = getViewer().getWorld().getPlayers()[1];
        final Player reinforcements = getViewer().getWorld().getPlayers()[2];

        final int start_x = 125 * 2;
        final int start_y = 222 * 2;
        final int thor_x = 40 * 2;
        final int thor_y = 40 * 2;

        // First reset camera direction and then move to rallypoint
        getViewer().getCamera().reset(start_x, start_y);
        getViewer().getCamera().setPos(thor_x, thor_y + 9);

        // Introduction
        final Runnable camera_jump = () -> getViewer().getGUIRoot().pushDelegate(new JumpDelegate(getViewer(),
                getViewer().getCamera(), start_x, start_y, 200f, 3f));
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START,
                    camera_jump);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), runnable);

        // Disable construction
        getViewer().getLocalPlayer().enableBuilding(Race.BUILDING_QUARTERS, false);
        getViewer().getLocalPlayer().enableBuilding(Race.BUILDING_ARMORY, false);
        getViewer().getLocalPlayer().enableBuilding(Race.BUILDING_TOWER, false);

        // Insert native men
        ResourceBundle player_bundle = ResourceBundle.getBundle(Player.class.getName());
        local_player.setActiveChieftain(new Unit(local_player, start_x, start_y, null,
                local_player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(player_bundle,
                        "native_chieftain_name"), false));
        local_player.getChieftain().increaseMagicEnergy(0, 1000);
        local_player.getChieftain().increaseMagicEnergy(1, 1000);
        // 5 peons
        for (int i = 0; i < 5; i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(Race.UNIT_PEON));
        }
        // rest as warriors
        int unit_count = getCampaign().getState().getNumPeons() + getCampaign().getState().getNumRockWarriors() + getCampaign().getState().getNumIronWarriors() + getCampaign().getState().getNumRubberWarriors() - 5;
        for (int i = 0; i < unit_count; i++) {
            if (getCampaign().getState().getDifficulty() == CampaignState.DIFFICULTY_EASY)
                new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(
                        Race.UNIT_WARRIOR_IRON));
            else
                new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(
                        Race.UNIT_WARRIOR_ROCK));
        }

        // Winner prize
        final Runnable prize = () -> {
            getCampaign().getState().setIslandState(3, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(4, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setHasMagic1(true);
            getCampaign().victory(getViewer());
        };

        // Ask for Stinking Stew
        final Runnable dialog8 = () -> {
            // Winning condition
            new MagicUsedTrigger(local_player.getChieftain(), thor_x, thor_y, 20, 0, prize);
            changeObjective(1);

            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header8"),
                    i18n("dialog8"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        };
        final Runnable dialog7 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header7"),
                    i18n("dialog7"),
                    getCampaign().getIcons().getFaces()[7],
                    Origin.AT_END,
                    dialog8);
            addModalForm(dialog);
        };
        final Runnable dialog6 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header6"),
                    i18n("dialog6"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START,
                    dialog7);
            addModalForm(dialog);
        };
        final Runnable dialog5 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header5"),
                    i18n("dialog5"),
                    getCampaign().getIcons().getFaces()[7],
                    Origin.AT_END,
                    dialog6);
            addModalForm(dialog);
        };
        final Runnable dialog4 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header4"),
                    i18n("dialog4"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START,
                    dialog5);
            addModalForm(dialog);
        };
        final Runnable dialog3 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header3"),
                    i18n("dialog3"),
                    getCampaign().getIcons().getFaces()[7],
                    Origin.AT_END,
                    dialog4);
            addModalForm(dialog);
        };
        final Runnable dialog2 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header2"),
                    i18n("dialog2"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START,
                    dialog3);
            addModalForm(dialog);
        };
        final Runnable dialog1 = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                    i18n("dialog1"),
                    getCampaign().getIcons().getFaces()[7],
                    Origin.AT_END,
                    dialog2);
            addModalForm(dialog);
            local_player.getChieftain().increaseMagicEnergy(0, 1000);
            local_player.getChieftain().increaseMagicEnergy(1, 1000);
        };
        new NearPointTrigger(thor_x / 2, thor_x / 2, 8, local_player.getChieftain(), dialog1);

        // Insert Thor
        float shadow_diameter = 4.5f;

        float dir = (float) Math.sin(Math.PI / 4);
        new SceneryModel(getViewer().getWorld(), thor_x, thor_y, dir, dir, enemy.getRace().getUnitTemplate(
                Race.UNIT_CHIEFTAIN).getSpriteRenderer(), shadow_diameter, true, i18n("god"), Unit.Animation.THOR, -1f,
                0f);


        // Insert reinforcements
        reinforcements.setStartX(126 * 2);
        reinforcements.setStartY(135 * 2);
        reinforcements.buildBuilding(Race.BUILDING_QUARTERS, 96, 145);
        reinforcements.buildBuilding(Race.BUILDING_ARMORY, 126, 135);
        for (int i = 0; i < 30; i++) {
            new Unit(reinforcements, 126 * 2, 135 * 2, null, reinforcements.getRace().getUnitTemplate(
                    Race.UNIT_WARRIOR_IRON));
        }

        // Insert native towers
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 60, 60);//*
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 110, 167);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 102, 127);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 67, 163);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 71, 189);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 92, 205);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 128, 186);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 191, 185);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 145, 128);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 173, 90);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 161, 82);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 120, 140);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 124, 127);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 135, 140);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 100, 139);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 80, 97);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 112, 76);

        // Scattered resistance
        new Unit(enemy, 180 * 2, 155 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 178 * 2, 145 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        new Unit(enemy, 178 * 2, 151 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 180 * 2, 153 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        new Unit(enemy, 103 * 2, 93 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 99 * 2, 99 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        new Unit(enemy, 113 * 2, 98 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 116 * 2, 98 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        new Unit(enemy, 83 * 2, 170 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 110 * 2, 199 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        new Unit(enemy, 186 * 2, 215 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 186 * 2, 211 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        new Unit(enemy, 53 * 2, 105 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 51 * 2, 107 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        new Unit(enemy, 53 * 2, 110 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        new Unit(enemy, 53 * 2, 101 * 2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));

        // Send reinforcements
        final Runnable reinforce = () -> {
            Unit unit = getWarrior(reinforcements);
            if (unit != null && !unit.isDead()) {
                Unit new_unit = changeOwner(unit, enemy);
                if (new_unit != null && !new_unit.isDead())
                    new_unit.setTarget(new LandscapeTarget(62, 62), Action.DEFAULT, true);
            }
        };
        float interval = switch (getCampaign().getState().getDifficulty()) {
            case CampaignState.DIFFICULTY_EASY -> 120f;
            case CampaignState.DIFFICULTY_NORMAL -> 60f;
            case CampaignState.DIFFICULTY_HARD -> 30f;
            default -> throw new RuntimeException("unrecognized difficulty");
        };
        float time = interval;
        for (int i = 0; i < 10; i++) {
            new TimeTrigger(getViewer().getWorld(), time, reinforce);
            time += interval;
        }
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
        return i18n("objective" + objective);
    }

    private void changeObjective(int objective) {
        this.objective = objective;
    }
}
