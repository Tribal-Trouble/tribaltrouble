package com.oddlabs.tt.player.campaign;

import com.oddlabs.tt.model.Race;

import com.oddlabs.tt.model.Difficulty;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.PlayerEleminatedTrigger;
import com.oddlabs.tt.trigger.campaign.ReinforcementsTrigger;
import com.oddlabs.tt.trigger.campaign.TimeTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.model.Target;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class VikingIsland4 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland4.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public VikingIsland4(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6)
                .mapToObj(i -> i18n("name" + i))
                .toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(network, gui_root, 256, Terrain.NATIVE, .65f, 1f, .5f,
                786433, 4, VikingCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                Race.VIKINGS.getValue(),
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0, new UnitInfo(false, false, 0, true,
                getCampaign().getState().getNumPeons(),
                getCampaign().getState().getNumRockWarriors(),
                getCampaign().getState().getNumIronWarriors(),
                getCampaign().getState().getNumRubberWarriors()));
        game_network.getClient().getServerInterface().setPlayerSlot(1,
                PlayerSlot.AI,
                Race.VIKINGS.getValue(),
                PlayerInfo.TEAM_NEUTRAL,
                true,
                PlayerSlot.AI_NEUTRAL_CAMPAIGN);
        game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                Race.NATIVES.getValue(),
                1,
                true,
                PlayerSlot.AI_PASSIVE_CAMPAIGN);
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 2, true, 10, 10, 10, 10));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player local_player = getViewer().getLocalPlayer();
        final Player captive = getViewer().getWorld().getPlayers().get(1);
        final Player enemy = getViewer().getWorld().getPlayers().get(2);

        // Introduction
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
        };
        new GameStartedTrigger(getViewer().getWorld(), runnable);

        final int attack1;
        final int attack2;
        final int defense;
        switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> {
                attack1 = 4;
                attack2 = 8;
                defense = 10;
            }
            case Difficulty.NORMAL -> {
                attack1 = 7;
                attack2 = 12;
                defense = 12;
            }
            case Difficulty.HARD -> {
                attack1 = 11;
                attack2 = 16;
                defense = 20;
            }
            default -> throw new IllegalArgumentException("Unrecognized difficulty");
        }

        // Attack1
        Runnable attack1_runnable = () -> {
            local_player.getArmory().filter(a -> !a.isDead())
                    .map(a -> (Target) a)
                    .or(() -> local_player.getChieftain().filter(c -> !c.isDead()))
                    .ifPresent(target -> attack(enemy, target, attack1));
            refillArmory(enemy);
            deploy(enemy, attack2);
        };

        // Attack2
        Runnable attack2_runnable = () -> {
            local_player.getArmory().filter(a -> !a.isDead())
                    .map(a -> (Target) a)
                    .or(() -> local_player.getChieftain().filter(c -> !c.isDead()))
                    .ifPresent(target -> attack(enemy, target, attack2));
            refillArmory(enemy);
            deploy(enemy, defense);
        };
        switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> {
                new TimeTrigger(getViewer().getWorld(), 7f * 60f, attack1_runnable);
                new TimeTrigger(getViewer().getWorld(), 11f * 60f, attack2_runnable);
            }
            case Difficulty.NORMAL -> {
                new TimeTrigger(getViewer().getWorld(), 4.5f * 60f, attack1_runnable);
                new TimeTrigger(getViewer().getWorld(), 8f * 60f, attack2_runnable);
            }
            case Difficulty.HARD -> {
                new TimeTrigger(getViewer().getWorld(), 4f * 60f, attack1_runnable);
                new TimeTrigger(getViewer().getWorld(), 6.5f * 60f, attack2_runnable);
            }
            default -> throw new IllegalArgumentException("Unrecognized difficulty");
        }

        // Winner prize
        final Runnable prize = () -> {
            getCampaign().getState().setIslandState(4, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(5, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setHasMagic0(true);
            getCampaign().victory(getViewer());
        };

        // Winning condition
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                    i18n("dialog1"),
                    getCampaign().getIcons().getFaces()[5],
                    Origin.AT_END,
                    prize);
            addModalForm(dialog);
        };
        new VictoryTrigger(getViewer(), runnable);

        // Place prisoner
        placePrisoners(captive, enemy, 0, 0, 0, 0, true);

        // Put warrior in tower
        enemy.getAI().ifPresent(ai -> ai.manTowers(1)); // TODO: replace with insertGuardTower()

        // Fill native armory with units and weapons
        int num_reinforcements = 100;
        enemy.getArmory().ifPresent(armory -> {
            if (armory.getSupplyContainer(IronAxeWeapon.class).orElseThrow().getNumSupplies() < num_reinforcements)
                armory.getSupplyContainer(IronAxeWeapon.class).orElseThrow().increaseSupply(num_reinforcements);
            if (armory.getUnitContainer().orElseThrow().getNumSupplies() < num_reinforcements)
                armory.getUnitContainer().orElseThrow().increaseSupply(num_reinforcements);
        });

        // Deploy reinforcements when needed
        new ReinforcementsTrigger(enemy, DeployType.IRON_WARRIOR);

        // Defeat if netrauls eleminated
        runnable = () -> getCampaign().defeated(getViewer(), i18n("game_over"));
        new PlayerEleminatedTrigger(runnable, captive);
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
