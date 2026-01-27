package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.delegate.PlacingDelegate;
import com.oddlabs.tt.delegate.RallyPointDelegate;
import com.oddlabs.tt.delegate.TargetDelegate;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.SupplyCounter;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.ResourceBundle;

public final class ActionButtonPanel extends GUIObject implements Animated {
	private static final int GROUP_LEFT_OFFSET = 10;
	private static final int GROUP_BOTTOM_OFFSET = 10;
	private static final int GROUP_RIGHT_OFFSET = 10;
	private static final int GROUP_TOP_OFFSET = 20;

	private final Group unit_group = new NonFocusGroup();
	private final Group peon_group = new NonFocusGroup();
	private final Group chieftain_group = new NonFocusGroup();
	private final Group tower_group = new NonFocusGroup();
	private final Group quarters_status_group = new NonFocusGroup();
	private final Group quarters_group = new NonFocusGroup();
	private final Group status_group = new NonFocusGroup();
	private final Group armory_group = new NonFocusGroup();
	private final Group harvest_group = new NonFocusGroup();
	private final Group build_group = new NonFocusGroup();
	private final Group army_group = new NonFocusGroup();
	private final Group transport_group = new NonFocusGroup();

	private final @NonNull NonFocusIconButton tower_attack_button;
	private final @NonNull NonFocusIconButton tower_exit_button;
//	private boolean tower_exit_button_disabled;
	private final @NonNull NonFocusIconButton move_button;
	private final @NonNull NonFocusIconButton attack_button;
	private final @NonNull NonFocusIconButton gather_repair_button;
	private final @NonNull NonFocusIconButton quarters_button;
//	private boolean quarters_button_disabled;
	private final @NonNull RechargeButton magic1_button;
	private final @NonNull RechargeButton magic2_button;
	private final @NonNull NonFocusIconButton armory_button;
//	private boolean armory_button_disabled;
	private final @NonNull NonFocusIconButton tower_button;
//	private boolean tower_button_disabled;
	private final @NonNull NonFocusIconButton harvest_button;
	private final @NonNull NonFocusIconButton build_button;
	private final @NonNull NonFocusIconButton army_button;
	private final @NonNull NonFocusIconButton transport_button;
	private final @NonNull NonFocusIconButton rally_point_button;

	private final @NonNull StatusIcon unit_status;
	private final @NonNull StatusIcon weapon_rock_status;
	private final @NonNull StatusIcon weapon_iron_status;
	private final @NonNull StatusIcon weapon_rubber_status;
	private final @NonNull StatusIcon tree_status;
	private final @NonNull StatusIcon rock_status;
	private final @NonNull StatusIcon iron_status;
	private final @NonNull StatusIcon rubber_status;

	private final @NonNull WatchStatusIcon quarters_unit_status;
	private final @NonNull DeploySpinner quarters_peon_button;
	private final @NonNull ChieftainButton quarters_chieftain_button;
	private final @NonNull NonFocusIconButton quarters_rally_point_button;

	private final @NonNull DeploySpinner harvest_tree_button;
	private final @NonNull DeploySpinner harvest_rock_button;
	private final @NonNull DeploySpinner harvest_iron_button;
	private final @NonNull DeploySpinner harvest_rubber_button;
	private final @NonNull NonFocusIconButton harvest_back_button;

	private final @NonNull BuildSpinner build_weapon_rock_button;
	private final @NonNull BuildSpinner build_weapon_iron_button;
	private final @NonNull BuildSpinner build_weapon_rubber_button;
	private final @NonNull NonFocusIconButton build_back_button;

	private final @NonNull DeploySpinner army_peon_button;
	private final @NonNull DeploySpinner army_warrior_rock_button;
	private final @NonNull DeploySpinner army_warrior_iron_button;
	private final @NonNull DeploySpinner army_warrior_rubber_button;
	private final @NonNull NonFocusIconButton army_back_button;

