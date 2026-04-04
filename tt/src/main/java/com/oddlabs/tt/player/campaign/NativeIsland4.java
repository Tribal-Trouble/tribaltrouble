package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.CounterLabel;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.PlayerEleminatedTrigger;
import com.oddlabs.tt.trigger.campaign.TimeTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class NativeIsland4 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(NativeIsland4.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final int minutes = 15;
    private final CounterLabel counter = new CounterLabel(minutes * 60f, Skin.getSkin().getHeadlineFont(), true);

    private boolean alive;

    public NativeIsland4(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6)
                .mapToObj(i -> i18n("name" + i))
                .toArray(String[]::new);
        GameNetwork game_network = startNewGame(network, gui_root, 512, Landscape.TerrainType.VIKING, .8f, .8f, .8f, 19 * 19, 4, NativeCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                RacesResources.RACE_NATIVES,
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0,
                new UnitInfo(false, false, 0, false,
                        0,//getCampaign().getState().getNumPeons(),
                        0,//getCampaign().getState().getNumRockWarriors(),
                        0,//getCampaign().getState().getNumIronWarriors(),
                        0));//getCampaign().getState().getNumRubberWarriors()));
        game_network.getClient().getServerInterface().setPlayerSlot(1,
                PlayerSlot.AI,
                RacesResources.RACE_NATIVES,
                0,
                true,
                PlayerSlot.AI_NEUTRAL_CAMPAIGN);
        game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                RacesResources.RACE_VIKINGS,
                1,
                true,
                PlayerSlot.AI_PASSIVE_CAMPAIGN);
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 0, false, 0, 10, 30, 0));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        alive = true;
        counter.start(getViewer().getWorld().getAnimationManagerGameTime());
        counter.setPos(0, 0);
        getViewer().getGUIRoot().addChild(counter);

        final Player local_player = getViewer().getLocalPlayer();
        final Player captives = getViewer().getWorld().getPlayers()[1];
        final Player enemy = getViewer().getWorld().getPlayers()[2];

        // Introduction
        new GameStartedTrigger(getViewer().getWorld(), () ->
                addModalForm(new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                        i18n("dialog0"),
                        getCampaign().getIcons().getFaces()[2],
                        Origin.AT_END,
                        () -> addModalForm(new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                                i18n("dialog1"),
                                getCampaign().getIcons().getFaces()[0],
                                Origin.AT_START)
                        ))
                )
        );

        // Move start position and insert men
        final int start_x = 45 * 2;
        final int start_y = 44 * 2;
        getViewer().getCamera().reset(start_x, start_y);
        ResourceBundle player_bundle = ResourceBundle.getBundle(Player.class.getName());
        local_player.setActiveChieftain(new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(player_bundle, "native_chieftain_name"), false));
        local_player.getChieftain().increaseMagicEnergy(0, 1000);
        local_player.getChieftain().increaseMagicEnergy(1, 1000);
        for (int i = 0; i < getCampaign().getState().getNumPeons(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(Race.UNIT_PEON));
        }
        for (int i = 0; i < getCampaign().getState().getNumRockWarriors(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
        }
        for (int i = 0; i < getCampaign().getState().getNumIronWarriors(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
        }
        for (int i = 0; i < getCampaign().getState().getNumRubberWarriors(); i++) {
            new Unit(local_player, start_x, start_y, null, local_player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
        }


        // Insert captives
        final int captive_x = 39;
        final int captive_y = 38;
        for (int i = 0; i < 10; i++) {
            new Unit(captives, captive_x * 2, captive_y * 2, null, captives.getRace().getUnitTemplate(Race.UNIT_PEON));
        }

        // Winning condition
        new TimeTrigger(getViewer().getWorld(), minutes * 60f, () -> {
            // Winner prize
            getCampaign().getState().setIslandState(4, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setNumPeons(getCampaign().getState().getNumPeons() + captives.getUnitCountContainer().getNumSupplies());
            if (isAlive()) {
                removeCounter();
            }
            getCampaign().victory(getViewer());
        });
/*
// done by DefeatTrigger in super
		// Remove counter if defeated
		runnable = new Runnable() {
			public final void run() {
				counter.remove();
			}
		};
		new DefeatTrigger(getCampaign(), local_player.getChieftain(), runnable);
*/
        // Insert native towers
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 125, 161);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 125, 150);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 136, 153);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 180, 184);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 158, 154);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 163, 177);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 173, 175);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 165, 167);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 104, 192);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 108, 185);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 103, 210);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 115, 205);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 155, 185);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 145, 171);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 108, 150);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 130, 189);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 82, 170);
        insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 65, 167);

        final int attack1;
        final int attack2;
        final int attack3;
        final int attack4;
        final int attack5;
        final int attack6;
        switch (getCampaign().getState().getDifficulty()) {
            case CampaignState.DIFFICULTY_EASY:
                attack1 = 5;
                attack2 = 15;
                attack3 = 20;
                attack4 = 35;
                attack5 = 35;
                attack6 = 35;
                break;
            case CampaignState.DIFFICULTY_NORMAL:
                attack1 = 10;
                attack2 = 30;
                attack3 = 40;
                attack4 = 70;
                attack5 = 70;
                attack6 = 70;
                break;
            case CampaignState.DIFFICULTY_HARD:
                attack1 = 20;
                attack2 = 60;
                attack3 = 80;
                attack4 = 90;
                attack5 = 90;
                attack6 = 90;
                break;
            default:
                throw new IllegalArgumentException("unexpected difficulty: " + getCampaign().getState().getDifficulty());
        }

        // Fill native armory with units and weapons
        refillArmory(enemy);

        // Attack1
        new TimeTrigger(getViewer().getWorld(), 3.5f * 60f, () -> {
            Building armory = local_player.getArmory();
            Unit chieftain = local_player.getChieftain();
            attack(enemy, new LandscapeTarget(captive_x, captive_y), attack1);
            if (armory != null && !armory.isDead()) {
                attack(enemy, armory, attack1);
            } else if (chieftain != null && !chieftain.isDead()) {
                attack(enemy, chieftain, attack1);
            }
            refillArmory(enemy);
            deploy(enemy, attack2);
        });

        // Attack2
        new TimeTrigger(getViewer().getWorld(), 4.5f * 60f, () -> {
            Building armory = local_player.getArmory();
            Unit chieftain = local_player.getChieftain();
            if (armory != null && !armory.isDead()) {
                attack(enemy, armory, attack2);
            } else if (chieftain != null && !chieftain.isDead()) {
                attack(enemy, chieftain, attack2);
            }
            refillArmory(enemy);
            deploy(enemy, attack3);
        });

        // Attack3
        new TimeTrigger(getViewer().getWorld(), 6 * 60f, () -> {
            Building armory = local_player.getArmory();
            Unit chieftain = local_player.getChieftain();
            if (armory != null && !armory.isDead()) {
                attack(enemy, armory, attack3);
            } else if (chieftain != null && !chieftain.isDead()) {
                attack(enemy, chieftain, attack3);
            }
            refillArmory(enemy);
            deploy(enemy, attack4);
        });

        // Attack4
        new TimeTrigger(getViewer().getWorld(), 9 * 60f, () -> {
            Building armory = local_player.getArmory();
            Unit chieftain = local_player.getChieftain();
            if (armory != null && !armory.isDead()) {
                attack(enemy, armory, attack4);
            } else if (chieftain != null && !chieftain.isDead()) {
                attack(enemy, chieftain, attack4);
            }
            refillArmory(enemy);
            deploy(enemy, attack5);
        });

        // Attack5
        new TimeTrigger(getViewer().getWorld(), 11 * 60f, () -> {
            Building armory = local_player.getArmory();
            Unit chieftain = local_player.getChieftain();
            if (armory != null && !armory.isDead()) {
                attack(enemy, armory, attack5);
            } else if (chieftain != null && !chieftain.isDead()) {
                attack(enemy, chieftain, attack5);
            }
            refillArmory(enemy);
            deploy(enemy, attack6);
        });

        // Attack6
        new TimeTrigger(getViewer().getWorld(), 12.5f * 60f, () -> {
            Building armory = local_player.getArmory();
            Unit chieftain = local_player.getChieftain();
            if (armory != null && !armory.isDead()) {
                attack(enemy, armory, attack6);
            } else if (chieftain != null && !chieftain.isDead()) {
                attack(enemy, chieftain, attack6);
            }
            refillArmory(enemy);
        });

        // Defeat if neutrals eliminated
        new PlayerEleminatedTrigger(() -> getCampaign().defeated(getViewer(), i18n("game_over")), captives);
    }

    private boolean isAlive() {
        return alive;
    }

    public void removeCounter() {
        alive = false;
        counter.remove();
        getViewer().getWorld().getAnimationManagerGameTime().removeAnimation(counter);
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
        return i18n("objective", minutes);
    }
}
