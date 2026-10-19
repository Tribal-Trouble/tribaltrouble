package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simulation scenarios with no window. Each ends by asserting the world checksum against a stored value, so any
 * change to simulation behaviour fails here. Re-baseline the checksum only in a commit that bumps SIM_VERSION.
 */
class HeadlessWorldTest {
    private static final int SEED = 42;
    private static final int PLAYERS = 2;
    private static final int TICKS_PER_MINUTE = 3000;

    /** World checksum after {@link #buildQuarters} on seed 42. Changes only with an intended sim change. */
    private static final int QUARTERS_CHECKSUM = 33682251;

    private static WorldParameters defaults() {
        return WorldParameters.builder().initialGameSpeed(Game.GAMESPEED_NORMAL).mapcode("").initialUnitCount(
                Player.INITIAL_UNIT_COUNT).maxUnitCount(Player.DEFAULT_MAX_UNIT_COUNT).mapSize(
                        Game.SIZE_MEDIUM).build();
    }

    private static World buildQuarters() {
        World world = HeadlessWorld.create(SEED, PLAYERS, defaults());
        Player player = world.getPlayers()[0];
        Selectable<?>[] peons = HeadlessWorld.builders(player);
        int[] site = HeadlessWorld.nearestSite(world, player, Race.BUILDING_QUARTERS, peons[0].getPositionX(),
                peons[0].getPositionY(), 20);
        player.placeBuilding(peons, Race.BUILDING_QUARTERS, site[0], site[1]);
        HeadlessWorld.tick(world, TICKS_PER_MINUTE);
        return world;
    }

    @Test
    void startsWithTheConfiguredPeons() {
        World world = HeadlessWorld.create(SEED, PLAYERS, defaults());
        for (Player player : world.getPlayers()) {
            assertEquals(Player.INITIAL_UNIT_COUNT, player.getUnitCountContainer().getNumSupplies(),
                    player.getPlayerInfo().getName());
            assertEquals(Player.DEFAULT_MAX_UNIT_COUNT, player.getUnitCountContainer().getMaxSupplyCount());
        }
    }

    @Test
    void peonsBuildQuartersWithinAMinute() {
        World world = buildQuarters();
        Player player = world.getPlayers()[0];
        assertEquals(1, player.getBuildingCountContainer().getNumSupplies(), "quarters built");
        assertEquals(0, world.getPlayers()[1].getBuildingCountContainer().getNumSupplies(), "other player idle");
    }

    @Test
    void simulationIsDeterministic() {
        assertEquals(buildQuarters().getChecksum(), buildQuarters().getChecksum());
    }

    @Test
    void simulationMatchesStoredChecksum() {
        assertEquals(QUARTERS_CHECKSUM, buildQuarters().getChecksum(),
                "simulation changed; bump SIM_VERSION and re-baseline if intended");
    }
}