	private final @NonNull DeploySpinner transport_tree_button;
	private final @NonNull DeploySpinner transport_rock_button;
	private final @NonNull DeploySpinner transport_iron_button;
	private final @NonNull DeploySpinner transport_rubber_button;
	private final @NonNull NonFocusIconButton transport_back_button;
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(ActionButtonPanel.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	private final @NonNull GameCamera camera;
	private final @NonNull WorldViewer viewer;

	private @Nullable Group current_submenu = null;
	private boolean update = false;
	private boolean current_quarters = false;
	private boolean current_armory = false;
	private @Nullable Building current_building;
	private boolean current_unit = false;
	private boolean current_peon = false;
	private @Nullable Unit current_chieftain;
	private boolean current_tower = false;
//	private boolean[] magic_disabled = new boolean[2];

	private @NonNull String formatTip(@NonNull String tip_key, String shortcut_key) {
		return i18n(tip_key, shortcut_key);
	}

	public ActionButtonPanel(@NonNull WorldViewer viewer, @NonNull GameCamera camera) {
		this(viewer, camera, viewer.getGUIRoot().getWidth(), viewer.getGUIRoot().getHeight());
	}

	public ActionButtonPanel(final @NonNull WorldViewer viewer, @NonNull GameCamera camera, int width, int height) {
		this.viewer = viewer;
		this.camera = camera;
		RaceIcons race_icons = viewer.getLocalPlayer().getRace().getIcons();
		Skin skin = Skin.getSkin();
		GUIIcons icons = GUIIcons.getIcons();
		String widest_char = new String(Character.toChars(skin.getEditFont().getWidestCodepoint("0123456789")));
		int label_width = skin.getEditFont().getWidth(widest_char + widest_char + widest_char);

		move_button = new NonFocusIconButton(race_icons.moveIcon(), formatTip("move_tip", "M"));
		move_button.setIconDisabler(() -> !viewer.getLocalPlayer().canMove());
		unit_group.addChild(move_button);
		move_button.addMouseClickListener((_, _, _, _) -> viewer.getGUIRoot().pushDelegate(new TargetDelegate(viewer, camera, Action.MOVE)));
		attack_button = new NonFocusIconButton(race_icons.attackIcon(), formatTip("attack_tip", "A"));
		attack_button.setIconDisabler(() -> !viewer.getLocalPlayer().canAttack());
		unit_group.addChild(attack_button);
		attack_button.addMouseClickListener((_, _, _, _) -> viewer.getGUIRoot().pushDelegate(new TargetDelegate(viewer, camera, Action.ATTACK)));
		move_button.place();
		attack_button.place(move_button, Placement.BOTTOM_MID);
		unit_group.compileCanvas(GROUP_LEFT_OFFSET, 0, GROUP_RIGHT_OFFSET, GROUP_BOTTOM_OFFSET);

		gather_repair_button = new NonFocusIconButton(race_icons.gatherRepairIcon(), formatTip("gather_repair_tip", "G"));
		peon_group.addChild(gather_repair_button);
		gather_repair_button.addMouseClickListener((_, _, _, _) -> viewer.getGUIRoot().pushDelegate(new TargetDelegate(viewer, camera, Action.GATHER_REPAIR)));
		gather_repair_button.setIconDisabler(() -> !viewer.getLocalPlayer().canRepair());
		quarters_button = new NonFocusIconButton(race_icons.quartersIcon(), formatTip("quarters_tip", "Q"));
		peon_group.addChild(quarters_button);
		quarters_button.addMouseClickListener((_, _, _, _) -> viewer.getGUIRoot().pushDelegate(new PlacingDelegate(viewer, camera.getState(), Race.BUILDING_QUARTERS)));
		quarters_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuild(Race.BUILDING_QUARTERS));
		armory_button = new NonFocusIconButton(race_icons.armoryIcon(), formatTip("armory_tip", "R"));
		peon_group.addChild(armory_button);
		armory_button.addMouseClickListener((_, _, _, _) -> viewer.getGUIRoot().pushDelegate(new PlacingDelegate(viewer, camera.getState(), Race.BUILDING_ARMORY)));
		armory_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuild(Race.BUILDING_ARMORY));
		tower_button = new NonFocusIconButton(race_icons.towerIcon(), formatTip("tower_tip", "T"));
		peon_group.addChild(tower_button);
		tower_button.addMouseClickListener((_, _, _, _) -> viewer.getGUIRoot().pushDelegate(new PlacingDelegate(viewer, camera.getState(), Race.BUILDING_TOWER)));
		tower_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuild(Race.BUILDING_TOWER));
		gather_repair_button.place();
		quarters_button.place(gather_repair_button, Placement.BOTTOM_MID);
		armory_button.place(quarters_button, Placement.BOTTOM_MID);
		tower_button.place(armory_button, Placement.BOTTOM_MID);
		peon_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, 0);

		PlayerInterface player_interface = viewer.getPeerHub().getPlayerInterface();
		magic1_button = new RechargeButton(player_interface, race_icons.magic1Icon(), race_icons.magic1Desc(), 0);
		chieftain_group.addChild(magic1_button);
//		magic1_button.addMouseClickListener(new MagicListener(0));
		magic2_button = new RechargeButton(player_interface, race_icons.magic2Icon(), race_icons.magic2Desc(), 1);
		chieftain_group.addChild(magic2_button);
