package com.oddlabs.tt.player.campaign;

import com.oddlabs.tt.model.Race;

import com.oddlabs.tt.model.Difficulty;

import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.model.UnitType;
import com.oddlabs.tt.model.MagicType;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.JumpDelegate;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.render.VisualRegistry;
import com.oddlabs.tt.trigger.campaign.DeathTrigger;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.MagicUsedTrigger;
import com.oddlabs.tt.trigger.campaign.NearArmyTrigger;
import com.oddlabs.tt.trigger.campaign.NearPointTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

/**
 * Campaign level logic for Viking Island 8, containing objectives and triggers.
 */
public final class VikingIsland8 extends Island {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland8.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private int objective = 0;

    public VikingIsland8(@NonNull Campaign campaign) {
        super(campaign);
    }

    @Override
    public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        String[] ai_names = IntStream.range(0, 6).mapToObj(i -> i18n("name" + i)).toArray(String[]::new);
        // gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
        GameNetwork game_network = startNewGame(network, gui_root, 1024, Terrain.NATIVE, 1f, 1f, 0f,
                285914281, 8, VikingCampaign.MAX_UNITS, ai_names);
        game_network.getClient().getServerInterface().setPlayerSlot(0,
                PlayerSlot.HUMAN,
                Race.VIKINGS.getValue(),
                0,
                true,
                PlayerSlot.AI_NONE);
        game_network.getClient().setUnitInfo(0, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
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
                PlayerSlot.AI_NEUTRAL_CAMPAIGN);
        game_network.getClient().setUnitInfo(2, new UnitInfo(false, false, 0, false, 0, 0, 0, 1));
        game_network.getClient().getServerInterface().startServer();
    }

    @Override
    protected void start() {
        final Player local_player = getViewer().getLocalPlayer();
        final Player lost = getViewer().getWorld().getPlayers().get(1);
        final Player enemy = getViewer().getWorld().getPlayers().get(2);

        // First reset camera direction and then move to rallypoint
        getViewer().getCamera().reset(170 * 2, 160 * 2);
        getViewer().getCamera().setPos(358 * 2, 484 * 2);

        // Introduction
        final Runnable camera_jump = () -> getViewer().getGUIRoot().pushDelegate(new JumpDelegate(getViewer(),
                getViewer().getCamera(), 170 * 2, 160 * 2, 200f, 3f));
        new GameStartedTrigger(getViewer().getWorld(), () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                    i18n("dialog0"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START,
                    camera_jump);
            addModalForm(dialog);
        });

        // Insert viking men
        ResourceBundle player_bundle = ResourceBundle.getBundle(Player.class.getName());
        local_player.setActiveChieftain(new Unit(local_player, 170 * 2, 160 * 2, null, local_player.getRaceInfo()
                .getUnitTemplate(UnitType.CHIEFTAIN), Utils.getBundleString(player_bundle, "chieftain_name"), false));
        local_player.getChieftain().ifPresent(chieftain -> chieftain.getOwner().getRaceInfo().getMagics().forEach(
                chieftain::maxMagicEnergy));
        int unit_count = getCampaign().getState().getNumPeons()
                + getCampaign().getState().getNumRockWarriors()
                + getCampaign().getState().getNumIronWarriors()
                + getCampaign().getState().getNumRubberWarriors();
        for (int i = 0; i < unit_count; i++) {
            new Unit(local_player, 170 * 2, 160 * 2, null, local_player.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
        }

        // Winner prize
        // Winning condition
        // See setMagicUsedTrigger()

        // Give blast when arrived
        new NearPointTrigger(354, 478, 4, local_player.getChieftain().orElseThrow(), () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                    i18n("dialog1"),
                    getCampaign().getIcons().getFaces()[0],
                    Origin.AT_START);
            addModalForm(dialog);
            getViewer().getLocalPlayer().enableMagic(MagicType.STUN, true);
            local_player.getChieftain().ifPresent(chieftain -> chieftain.getOwner().getRaceInfo().getMagics().forEach(
                    chieftain::maxMagicEnergy));
            setMagicUsedTrigger();
            changeObjective(1);
        });

        // Insert rally point
        new SceneryModel(getViewer().getWorld(), 354 * 2, 478 * 2, 0, -1, VisualRegistry.getInstance().getRallyPoint(
                local_player.getRaceInfo().getRaceType()));

        // Insert native towers
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 208, 210);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 124, 211);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 139, 223);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 171, 250);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 155, 244);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 68, 189);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 56, 187);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 44, 190);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 180, 124);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 224, 269);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 302, 370);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 312, 394);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 301, 388);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 253, 236);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 278, 316);
        insertGuardTower(enemy, UnitType.WARRIOR_ROCK, 261, 166);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 308, 467);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 192, 252);
        insertGuardTower(enemy, UnitType.WARRIOR_RUBBER, 203, 267);
        insertGuardTower(enemy, UnitType.WARRIOR_IRON, 316, 435);

        // Insert first blocking army
        Unit bait = new Unit(enemy, 240 * 2, 137 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        final Selectable<?>[] army = Selectable.newArray(
                new Unit(enemy, 213 * 2, 122 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER)),
                new Unit(enemy, 213 * 2, 122 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK)),
                new Unit(enemy, 213 * 2, 122 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 213 * 2, 122 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 213 * 2, 122 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 245 * 2, 170 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER)),
                new Unit(enemy, 245 * 2, 170 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER)),
                new Unit(enemy, 245 * 2, 170 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 245 * 2, 170 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 245 * 2, 170 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 239 * 2, 177 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER)),
                new Unit(enemy, 239 * 2, 177 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK)),
                new Unit(enemy, 239 * 2, 177 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 239 * 2, 177 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 239 * 2, 177 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 286 * 2, 113 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER)),
                new Unit(enemy, 286 * 2, 113 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK)),
                new Unit(enemy, 286 * 2, 113 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 286 * 2, 113 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 286 * 2, 113 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 289 * 2, 141 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER)),
                new Unit(enemy, 289 * 2, 141 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK)),
                new Unit(enemy, 289 * 2, 141 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 289 * 2, 141 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON)),
                new Unit(enemy, 289 * 2, 141 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON))
        );
        new DeathTrigger(bait, () -> enemy.setLandscapeTarget(army, 238, 136, Action.ATTACK, true));

        // Insert second blocking army
        Unit bait2 = new Unit(enemy, 347 * 2, 455 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                UnitType.WARRIOR_ROCK));
        int num_hidden = switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> 7;
            case Difficulty.NORMAL -> 13;
            case Difficulty.HARD -> 20;
            default -> throw new IllegalArgumentException("Unrecognized difficulty");
        };
        final Selectable<?>[] army2 = Selectable.newArray(num_hidden);
        army2[0] = new Unit(enemy, 364 * 2, 440 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        army2[1] = new Unit(enemy, 364 * 2, 440 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        army2[2] = new Unit(enemy, 364 * 2, 440 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        army2[3] = new Unit(enemy, 364 * 2, 440 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        army2[4] = new Unit(enemy, 364 * 2, 440 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                UnitType.WARRIOR_RUBBER));
        army2[5] = new Unit(enemy, 364 * 2, 440 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                UnitType.WARRIOR_RUBBER));
        army2[6] = new Unit(enemy, 364 * 2, 440 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                UnitType.WARRIOR_RUBBER));

        if (getCampaign().getState().getDifficulty() == Difficulty.HARD || getCampaign().getState()
                .getDifficulty() == Difficulty.NORMAL) {
            army2[7] = new Unit(enemy, 365 * 2, 427 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
            army2[8] = new Unit(enemy, 365 * 2, 427 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
            army2[9] = new Unit(enemy, 365 * 2, 427 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
            army2[10] = new Unit(enemy, 365 * 2, 427 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
            army2[11] = new Unit(enemy, 365 * 2, 427 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
            army2[12] = new Unit(enemy, 365 * 2, 427 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
        }
        if (getCampaign().getState().getDifficulty() == Difficulty.HARD) {
            army2[13] = new Unit(enemy, 366 * 2, 419 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
            army2[14] = new Unit(enemy, 366 * 2, 419 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
            army2[15] = new Unit(enemy, 366 * 2, 419 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
            army2[16] = new Unit(enemy, 366 * 2, 419 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
            army2[17] = new Unit(enemy, 366 * 2, 419 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
            army2[18] = new Unit(enemy, 366 * 2, 419 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
            army2[19] = new Unit(enemy, 366 * 2, 419 * 2, null, enemy.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
        }
        new DeathTrigger(bait2, () -> enemy.setLandscapeTarget(army2, 352, 480, Action.ATTACK, true));

        // Insert scattered resistance
        new Unit(enemy, 348 * 2, 315 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 348 * 2, 315 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 348 * 2, 315 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 348 * 2, 315 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));

        new Unit(enemy, 299 * 2, 321 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 299 * 2, 321 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 299 * 2, 321 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));

        new Unit(enemy, 300 * 2, 453 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 300 * 2, 453 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        new Unit(enemy, 300 * 2, 453 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 300 * 2, 453 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 300 * 2, 453 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 300 * 2, 453 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 300 * 2, 453 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));

        new Unit(enemy, 352 * 2, 456 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 355 * 2, 459 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 360 * 2, 461 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 365 * 2, 466 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 367 * 2, 474 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 369 * 2, 479 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 348 * 2, 455 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 342 * 2, 459 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 335 * 2, 467 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 334 * 2, 475 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 345 * 2, 465 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 346 * 2, 460 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 347 * 2, 467 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));

        new Unit(enemy, 331 * 2, 374 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 331 * 2, 374 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 331 * 2, 374 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 331 * 2, 374 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));

        new Unit(enemy, 348 * 2, 371 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 348 * 2, 371 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));

        new Unit(enemy, 399 * 2, 399 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 399 * 2, 399 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 399 * 2, 399 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 399 * 2, 399 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 399 * 2, 399 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));

        new Unit(enemy, 354 * 2, 474 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 354 * 2, 474 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        new Unit(enemy, 354 * 2, 474 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 354 * 2, 474 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 354 * 2, 474 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
        new Unit(enemy, 354 * 2, 474 * 2, null, enemy.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));

        // Insert Neutral units
        int num_neutrals = switch (getCampaign().getState().getDifficulty()) {
            case Difficulty.EASY -> 15;
            case Difficulty.NORMAL -> 9;
            case Difficulty.HARD -> 6;
            default -> throw new IllegalArgumentException();
        };
        final Unit[] neutrals = new Unit[num_neutrals];
        neutrals[0] = new Unit(lost, 267 * 2, 325 * 2, null, lost.getRaceInfo().getUnitTemplate(
                UnitType.WARRIOR_RUBBER));
        neutrals[1] = new Unit(lost, 268 * 2, 324 * 2, null, lost.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        neutrals[2] = new Unit(lost, 267 * 2, 323 * 2, null, lost.getRaceInfo().getUnitTemplate(
                UnitType.WARRIOR_RUBBER));
        neutrals[3] = new Unit(lost, 265 * 2, 325 * 2, null, lost.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        neutrals[4] = new Unit(lost, 265 * 2, 324 * 2, null, lost.getRaceInfo().getUnitTemplate(
                UnitType.WARRIOR_RUBBER));
        neutrals[5] = new Unit(lost, 266 * 2, 326 * 2, null, lost.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
        if (getCampaign().getState().getDifficulty() == Difficulty.EASY || getCampaign().getState()
                .getDifficulty() == Difficulty.NORMAL) {
            neutrals[6] = new Unit(lost, 267 * 2, 327 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
            neutrals[7] = new Unit(lost, 269 * 2, 327 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
            neutrals[8] = new Unit(lost, 270 * 2, 327 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
        }
        if (getCampaign().getState().getDifficulty() == Difficulty.EASY) {
            neutrals[9] = new Unit(lost, 268 * 2, 326 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
            neutrals[10] = new Unit(lost, 271 * 2, 325 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
            neutrals[11] = new Unit(lost, 270 * 2, 324 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
            neutrals[12] = new Unit(lost, 271 * 2, 328 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
            neutrals[13] = new Unit(lost, 269 * 2, 330 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
            neutrals[14] = new Unit(lost, 272 * 2, 323 * 2, null, lost.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
        }
        new NearArmyTrigger(neutrals, 10f, local_player, () -> {
            CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header2"),
                    i18n("dialog2"),
                    getCampaign().getIcons().getFaces()[4],
                    Origin.AT_END);
            addModalForm(dialog);
            for (Unit neutral : neutrals) {
                if (!neutral.isDead()) {
                    changeOwner(neutral, local_player);
                }
            }
        });
    }

    private void setMagicUsedTrigger() {
        Runnable runnable = () -> {
            getCampaign().getState().setIslandState(8, CampaignState.ISLAND_COMPLETED);
            getCampaign().getState().setIslandState(7, CampaignState.ISLAND_AVAILABLE);
            getCampaign().getState().setIslandState(9, CampaignState.ISLAND_SEMI_AVAILABLE);
            getCampaign().getState().setIslandState(11, CampaignState.ISLAND_SEMI_AVAILABLE);
            getCampaign().victory(getViewer());
        };

        new MagicUsedTrigger(getViewer().getLocalPlayer().getChieftain().orElseThrow(), 354 * 2, 478 * 2, 15,
                MagicType.STUN,
                runnable);
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
