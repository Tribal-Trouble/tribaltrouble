package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.JumpDelegate;
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
import com.oddlabs.tt.trigger.campaign.MagicUsedTrigger;
import com.oddlabs.tt.trigger.campaign.NearPointTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class VikingIsland10 extends Island {
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland10.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	private int objective = 0;
	
	public VikingIsland10(@NonNull Campaign campaign) {
		super(campaign);
	}

	@Override
	public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
		String[] ai_names = IntStream.range(0,6)
				.mapToObj(i -> i18n( "name" + i))
				.toArray(String[]::new);
		// gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
		GameNetwork game_network = startNewGame(network, gui_root, 512, Landscape.TerrainType.NATIVE, 1f, 1f, 0f, -1442873271, 10, VikingCampaign.MAX_UNITS, ai_names);
		game_network.getClient().getServerInterface().setPlayerSlot(0,
				PlayerSlot.HUMAN,
				RacesResources.RACE_VIKINGS,
				0,
				true,
				PlayerSlot.AI_NONE);
		game_network.getClient().setUnitInfo(0, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
		game_network.getClient().getServerInterface().setPlayerSlot(2,
				PlayerSlot.AI,
				RacesResources.RACE_NATIVES,
				1,
				true,
				PlayerSlot.AI_NEUTRAL_CAMPAIGN);
		game_network.getClient().setUnitInfo(2, new UnitInfo(false, false, 0, false, 0, 0, 0, 1));
		game_network.getClient().getServerInterface().startServer();
	}

	@Override
	protected void start() {
		Runnable runnable;
		final Player local_player = getViewer().getLocalPlayer();
		final Player enemy = getViewer().getWorld().getPlayers()[1];

		// First reset camera direction and then move to rallypoint
		getViewer().getCamera().reset(142*2, 182*2);
		getViewer().getCamera().setPos(177*2, 156*2);

		// Introduction
		final Runnable camera_jump = () -> getViewer().getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), getViewer().getCamera(), 142*2, 182*2, 200f, 3f));
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

		// Insert viking men
		ResourceBundle player_bundle = ResourceBundle.getBundle(Player.class.getName());
		local_player.setActiveChieftain(new Unit(local_player, 142*2, 182*2, null, local_player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(player_bundle, "chieftain_name"), false));
		local_player.getChieftain().increaseMagicEnergy(0, 1000);
		local_player.getChieftain().increaseMagicEnergy(1, 1000);
		// 5 peons
		for (int i = 0; i < 5; i++) {
			new Unit(local_player, 142*2, 182*2, null, local_player.getRace().getUnitTemplate(Race.UNIT_PEON));
		}
		// rest as warriors
		int unit_count = getCampaign().getState().getNumPeons()
			+ getCampaign().getState().getNumRockWarriors()
			+ getCampaign().getState().getNumIronWarriors()
			+ getCampaign().getState().getNumRubberWarriors() - 5;
		for (int i = 0; i < unit_count; i++) {
			if (getCampaign().getState().getDifficulty() == CampaignState.DIFFICULTY_EASY)
				new Unit(local_player, 142*2, 182*2, null, local_player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
			else 
				new Unit(local_player, 142*2, 182*2, null, local_player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		}

		// Winner prize
		runnable = () -> {
                    getCampaign().getState().setIslandState(10, CampaignState.ISLAND_COMPLETED);
                    getCampaign().getState().setIslandState(13, CampaignState.ISLAND_AVAILABLE);
                    getCampaign().getState().setHasMagic1(true);
                    getCampaign().victory(getViewer());
                };

		// Winning condition
		new MagicUsedTrigger(local_player.getChieftain(), 173*2, 153*2, 7, 1, runnable);

		// Give blast when arrived
		final Runnable dialog11 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header11"),
                            i18n("dialog11"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START);
                    addModalForm(dialog);
                    changeObjective(1);
                };
		final Runnable dialog10 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header10"),
                            i18n("dialog10"),
                            getCampaign().getIcons().getFaces()[8],
                            Origin.AT_END,
                            dialog11);
                    addModalForm(dialog);
                };
		final Runnable dialog9 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header9"),
                            i18n("dialog9"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog10);
                    addModalForm(dialog);
                };
		final Runnable dialog8 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header8"),
                            i18n("dialog8"),
                            getCampaign().getIcons().getFaces()[8],
                            Origin.AT_END,
                            dialog9);
                    addModalForm(dialog);
                };
		final Runnable dialog7 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header7"),
                            i18n("dialog7"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog8);
                    addModalForm(dialog);
                };
		final Runnable dialog6 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header6"),
                            i18n("dialog6"),
                            getCampaign().getIcons().getFaces()[8],
                            Origin.AT_END,
                            dialog7);
                    addModalForm(dialog);
                };
		final Runnable dialog5 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header5"),
                            i18n("dialog5"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog6);
                    addModalForm(dialog);
                };
		final Runnable dialog4 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header4"),
                            i18n("dialog4"),
                            getCampaign().getIcons().getFaces()[8],
                            Origin.AT_END,
                            dialog5);
                    addModalForm(dialog);
                };
		final Runnable dialog3 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header3"),
                            i18n("dialog3"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog4);
                    addModalForm(dialog);
                };
		final Runnable dialog2 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header2"),
                            i18n("dialog2"),
                            getCampaign().getIcons().getFaces()[8],
                            Origin.AT_END,
                            dialog3);
                    addModalForm(dialog);
                };
		final Runnable dialog1 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                            i18n("dialog1"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog2);
                    addModalForm(dialog);
                    getViewer().getLocalPlayer().enableMagic(1, true);
                    local_player.getChieftain().increaseMagicEnergy(0, 1000);
                    local_player.getChieftain().increaseMagicEnergy(1, 1000);
                };
		new NearPointTrigger(173, 153, 3, local_player.getChieftain(), dialog1);

		// Insert statue
		float shadow_diameter = 2.6f;
		float offset = HeightMap.METERS_PER_UNIT_GRID/2f;
		new SceneryModel(getViewer().getWorld(), 173*2 + offset, 153*2 + offset, 0, 1, getViewer().getWorld().getRacesResources().getTreasures()[2], shadow_diameter, true, i18n("statue"));

		// Insert native towers
		insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 177, 159);//*
		insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 180, 176);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 165, 195);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 169, 198);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 152, 209);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 200, 197);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_IRON, 199, 169);

		// Blocking army
		new Unit(enemy, 173*2, 188*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 173*2, 188*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 173*2, 188*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 173*2, 188*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 173*2, 188*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 175*2, 190*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 175*2, 190*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 175*2, 190*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 175*2, 190*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 175*2, 190*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 178*2, 192*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 178*2, 192*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 178*2, 192*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 178*2, 192*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 178*2, 192*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 185*2, 194*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 185*2, 194*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 185*2, 194*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 185*2, 194*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 185*2, 194*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 181*2, 195*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		new Unit(enemy, 164*2, 203*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));

		// Scattered resistance
		new Unit(enemy, 114*2, 163*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy, 114*2, 163*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy, 118*2, 170*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy, 118*2, 170*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy, 109*2, 153*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy, 109*2, 153*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy, 122*2, 151*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy, 122*2, 151*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy,  98*2, 137*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy,  98*2, 137*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy,  93*2, 130*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy,  93*2, 130*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy,  86*2, 132*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy,  86*2, 132*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy,  72*2, 146*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy,  72*2, 146*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy, 158*2,  97*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy, 158*2,  97*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy, 132*2, 118*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy, 132*2, 118*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		new Unit(enemy, 157*2, 135*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(enemy, 157*2, 135*2, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
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