//		magic2_button.addMouseClickListener(new MagicListener(1));
		magic1_button.place();
		magic2_button.place(magic1_button, Placement.BOTTOM_MID);
		chieftain_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, 0);

		tower_attack_button = new NonFocusIconButton(race_icons.attackIcon(), formatTip("attack_tip", "A"));
		tower_group.addChild(tower_attack_button);
		tower_attack_button.addMouseClickListener((_, _, _, _) -> viewer.getGUIRoot().pushDelegate(new TargetDelegate(viewer, camera, Action.ATTACK)));
		tower_exit_button = new NonFocusIconButton(race_icons.towerExitIcon(), formatTip("exit_tip", "X"));
		tower_group.addChild(tower_exit_button);
		tower_exit_button.addMouseClickListener((_, _, _, _) -> {
			if (current_building != null && !current_building.isDead())
				viewer.getPeerHub().getPlayerInterface().exitTower(current_building);
			removeGroups();
			update = true;
		});
		tower_attack_button.place();
		tower_exit_button.place(tower_attack_button, Placement.BOTTOM_MID);
		tower_group.compileCanvas();

		unit_status = new StatusIcon(label_width, race_icons.unitStatusIcon(), i18n("units_tip"));
		status_group.addChild(unit_status);
		weapon_rock_status = new StatusIcon(label_width, race_icons.weaponRockStatusIcon(), i18n("rock_weapons_tip"));
		status_group.addChild(weapon_rock_status);
		weapon_iron_status = new StatusIcon(label_width, race_icons.weaponIronStatusIcon(), i18n("iron_weapons_tip"));
		status_group.addChild(weapon_iron_status);
		weapon_rubber_status = new StatusIcon(label_width, race_icons.weaponRubberStatusIcon(), i18n("chicken_weapons_tip"));
		status_group.addChild(weapon_rubber_status);
		tree_status = new StatusIcon(label_width, icons.getTreeStatusIcon(), i18n("tree_resources_tip"));
		status_group.addChild(tree_status);
		rock_status = new StatusIcon(label_width, icons.getRockStatusIcon(), i18n("rock_resources_tip"));
		status_group.addChild(rock_status);
		iron_status = new StatusIcon(label_width, icons.getIronStatusIcon(), i18n("iron_resources_tip"));
		status_group.addChild(iron_status);
		rubber_status = new StatusIcon(label_width, icons.getRubberStatusIcon(), i18n("chicken_resources_tip"));
		status_group.addChild(rubber_status);
		unit_status.place();
		weapon_rock_status.place(unit_status, Placement.BOTTOM_MID);
		weapon_iron_status.place(weapon_rock_status, Placement.BOTTOM_MID);
		weapon_rubber_status.place(weapon_iron_status, Placement.BOTTOM_MID);
		tree_status.place(unit_status, Placement.LEFT_MID, 5);
		rock_status.place(tree_status, Placement.BOTTOM_MID);
		iron_status.place(rock_status, Placement.BOTTOM_MID);
		rubber_status.place(iron_status, Placement.BOTTOM_MID);
		status_group.compileCanvas(5, 5, 5, 5);

		quarters_unit_status = new WatchStatusIcon(label_width, race_icons.unitStatusIcon(), i18n("units_tip"));
		quarters_status_group.addChild(quarters_unit_status);
		quarters_unit_status.place();
		quarters_status_group.compileCanvas(5, 5, 5, 5);

		quarters_peon_button = new DeploySpinner(viewer, player_interface, race_icons.peonIcon(), i18n("deploy_peon_tip"),
				new IconQuad[]{race_icons.unitStatusIcon()}, "P");
		quarters_group.addChild(quarters_peon_button);
		quarters_chieftain_button = new ChieftainButton(viewer, player_interface, race_icons.chieftainIcon(), formatTip("train_chieftain_tip", "C"));
//		if (Settings.getSettings().developer_mode) {
			quarters_group.addChild(quarters_chieftain_button);
//		}
		quarters_rally_point_button = new NonFocusIconButton(race_icons.rallyPointIcon(), formatTip("rally_point_tip", "R"));
		quarters_group.addChild(quarters_rally_point_button);
		quarters_rally_point_button.addMouseClickListener(this::setRallyPoint);
		quarters_peon_button.place();
//		if (Settings.getSettings().developer_mode) {
			quarters_chieftain_button.place(quarters_peon_button, Placement.BOTTOM_MID);
			quarters_rally_point_button.place(quarters_chieftain_button, Placement.BOTTOM_MID);
