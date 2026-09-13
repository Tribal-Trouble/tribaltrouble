package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two peers, two worlds, one in-process router: the real lockstep path with no window. Orders issued on one peer
 * must show up in the other's world, and both worlds must stay checksum-identical.
 */
class HeadlessPeersTest {
    private static final int SEED = 42;
    private static final int PEERS = 2;
    private static final int TICKS_PER_MINUTE = 3000;
    private static final int MAX_PUMPS = 20000;

    private static WorldParameters defaults() {
        return WorldParameters.builder().initialGameSpeed(Game.GAMESPEED_NORMAL).mapcode("").initialUnitCount(
                Player.INITIAL_UNIT_COUNT).maxUnitCount(Player.DEFAULT_MAX_UNIT_COUNT).mapSize(
                        Game.SIZE_MEDIUM).build();
    }

    @Test
    void peersStayInLockstep() {
        try (HeadlessPeers peers = new HeadlessPeers(SEED, PEERS, defaults())) {
            peers.runUntilTick(TICKS_PER_MINUTE, MAX_PUMPS);
            assertEquals(peers.peer(0).world().getChecksum(), peers.peer(1).world().getChecksum(),
                    "worlds diverged with no orders");
        }
    }

    @Test
    void ordersFromOnePeerReachTheOther() {
        try (HeadlessPeers peers = new HeadlessPeers(SEED, PEERS, defaults())) {
            peers.runUntilTick(50, MAX_PUMPS);
            HeadlessPeers.Peer host = peers.peer(0);
            Player player = host.localPlayer();
            Selectable<?>[] peons = HeadlessWorld.builders(player);
            int[] site = HeadlessWorld.nearestSite(host.world(), player, Race.BUILDING_QUARTERS,
                    peons[0].getPositionX(), peons[0].getPositionY(), 20);
            host.orders().placeBuilding(peons, Race.BUILDING_QUARTERS, site[0], site[1]);
            peers.runUntilTick(50 + TICKS_PER_MINUTE, MAX_PUMPS);
            for (int i = 0; i < PEERS; i++) {
                Player copy = peers.peer(i).world().getPlayers()[0];
                assertEquals(1, copy.getBuildingCountContainer().getNumSupplies(), "quarters in world " + i);
            }
            assertEquals(peers.peer(0).world().getChecksum(), peers.peer(1).world().getChecksum(),
                    "worlds diverged after an order");
        }
    }
}
