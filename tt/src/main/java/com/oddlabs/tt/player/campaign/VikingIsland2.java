package com.oddlabs.tt.player.campaign;

import com.oddlabs.tt.model.Race;

import com.oddlabs.tt.model.Difficulty;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.TimeTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.model.Target;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class VikingIsland2 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland2.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public VikingIsland2(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6)
                .mapToObj(i -> i18n("name" + i))
                .toArray(String[]::new);
        GameNetwork game_network = startNewGame(network, gui_root, 256, Terrain.NATIVE, .65f, 1f, .7f,
                447363, 2, VikingCampaign.MAX_UNITS, ai_names);
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
        game_network.getClient().getServerInterface().setPlayerSlot(2,
                PlayerSlot.AI,
                Race.NATIVES.getValue(),
                1,
                true,
                PlayerSlot.AI_PASSIVE_CAMPAIGN);
        int ai_units = switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> 10;
            case Difficulty.NORMAL -> 20;
            case Difficulty.HARD -> 30;
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 0, false, 0, 0, 0, ai_units));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        Runnable runnable;
        final Player local_player = getViewer().getLocalPlayer();
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
        final Runnable prize = () -> {
            getCampaign().getState().setIslandState(2, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(1, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(3, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setHasRubberWeapons(true);
            getCampaign().victory(getViewer());
        };

        // Winning condition
        runnable = () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                    i18n("dialog1"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START,
                    prize);
            addModalForm(dialog);
        };
        new VictoryTrigger(getViewer(), runnable);

        final int attack1;
        final int attack2;
        final int defense;
        switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> {
                attack1 = 3;
                attack2 = 6;
                defense = 10;
            }
            case Difficulty.NORMAL -> {
                attack1 = 5;
                attack2 = 10;
                defense = 10;
            }
            case Difficulty.HARD -> {
                attack1 = 7;
                attack2 = 13;
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
                new TimeTrigger(getViewer().getWorld(), 10f * 60f, attack1_runnable);
                new TimeTrigger(getViewer().getWorld(), 27f * 60f, attack2_runnable);
            }
            case Difficulty.NORMAL -> {
                new TimeTrigger(getViewer().getWorld(), 6f * 60f, attack1_runnable);
                new TimeTrigger(getViewer().getWorld(), 9f * 60f, attack2_runnable);
            }
            case Difficulty.HARD -> {
                new TimeTrigger(getViewer().getWorld(), 4.5f * 60f, attack1_runnable);
                new TimeTrigger(getViewer().getWorld(), 7.5f * 60f, attack2_runnable);
            }
            default -> throw new IllegalArgumentException("Unrecognized difficulty");
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
        return i18n("objective");
    }
}
