package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.AudioImplementation;
import com.oddlabs.tt.landscape.LandscapeResources;
import com.oddlabs.tt.landscape.NotificationListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import org.jspecify.annotations.NonNull;

/**
 * Builds and ticks a world with no window, no GL context and no audio device: the simulation as a library, for
 * tests. Terrain comes from {@link IslandGenerator#generateTerrain}; textures are never baked.
 */
public final class HeadlessWorld {
    private static final int METERS_PER_WORLD_MEDIUM = 512;

    private HeadlessWorld() {
    }

    /** A no-op audio implementation: every sound becomes a player that does nothing. */
    public static @NonNull AudioImplementation silentAudio() {
        return params -> new AbstractAudioPlayer(null, params) {
        };
    }

    public static @NonNull World create(int seed, int num_players, @NonNull WorldParameters params) {
        if (Settings.getSettings() == null) {
            Settings.setSettings(new Settings());
        }
        IslandGenerator generator = new IslandGenerator(METERS_PER_WORLD_MEDIUM, Landscape.TerrainType.NATIVE, .5f,
                .5f, .5f, seed, false);
        WorldInfo terrain = generator.generateTerrain(num_players, params.getInitialUnitCount(), 0f);
        RenderQueues queues = new RenderQueues();
        LandscapeResources landscape_resources = new LandscapeResources(queues);
        RacesResources races_resources = new RacesResources(queues);
        PlayerInfo[] infos = new PlayerInfo[num_players];
        for (int i = 0; i < num_players; i++) {
            infos[i] = new PlayerInfo(i, i % 2, "Player " + (i + 1));
        }
        return World.newWorld(silentAudio(), landscape_resources, races_resources, new NotificationListener() {
        }, params, terrain, generator.getTerrainType(), infos, generator.getFogInfo());
    }

    /** Runs one simulation tick at the world's current game speed. */
    public static void tick(@NonNull World world) {
        world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
    }

    public static void main(String[] args) {
        int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 500;
        WorldParameters params = WorldParameters.builder().initialGameSpeed(Game.GAMESPEED_NORMAL).mapcode(
                "").initialUnitCount(Player.INITIAL_UNIT_COUNT).maxUnitCount(Player.DEFAULT_MAX_UNIT_COUNT).mapSize(
                        Game.SIZE_MEDIUM).build();
        long start = System.currentTimeMillis();
        World world = create(42, 2, params);
        long built = System.currentTimeMillis();
        for (int i = 0; i < ticks; i++) {
            tick(world);
        }
        long done = System.currentTimeMillis();
        for (Player p : world.getPlayers()) {
            System.out.println("player " + p.getPlayerInfo().getName() + " units " + p.getUnits().size());
        }
        System.out.println("tick " + world.getTick() + " checksum " + world.getChecksum());
        System.out.println("built in " + (built - start) + " ms, " + ticks + " ticks in " + (done - built) + " ms");
    }
}