//		} else {
//			quarters_rally_point_button.place(quarters_peon_button, Placement.Placement.BOTTOM_MID);
//		}
		quarters_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

		harvest_button = new NonFocusIconButton(icons.getHarvestIcon(), formatTip("gather_resources_tip", "G"));
		harvest_button.setIconDisabler(() -> !viewer.getLocalPlayer().canHarvest());
		armory_group.addChild(harvest_button);
		harvest_button.addMouseClickListener((_, _, _, _) -> {
			armory_group.remove();
			addChild(harvest_group);
			current_submenu = harvest_group;
		});
		build_button = new NonFocusIconButton(race_icons.buildWeaponsIcon(), formatTip("produce_weapons_tip", "W"));
		build_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuildWeapons());
		armory_group.addChild(build_button);
		build_button.addMouseClickListener((_, _, _, _) -> {
			armory_group.remove();
			addChild(build_group);
			current_submenu = build_group;
			updateCounters();
		});
		army_button = new NonFocusIconButton(race_icons.armyIcon(), formatTip("deploy_army_tip", "A"));
		army_button.setIconDisabler(() -> !viewer.getLocalPlayer().canBuildArmies());
		armory_group.addChild(army_button);
		army_button.addMouseClickListener((_, _, _, _) -> {
			armory_group.remove();
			addChild(army_group);
			current_submenu = army_group;
		});
		transport_button = new NonFocusIconButton(race_icons.transportIcon(), formatTip("transport_resources_tip", "T"));
		armory_group.addChild(transport_button);
		transport_button.addMouseClickListener((_, _, _, _) -> {
			armory_group.remove();
			addChild(transport_group);
			current_submenu = transport_group;
		});
		rally_point_button = new NonFocusIconButton(race_icons.rallyPointIcon(), formatTip("rally_point_tip", "R"));
		rally_point_button.setIconDisabler(() -> !viewer.getLocalPlayer().canSetRallyPoints());
		armory_group.addChild(rally_point_button);
		rally_point_button.addMouseClickListener(this::setRallyPoint);
		harvest_button.place();
		build_button.place(harvest_button, Placement.BOTTOM_MID);
		army_button.place(build_button, Placement.BOTTOM_MID);
		transport_button.place(army_button, Placement.BOTTOM_MID);
		rally_point_button.place(transport_button, Placement.BOTTOM_MID);
		armory_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

		harvest_tree_button = new DeploySpinner(viewer, player_interface, icons.getTreeIcon(), i18n("harvest_tree_tip"), new IconQuad[]{race_icons.unitStatusIcon()}, "W");
		harvest_group.addChild(harvest_tree_button);
		harvest_rock_button = new DeploySpinner(viewer, player_interface, icons.getRockIcon(), i18n("harvest_rock_tip"), new IconQuad[]{race_icons.unitStatusIcon()}, "R");
		harvest_group.addChild(harvest_rock_button);
		harvest_iron_button = new DeploySpinner(viewer, player_interface, icons.getIronIcon(), i18n("harvest_iron_tip"), new IconQuad[]{race_icons.unitStatusIcon()}, "I");
		harvest_group.addChild(harvest_iron_button);
		harvest_rubber_button = new DeploySpinner(viewer, player_interface, icons.getRubberIcon(), i18n("harvest_chicken_tip"), new IconQuad[]{race_icons.unitStatusIcon()}, "C");
		harvest_group.addChild(harvest_rubber_button);
		harvest_back_button = new NonFocusIconButton(skin.getBackButton(), formatTip("back_tip", "Backspace"));
		harvest_back_button.addMouseClickListener(this::cancelSubMenu);
		harvest_group.addChild(harvest_back_button);
		harvest_tree_button.place();
		harvest_rock_button.place(harvest_tree_button, Placement.BOTTOM_MID);
		harvest_iron_button.place(harvest_rock_button, Placement.BOTTOM_MID);
		harvest_rubber_button.place(harvest_iron_button, Placement.BOTTOM_MID);
		harvest_back_button.place(harvest_rubber_button, Placement.BOTTOM_MID);
		harvest_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

		build_weapon_rock_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponRockIcon(), i18n("build_rock_tip"), Building.COST_ROCK_WEAPON.toIconArray(), "R");
		build_group.addChild(build_weapon_rock_button);
		build_weapon_iron_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponIronIcon(), i18n("build_iron_tip"), Building.COST_IRON_WEAPON.toIconArray(), "I");
		build_group.addChild(build_weapon_iron_button);
		build_weapon_rubber_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponRubberIcon(), i18n("build_chicken_tip"), Building.COST_RUBBER_WEAPON.toIconArray(), "C");
		build_group.addChild(build_weapon_rubber_button);
		build_back_button = new NonFocusIconButton(skin.getBackButton(), formatTip("back_tip", "Backspace"));
		build_back_button.addMouseClickListener(this::cancelSubMenu);
		build_group.addChild(build_back_button);
		build_weapon_rock_button.place();
		build_weapon_iron_button.place(build_weapon_rock_button, Placement.BOTTOM_MID);
		build_weapon_rubber_button.place(build_weapon_iron_button, Placement.BOTTOM_MID);
		build_back_button.place(build_weapon_rubber_button, Placement.BOTTOM_MID);
		build_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

		army_peon_button = new DeploySpinner(viewer, player_interface, race_icons.peonIcon(), i18n("deploy_peon_tip"),
				new IconQuad[]{race_icons.unitStatusIcon()}, "P");
		army_group.addChild(army_peon_button);
		army_warrior_rock_button = new DeploySpinner(viewer, player_interface, race_icons.warriorRockIcon(), i18n("deploy_rock_tip"),
				new IconQuad[]{race_icons.unitStatusIcon(), race_icons.weaponRockStatusIcon()}, "R");
		army_group.addChild(army_warrior_rock_button);

		army_warrior_iron_button = new DeploySpinner(viewer, player_interface, race_icons.warriorIronIcon(), i18n("deploy_iron_tip"),
				new IconQuad[]{race_icons.unitStatusIcon(), race_icons.weaponIronStatusIcon()}, "I");
		army_group.addChild(army_warrior_iron_button);

		army_warrior_rubber_button = new DeploySpinner(viewer, player_interface, race_icons.warriorRubberIcon(), i18n("deploy_chicken_tip"),
				new IconQuad[]{race_icons.unitStatusIcon(), race_icons.weaponRubberStatusIcon()}, "C");
		army_group.addChild(army_warrior_rubber_button);

		army_back_button = new NonFocusIconButton(skin.getBackButton(), formatTip("back_tip", "Backspace"));
		army_back_button.addMouseClickListener(this::cancelSubMenu);
		army_group.addChild(army_back_button);
		army_peon_button.place();
		army_warrior_rock_button.place(army_peon_button, Placement.BOTTOM_MID);
		army_warrior_iron_button.place(army_warrior_rock_button, Placement.BOTTOM_MID);
		army_warrior_rubber_button.place(army_warrior_iron_button, Placement.BOTTOM_MID);
		army_back_button.place(army_warrior_rubber_button, Placement.BOTTOM_MID);
		army_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

		transport_tree_button = new DeploySpinner(viewer, player_interface, icons.getTreeIcon(), i18n("transport_tree_tip"),
				new IconQuad[]{race_icons.unitStatusIcon(), icons.getTreeStatusIcon()}, "W");
		transport_group.addChild(transport_tree_button);
		transport_rock_button = new DeploySpinner(viewer, player_interface, icons.getRockIcon(), i18n("transport_rock_tip"),
				new IconQuad[]{race_icons.unitStatusIcon(), icons.getRockStatusIcon()}, "R");
		transport_group.addChild(transport_rock_button);
		transport_iron_button = new DeploySpinner(viewer, player_interface, icons.getIronIcon(), i18n("transport_iron_tip"),
				new IconQuad[]{race_icons.unitStatusIcon(), icons.getIronStatusIcon()}, "I");
		transport_group.addChild(transport_iron_button);
		transport_rubber_button = new DeploySpinner(viewer, player_interface, icons.getRubberIcon(), i18n("transport_chicken_tip"),
				new IconQuad[]{race_icons.unitStatusIcon(), icons.getRubberStatusIcon()}, "C");
		transport_group.addChild(transport_rubber_button);
		transport_back_button = new NonFocusIconButton(skin.getBackButton(), formatTip("back_tip", "Backspace"));
		transport_back_button.addMouseClickListener(this::cancelSubMenu);
		transport_group.addChild(transport_back_button);
		transport_tree_button.place();
		transport_rock_button.place(transport_tree_button, Placement.BOTTOM_MID);
		transport_iron_button.place(transport_rock_button, Placement.BOTTOM_MID);
		transport_rubber_button.place(transport_iron_button, Placement.BOTTOM_MID);
		transport_back_button.place(transport_rubber_button, Placement.BOTTOM_MID);
		transport_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

		setCanFocus(true);
		displayChangedNotify(width, height);
	}

	@Override
	public void doAdd() {
		super.doAdd();
		viewer.getAnimationManagerLocal().registerAnimation(this);
		GUIRoot root = getParentGUIRoot();
		if (root != null) {
			displayChangedNotify(root.getWidth(), root.getHeight());
		}
	}

	@Override
	protected void doRemove() {
		super.doRemove();
		viewer.getAnimationManagerLocal().removeAnimation(this);
	}

    @Override
	public void animate(float t) {
		Building new_building = viewer.getSelection().getCurrentSelection().getBuilding();
		boolean different_building = new_building != current_building;
		current_building = new_building;
		viewer.getRenderer().setSelectedBuilding(new_building);

		Unit new_chieftain = viewer.getSelection().getCurrentSelection().getChieftain();
		boolean different_chieftain = new_chieftain != current_chieftain;
		current_chieftain = new_chieftain;

		int current_num_units = viewer.getSelection().getCurrentSelection().getNumUnits();
		int current_num_peons = viewer.getSelection().getCurrentSelection().getNumBuilders();

		boolean new_quarters = current_building != null && current_building.getAbilities().hasAbilities(Abilities.REPRODUCE);
		boolean new_armory = current_building != null && current_building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES);
		boolean new_unit = current_num_units > 0;
		boolean new_peon = current_num_peons > 0;
		boolean new_tower = current_building != null && current_building.getAbilities().hasAbilities(Abilities.ATTACK);
		update = update || different_building || different_chieftain || new_quarters != current_quarters || new_armory != current_armory || new_unit != current_unit || new_peon != current_peon || new_tower != current_tower;
		if (update) {
			current_quarters = new_quarters;
			current_armory = new_armory;
			current_tower = new_tower;
			current_unit = new_unit;
			current_peon = new_peon;
			update = false;

			removeGroups();

			if (current_unit) {
				addChild(unit_group);
			}
			if (current_peon) {
				addChild(peon_group);
			}
			if (current_chieftain != null) {
				addChild(chieftain_group);
				updateGroups();
				Player player = viewer.getLocalPlayer();
				if (player.canDoMagic(0)) {
					magic1_button.setUnit(current_chieftain);
					magic1_button.setIconDisabler(() -> !current_chieftain.canDoMagic(0));
					chieftain_group.addChild(magic1_button);
				} else
					magic1_button.remove();
				if (player.canDoMagic(1)) {
					magic2_button.setUnit(current_chieftain);
					magic2_button.setIconDisabler(() -> !current_chieftain.canDoMagic(1));
					chieftain_group.addChild(magic2_button);
				} else
					magic2_button.remove();
			}
			if (current_tower) {
				addChild(tower_group);
				tower_attack_button.setIconDisabler(() -> current_building == null || !current_building.getAbilities().hasAbilities(Abilities.ATTACK));
				tower_exit_button.setIconDisabler(() -> current_building == null || !current_building.canExitTower());
			}
			if (current_quarters) {
				addChild(quarters_status_group);
				addChild(quarters_group);
				SupplyCounter unit_counter = new SupplyCounter(current_building, Unit.class);
				quarters_unit_status.setCounter(unit_counter);
				quarters_unit_status.setUnitContainerBuilding(current_building);
				quarters_peon_button.setContainers(current_building, DeployType.PEON, null);
				quarters_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
				quarters_chieftain_button.setIconDisabler(() -> current_building != null && !current_building.canBuildChieftain() && !current_building.canStopChieftain());
				quarters_chieftain_button.setBuilding(current_building);
			}
			if (current_armory) {
				addChild(status_group);
				addChild(armory_group);
				if (viewer.getLocalPlayer().canUseRubber()) {
					build_group.addChild(build_weapon_rubber_button);
					army_group.addChild(army_warrior_rubber_button);
				} else {
					build_weapon_rubber_button.remove();
					army_warrior_rubber_button.remove();
				}
				updateCounters();
			}
		}
		updateButtons();
	}

	private void updateButtons() {
		if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.ATTACK)) {
			tower_attack_button.doUpdate();
			tower_exit_button.doUpdate();
		} else if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
			unit_status.doUpdate();
			weapon_rock_status.doUpdate();
			weapon_iron_status.doUpdate();
			weapon_rubber_status.doUpdate();
			tree_status.doUpdate();
			rock_status.doUpdate();
			iron_status.doUpdate();
			rubber_status.doUpdate();

			harvest_button.doUpdate();
			build_button.doUpdate();
			army_button.doUpdate();

			harvest_tree_button.doUpdate();
			harvest_rock_button.doUpdate();
			harvest_iron_button.doUpdate();
			harvest_rubber_button.doUpdate();
			build_weapon_rock_button.doUpdate();
			build_weapon_iron_button.doUpdate();
			build_weapon_rubber_button.doUpdate();
			army_warrior_rubber_button.doUpdate();
			army_warrior_iron_button.doUpdate();
			army_warrior_rock_button.doUpdate();
			army_peon_button.doUpdate();
			transport_tree_button.doUpdate();
			transport_rock_button.doUpdate();
			transport_iron_button.doUpdate();
			transport_rubber_button.doUpdate();
		} else if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.REPRODUCE)) {
			quarters_unit_status.doUpdate();

			quarters_peon_button.doUpdate();
			quarters_chieftain_button.doUpdate();
		} else if (current_peon) {
			quarters_button.doUpdate();
			armory_button.doUpdate();
			tower_button.doUpdate();
		}
		if (current_unit) {
			move_button.doUpdate();
			attack_button.doUpdate();
			gather_repair_button.doUpdate();
		}
		if (current_chieftain != null) {
			magic1_button.doUpdate();
			magic2_button.doUpdate();
		}
	}

	private void removeGroups() {
		unit_group.remove();
		peon_group.remove();
		chieftain_group.remove();
		tower_group.remove();
		quarters_status_group.remove();
		quarters_group.remove();
		status_group.remove();
		armory_group.remove();
		harvest_group.remove();
		build_group.remove();
		army_group.remove();
		transport_group.remove();
		current_submenu = null;
	}

	private void updateCounters() {
		assert current_building != null: "Building is null";
		SupplyCounter unit_counter = new SupplyCounter(current_building, Unit.class);
		unit_status.setCounter(unit_counter);
		SupplyCounter weapon_rock_counter = new SupplyCounter(current_building, RockAxeWeapon.class);
		weapon_rock_status.setCounter(weapon_rock_counter);
		SupplyCounter weapon_iron_counter = new SupplyCounter(current_building, IronAxeWeapon.class);
		weapon_iron_status.setCounter(weapon_iron_counter);
		SupplyCounter weapon_rubber_counter = new SupplyCounter(current_building, RubberAxeWeapon.class);
		weapon_rubber_status.setCounter(weapon_rubber_counter);
		SupplyCounter tree_counter = new SupplyCounter(current_building, TreeSupply.class);
		tree_status.setCounter(tree_counter);
		SupplyCounter rock_counter = new SupplyCounter(current_building, RockSupply.class);
		rock_status.setCounter(rock_counter);
		SupplyCounter iron_counter = new SupplyCounter(current_building, IronSupply.class);
		iron_status.setCounter(iron_counter);
		SupplyCounter rubber_counter = new SupplyCounter(current_building, RubberSupply.class);
		rubber_status.setCounter(rubber_counter);

		harvest_tree_button.setContainers(current_building, DeployType.PEON_HARVEST_TREE, null);
		harvest_tree_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
		harvest_rock_button.setContainers(current_building, DeployType.PEON_HARVEST_ROCK, null);
		harvest_rock_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
		harvest_iron_button.setContainers(current_building, DeployType.PEON_HARVEST_IRON, null);
		harvest_iron_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
		harvest_rubber_button.setContainers(current_building, DeployType.PEON_HARVEST_RUBBER, null);
		harvest_rubber_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);

		build_weapon_rock_button.setBuildSupplyContainer(current_building, RockAxeWeapon.class);
		build_weapon_iron_button.setBuildSupplyContainer(current_building, IronAxeWeapon.class);
		build_weapon_rubber_button.setBuildSupplyContainer(current_building, RubberAxeWeapon.class);

		army_peon_button.setContainers(current_building, DeployType.PEON, null);
		army_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
		army_warrior_rock_button.setContainers(current_building, DeployType.ROCK_WARRIOR, RockAxeWeapon.class);
		army_warrior_rock_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rock_counter));
		army_warrior_iron_button.setContainers(current_building, DeployType.IRON_WARRIOR, IronAxeWeapon.class);
		army_warrior_iron_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_iron_counter));
		army_warrior_rubber_button.setContainers(current_building, DeployType.RUBBER_WARRIOR, RubberAxeWeapon.class);
		army_warrior_rubber_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rubber_counter));

		transport_tree_button.setContainers(current_building, DeployType.PEON_TRANSPORT_TREE, TreeSupply.class);
		transport_tree_button.setIconDisabler(() -> suppliesEmpty(unit_counter, tree_counter));
		transport_rock_button.setContainers(current_building, DeployType.PEON_TRANSPORT_ROCK, RockSupply.class);
		transport_rock_button.setIconDisabler(() -> suppliesEmpty(unit_counter, rock_counter));
		transport_iron_button.setContainers(current_building, DeployType.PEON_TRANSPORT_IRON, IronSupply.class);
		transport_iron_button.setIconDisabler(() -> suppliesEmpty(unit_counter, iron_counter));
		transport_rubber_button.setContainers(current_building, DeployType.PEON_TRANSPORT_RUBBER, RubberSupply.class);
		transport_rubber_button.setIconDisabler(() -> suppliesEmpty(unit_counter, rubber_counter));
	}

	@Override
	public void displayChangedNotify(int width, int height) {
		setDim(width, height);
		updateGroups();
	}

	private void updateGroups() {
		int width = getWidth();
		int height = getHeight();
		unit_group.setPos(width - unit_group.getWidth(), height - unit_group.getHeight());
		peon_group.setPos(width - peon_group.getWidth(), unit_group.getY() - peon_group.getHeight());
		if (current_peon)
			chieftain_group.setPos(width - chieftain_group.getWidth(), peon_group.getY() - chieftain_group.getHeight());
		else
			chieftain_group.setPos(width - chieftain_group.getWidth(), unit_group.getY() - chieftain_group.getHeight());
		tower_group.setPos(width - tower_group.getWidth(), height - tower_group.getHeight());
		quarters_status_group.setPos(width - quarters_status_group.getWidth(), height - quarters_status_group.getHeight());
		quarters_group.setPos(width - quarters_group.getWidth(), quarters_status_group.getY() - quarters_group.getHeight());
		status_group.setPos(width - status_group.getWidth(), height - status_group.getHeight());
		armory_group.setPos(width - armory_group.getWidth(), status_group.getY() - armory_group.getHeight());
		harvest_group.setPos(width - harvest_group.getWidth(), status_group.getY() - harvest_group.getHeight());
		build_group.setPos(width - build_group.getWidth(), status_group.getY() - build_group.getHeight());
		army_group.setPos(width - army_group.getWidth(), status_group.getY() - army_group.getHeight());
		transport_group.setPos(width - transport_group.getWidth(), status_group.getY() - transport_group.getHeight());
	}

	@Override
	public void handleInput(@NonNull InputEvent event) {
		InputPhase phase = event.getPhase();
		boolean pressed = phase == InputPhase.PRESSED || phase == InputPhase.REPEAT;
		boolean released = phase == InputPhase.RELEASED;

		if (pressed) {
			if (event.consumeAction(GameAction.UNIT_MOVE)) {
				if (current_unit) {
					move_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				}
			} else if (event.consumeAction(GameAction.UNIT_BUILD_QUARTERS)) {
				if (current_unit && current_peon) {
					quarters_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				}
			} else if (event.consumeAction(GameAction.UNIT_ATTACK) || event.consumeAction(GameAction.PROD_ARMY)) {
				// A - Attack or Army
				boolean ctrl = event.isControlDown();
				if (!ctrl) {
					if (current_unit) {
						attack_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					} else if (current_armory && current_submenu == null) {
						army_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					} else if (current_tower) {
						tower_attack_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					}
				}
			} else if (event.consumeAction(GameAction.TRAIN_PEON)) {
				// P - Peon
				boolean shift = event.isShiftDown();
				boolean ctrl = event.isControlDown();
				if (current_quarters) {
					quarters_peon_button.shortcutPressed(shift, ctrl);
				} else if (current_armory && current_submenu == army_group) {
					army_peon_button.shortcutPressed(shift, ctrl);
				}
			} else if (event.consumeAction(GameAction.UNIT_GATHER) || event.consumeAction(GameAction.PROD_HARVEST)) {
				// G - Gather or Harvest
				if (current_unit) {
					gather_repair_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				} else if (current_armory) {
					harvest_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				}
			} else if (event.consumeAction(GameAction.UNIT_BUILD_TOWER) || event.consumeAction(GameAction.PROD_TRANSPORT)) {
				// T - Tower or Transport
				if (current_peon) {
					tower_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				} else if (current_armory && current_submenu == null) {
					transport_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				}
			} else if (event.consumeAction(GameAction.TRAIN_CHIEFTAIN) || event.consumeAction(GameAction.MAGIC_2) || event.consumeAction(GameAction.RES_CHICKEN)) {
				// C - Chieftain, Magic2, Chicken/Rubber
				if (current_quarters) {
					quarters_chieftain_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				} else if (current_chieftain != null) {
					Player player = viewer.getLocalPlayer();
					if (player.canDoMagic(1)) {
						magic2_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					}
				} else if (current_armory) {
					boolean shift = event.isShiftDown();
					boolean ctrl = event.isControlDown();
					if (current_submenu == harvest_group) harvest_rubber_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == build_group) build_weapon_rubber_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == army_group) army_warrior_rubber_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == transport_group) transport_rubber_button.shortcutPressed(shift, ctrl);
				}
			} else if (event.consumeAction(GameAction.RES_IRON)) {
				// I - Iron
				if (current_armory) {
					boolean shift = event.isShiftDown();
					boolean ctrl = event.isControlDown();
					if (current_submenu == harvest_group) harvest_iron_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == build_group) build_weapon_iron_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == army_group) army_warrior_iron_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == transport_group) transport_iron_button.shortcutPressed(shift, ctrl);
				}
			} else if (event.consumeAction(GameAction.PROD_WEAPONS) || event.consumeAction(GameAction.RES_TREE)) {
				// W - Weapons or Tree
				if (current_armory) {
					boolean shift = event.isShiftDown();
					boolean ctrl = event.isControlDown();
					if (current_submenu == null) build_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					else if (current_submenu == harvest_group) harvest_tree_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == transport_group) transport_tree_button.shortcutPressed(shift, ctrl);
				}
			} else if (event.consumeAction(GameAction.GAMEPLAY_BACK)) {
				// Backspace
				if (current_armory && current_submenu != null) {
					if (current_submenu == harvest_group) harvest_back_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					else if (current_submenu == build_group) build_back_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					else if (current_submenu == army_group) army_back_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					else if (current_submenu == transport_group) transport_back_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				}
			} else if (event.consumeAction(GameAction.UNIT_BUILD_ARMORY) || event.consumeAction(GameAction.RES_ROCK) || event.consumeAction(GameAction.UNIT_SET_RALLY)) {
				// R - Armory, Rock, Rally
				if (current_peon) {
					armory_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				} else if (current_armory) {
					boolean shift = event.isShiftDown();
					boolean ctrl = event.isControlDown();
					if (current_submenu == harvest_group) harvest_rock_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == build_group) build_weapon_rock_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == army_group) army_warrior_rock_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == transport_group) transport_rock_button.shortcutPressed(shift, ctrl);
					else if (current_submenu == null) rally_point_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				} else if (current_quarters) {
					quarters_rally_point_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				}
			} else if (event.consumeAction(GameAction.UNIT_EXIT_TOWER)) {
				// X - Exit Tower
				if (current_tower) {
					tower_exit_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
				}
			} else if (event.consumeAction(GameAction.MAGIC_1)) {
				// S - Magic 1
				if (current_chieftain != null) {
					Player player = viewer.getLocalPlayer();
					if (player.canDoMagic(0)) {
						magic1_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
					}
				}
			}
		} else if (released) {
			boolean shift = event.isShiftDown();
			boolean ctrl = event.isControlDown();
			
			if (event.consumeAction(GameAction.TRAIN_CHIEFTAIN) || event.consumeAction(GameAction.MAGIC_2) || event.consumeAction(GameAction.RES_CHICKEN)) {
				if (current_armory) {
					if (current_submenu == harvest_group) harvest_rubber_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == build_group) build_weapon_rubber_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == army_group) army_warrior_rubber_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == transport_group) transport_rubber_button.shortcutReleased(shift, ctrl);
				}
			} else if (event.consumeAction(GameAction.TRAIN_PEON)) {
				if (current_quarters) quarters_peon_button.shortcutReleased(shift, ctrl);
				else if (current_armory && current_submenu == army_group) army_peon_button.shortcutReleased(shift, ctrl);
			} else if (event.consumeAction(GameAction.RES_IRON)) {
				if (current_armory) {
					if (current_submenu == harvest_group) harvest_iron_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == build_group) build_weapon_iron_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == army_group) army_warrior_iron_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == transport_group) transport_iron_button.shortcutReleased(shift, ctrl);
				}
			} else if (event.consumeAction(GameAction.UNIT_BUILD_ARMORY) || event.consumeAction(GameAction.RES_ROCK) || event.consumeAction(GameAction.UNIT_SET_RALLY)) {
				if (current_armory) {
					if (current_submenu == harvest_group) harvest_rock_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == build_group) build_weapon_rock_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == army_group) army_warrior_rock_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == transport_group) transport_rock_button.shortcutReleased(shift, ctrl);
				}
			} else if (event.consumeAction(GameAction.UNIT_BUILD_TOWER) || event.consumeAction(GameAction.PROD_TRANSPORT)) {
				if (current_armory && current_submenu == null) {
                    transport_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
                }
			} else if (event.consumeAction(GameAction.PROD_WEAPONS) || event.consumeAction(GameAction.RES_TREE)) {
				if (current_armory) {
					if (current_submenu == harvest_group) harvest_tree_button.shortcutReleased(shift, ctrl);
					else if (current_submenu == transport_group) transport_tree_button.shortcutReleased(shift, ctrl);
				}
			}
		}
	}

	@Override
	public boolean canHoverBehind() {
		return true;
	}

    public boolean inHarvestMenu() {
		return current_submenu == harvest_group;
	}

	public boolean inBuildMenu() {
		return current_submenu == build_group;
	}

	public boolean inArmyMenu() {
		return current_submenu == army_group;
	}

	public boolean inTransportMenu() {
		return current_submenu == transport_group;
	}


	@Override
	public void mouseDragged (@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		if (getParent() != null)
			getParent().mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
	}

	private void setRallyPoint(@NonNull MouseButton button, int x, int y, int clicks) {
		if (current_building != null && !current_building.isDead()) {
			viewer.getGUIRoot().pushDelegate(new RallyPointDelegate(viewer, camera, current_building));
		}
		removeGroups();
		update = true;
	}

	private void cancelSubMenu(@NonNull MouseButton button, int x, int y, int clicks) {
		removeGroups();
		update = true;
	}

	private boolean suppliesEmpty(@NonNull SupplyCounter @NonNull ... counters) {
		return Arrays.stream(counters).anyMatch(c -> c.getNumSupplies() == 0);
	}
}