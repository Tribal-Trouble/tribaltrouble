package com.oddlabs.tt.landscape;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.model.AbstractElementNode;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.SupplyManager;
import com.oddlabs.tt.model.SupplyManagers;
import com.oddlabs.tt.pathfinder.RegionBuilder;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.WorldInfo;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public final class World {
	public static final int GAMESPEED_DONTCARE = -2;

	private static final float[] GAMESPEEDS = new float[]{
		0f,
        AnimationManager.ANIMATION_SECONDS_PER_TICK/2,
        AnimationManager.ANIMATION_SECONDS_PER_TICK,
        AnimationManager.ANIMATION_SECONDS_PER_TICK*1.75f,
        AnimationManager.ANIMATION_SECONDS_PER_TICK*4
    };

	private final @NonNull HeightMap world;
	private final @NonNull Random random;
	private final @NonNull AnimationManager animation_manager_game_time;
	private final @NonNull AnimationManager animation_manager_real_time;
	private final @NonNull AudioImplementation audio_impl;

	private final int max_unit_count;
	private final @NonNull NotificationListener notification_listener;

	private final @NonNull Player @NonNull [] players;
	private final @NonNull SupplyManagers supply_managers;
	private final @NonNull UnitGrid unit_grid;
	private final @NonNull AbstractPatchGroup patch_root;
	private final @NonNull AbstractTreeGroup tree_root;
	private final @NonNull AbstractElementNode<?> element_root;
	private final @Nullable RacesResources races_resources;
	private final @NonNull LandscapeResources landscape_resources;
    private final @NonNull FogInfo fog;

	private int global_checksum;
	private int gamespeed;

	public static @NonNull LandscapeResources loadCommon(@NonNull RenderQueues queues) {
		LandscapeResources landscape_resources = new LandscapeResources(queues);
		ProgressForm.progress();
		return landscape_resources;
	}

	public static @NonNull RacesResources loadInGame(@NonNull RenderQueues queues) {
		return new RacesResources(queues);
	}

	public static @NonNull World newWorld(@NonNull AudioImplementation audio_implementation, @NonNull LandscapeResources landscape_resources, @Nullable RacesResources races_resources, @NonNull NotificationListener notification_listener, @NonNull WorldParameters world_params, @NonNull WorldInfo world_info, Landscape.@NonNull TerrainType terrain, @NonNull PlayerInfo @NonNull [] player_infos, @NonNull Vector4fc @NonNull [] colors, com.oddlabs.tt.resource.@NonNull FogInfo fog) {
		ProgressForm.progress();
		World world = new World(audio_implementation, landscape_resources, races_resources, notification_listener, world_params, world_info, terrain, player_infos, colors, fog);
		ProgressForm.progress();
		ProgressForm.progress(1/5f);
		ProgressForm.progress();
		Player[] players = world.getPlayers();
		for (short i = 0; i < players.length; i++) {
			Player player = players[i];
			assert player != null;
			player.init(world_info.starting_locations[i]);
		}
		return world;
	}
    
    public com.oddlabs.tt.resource.@NonNull FogInfo getFog() {
        return fog;
    }

	public @NonNull LandscapeResources getLandscapeResources() {
		return landscape_resources;
	}

	public @Nullable RacesResources getRacesResources() {
		return races_resources;
	}

	public @NonNull AudioImplementation getAudio() {
		return audio_impl;
	}

	public int getChecksum() {
		return global_checksum;
	}

	public void updateGlobalChecksum(int value) {
		global_checksum += value;
	}

	public int getGamespeed() {
		return gamespeed;
	}

	public float getSecondsPerTick() {
		return GAMESPEEDS[gamespeed];
	}

	public static boolean isValidPreferredGamespeed(int speed) {
		return speed == GAMESPEED_DONTCARE || isValidGamespeed(speed);
	}

	public static boolean isValidGamespeed(int speed) {
		return speed >= 0 && speed < GAMESPEEDS.length;
	}

	public void gamespeedChanged() {
		int new_gamespeed = GAMESPEED_DONTCARE;
            for (Player player : players) {
                int gamespeed = player.getPreferredGamespeed();
                if (gamespeed != GAMESPEED_DONTCARE) {
                    if (new_gamespeed != GAMESPEED_DONTCARE && gamespeed != new_gamespeed)
                        return;
                    new_gamespeed = gamespeed;
                }
            }
		if (new_gamespeed != GAMESPEED_DONTCARE && new_gamespeed != gamespeed) {
			gamespeed = new_gamespeed;
			getNotificationListener().gamespeedChanged(gamespeed);
		}
	}

	public void tick(float t) {
		getAnimationManagerGameTime().runAnimations(getSecondsPerTick()*t/AnimationManager.ANIMATION_SECONDS_PER_TICK);
		getAnimationManagerRealTime().runAnimations(t/*AnimationManager.ANIMATION_SECONDS_PER_TICK*/);
	}

	public int getTick() {
		return getAnimationManagerRealTime().getTick();
	}

	private World(@NonNull AudioImplementation audio_implementation, @NonNull LandscapeResources landscape_resources, @Nullable RacesResources races_resources, @NonNull NotificationListener notification_listener, @NonNull WorldParameters world_params, @NonNull WorldInfo world_info, Landscape.@NonNull TerrainType terrain, @NonNull PlayerInfo @NonNull [] player_infos, @NonNull Vector4fc @NonNull [] colors, com.oddlabs.tt.resource.@NonNull FogInfo fog) {
		IO.println("****************** Generating landscape at tick " + LocalEventQueue.getQueue().getHighPrecisionManager().getTick() + " ********************");
        this.fog = fog;
		this.landscape_resources = landscape_resources;
		this.races_resources = races_resources;
		this.audio_impl = audio_implementation;
		this.max_unit_count = world_params.getMaxUnitCount();
		this.notification_listener = notification_listener;
		this.gamespeed = world_params.getInitialGameSpeed();
		long time_start = System.currentTimeMillis();

		world = new HeightMap(this, world_info.meters_per_world, world_info.sea_level_meters, world_info.texels_per_colormap, world_info.chunks_per_colormap, world_info.heightmap, world_info.trees, world_info.access_grid, world_info.build_grid);
		animation_manager_game_time = new AnimationManager();
		animation_manager_real_time = new AnimationManager();
		random = new Random(42);

        Iterator<@NonNull Vector4fc> eachColor = Arrays.asList(colors).iterator();
        players = Arrays.stream(player_infos)
                .map(info -> new Player(this, info, eachColor.next()))
                .toArray(Player[]::new);

		long time_stop = System.currentTimeMillis();
		IO.println("****************** Finished landscape in " + ((time_stop - time_start) / 1000f) + " sec ********************");
		this.supply_managers = new SupplyManagers(this);
		this.unit_grid = new UnitGrid(world);
		RegionBuilder.buildRegions(unit_grid, world_info.starting_locations[0][0], world_info.starting_locations[0][1]);
		this.patch_root = new PatchGroup(this);
		this.tree_root = AbstractTreeGroup.newRoot(this, world_info.trees, world_info.palm_trees, terrain);
		this.element_root = AbstractElementNode.newRoot(world);
		AbstractElementNode.buildSupplies(this, world_info.iron, world_info.rocks, world_info.plants, terrain);
	}

	public @NonNull AbstractElementNode getElementRoot() {
		return element_root;
	}

	public @NonNull AbstractTreeGroup getTreeRoot() {
		return tree_root;
	}

	public @NonNull AbstractPatchGroup getPatchRoot() {
		return patch_root;
	}

	public @NonNull UnitGrid getUnitGrid() {
		return unit_grid;
	}

	public @Nullable SupplyManager getSupplyManager(@NonNull Class<? extends Supply> cl) {
		return supply_managers.getSupplyManager(cl);
	}

	public @NonNull Player @NonNull [] getPlayers() {
		return players;
	}

	public int getMaxUnitCount() {
		return max_unit_count;
	}

	public @NonNull NotificationListener getNotificationListener() {
		return notification_listener;
	}

	public @NonNull HeightMap getHeightMap() {
		return world;
	}

	public @NonNull AnimationManager getAnimationManagerGameTime() {
		return animation_manager_game_time;
	}

	public @NonNull AnimationManager getAnimationManagerRealTime() {
		return animation_manager_real_time;
	}

	public @NonNull Random getRandom() {
		return random;
	}
}
