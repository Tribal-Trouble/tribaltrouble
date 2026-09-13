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
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import org.jspecify.annotations.NonNull;

/**
 * Builds and ticks a world with no window, no GL context and no audio device: the simulation as a library, for
 * tests. Terrain comes from {@link IslandGenerator#generateTerrain}; textures are never baked. Orders go straight
 * to {@link Player}, which is the same interface the network hands them to in a real game.
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

    /** A world with each player's starting peons placed at the generated start positions. */
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
        World world = World.newWorld(silentAudio(), landscape_resources, races_resources, new NotificationListener() {
        }, params, terrain, generator.getTerrainType(), infos, generator.getFogInfo());
        Player[] players = world.getPlayers();
        for (int i = 0; i < players.length; i++) {
            float[] start = terrain.starting_locations()[i];
            for (int j = 0; j < params.getInitialUnitCount(); j++) {
                new Unit(players[i], start[2 * j], start[2 * j + 1], null,
                        players[i].getRace().getUnitTemplate(Race.UNIT_PEON));
            }
        }
        return world;
    }

    /** Runs one simulation tick at the world's current game speed. */
    public static void tick(@NonNull World world) {
        world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
    }

    public static void tick(@NonNull World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            tick(world);
        }
    }

    /** The player's units that can build. */
    public static @NonNull Selectable<?> @NonNull [] builders(@NonNull Player player) {
        return player.getUnits().filter(Abilities.BUILD);
    }

    /** The legal placement cell for {@code building_index} nearest to a point, searched within {@code radius}. */
    public static int @NonNull [] nearestSite(@NonNull World world, @NonNull Player player, int building_index,
            float x, float y, int radius) {
        BuildingTemplate template = player.getRace().getBuildingTemplate(building_index);
        UnitGrid grid = world.getUnitGrid();
        int cx = UnitGrid.toGridCoordinate(x);
        int cy = UnitGrid.toGridCoordinate(y);
        int size = template.getPlacingSize();
        int best_x = -1, best_y = -1, best_d = Integer.MAX_VALUE;
        for (int gy = Math.max(size, cy - radius); gy <= Math.min(grid.getGridSize() - size - 1, cy + radius); gy++) {
            for (int gx = Math.max(size, cx - radius); gx <= Math.min(grid.getGridSize() - size - 1,
                    cx + radius); gx++) {
                int d = Math.abs(gx - cx) + Math.abs(gy - cy);
                if (d < best_d && d >= size && template.isPlacingLegal(grid, gx, gy)) {
                    best_x = gx;
                    best_y = gy;
                    best_d = d;
                }
            }
        }
        if (best_x < 0) {
            throw new IllegalStateException("no legal site within " + radius + " cells of " + x + "," + y);
        }
        return new int[]{best_x, best_y};
    }

    public static void main(String[] args) {
        int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 3000;
        WorldParameters params = WorldParameters.builder().initialGameSpeed(Game.GAMESPEED_NORMAL).mapcode(
                "").initialUnitCount(Player.INITIAL_UNIT_COUNT).maxUnitCount(Player.DEFAULT_MAX_UNIT_COUNT).mapSize(
                        Game.SIZE_MEDIUM).build();
        long start = System.currentTimeMillis();
        World world = create(42, 2, params);
        long built = System.currentTimeMillis();
        Player player = world.getPlayers()[0];
        Selectable<?>[] peons = builders(player);
        int[] site = nearestSite(world, player, Race.BUILDING_QUARTERS, peons[0].getPositionX(),
                peons[0].getPositionY(), 20);
        player.placeBuilding(peons, Race.BUILDING_QUARTERS, site[0], site[1]);
        tick(world, ticks);
        long done = System.currentTimeMillis();
        for (Player p : world.getPlayers()) {
            System.out.println(
                    "player " + p.getPlayerInfo().getName() + " units " + p.getUnits().size() + " unit count " + p.getUnitCountContainer().getNumSupplies() + " buildings " + p.getBuildingCountContainer().getNumSupplies());
        }
        System.out.println(
                "quarters site " + site[0] + " " + site[1] + " tick " + world.getTick() + " checksum " + world.getChecksum());
        System.out.println("built in " + (built - start) + " ms, " + ticks + " ticks in " + (done - built) + " ms");
    }
}
