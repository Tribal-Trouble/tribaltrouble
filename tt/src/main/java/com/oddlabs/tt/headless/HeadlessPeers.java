package com.oddlabs.tt.headless;

import com.oddlabs.event.NotDeterministic;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.Router;
import com.oddlabs.router.SessionID;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.NotificationListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.net.DistributableTable;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.net.StallHandler;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Several peers in one JVM, each with its own world and peer hub, all joined to one in-process router over the
 * loopback interface: the lockstep multiplayer path with no window, no matchmaker and no wall clock. Orders go
 * through {@link PeerHub#getPlayerInterface()} exactly as they do in a real game, so both worlds must agree.
 */
public final class HeadlessPeers implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(HeadlessPeers.class.getName());
    private static final long MILLIS_PER_TICK = AnimationManager.ANIMATION_MILLISECONDS_PER_TICK;

    public record Peer(@NonNull World world, @NonNull PeerHub hub, @NonNull AnimationManager manager,
                       @NonNull DistributableTable table, @NonNull Player localPlayer) {

        public @NonNull PlayerInterface orders() {
            return hub.getPlayerInterface();
        }
    }

    private final @NonNull NetworkSelector network;
    private final @NonNull Router router;
    private final @NonNull Peer @NonNull [] peers;
    private long clock;

    public HeadlessPeers(int seed, int num_players, @NonNull WorldParameters params) {
        // The router and the hubs read this clock, so ticks run as fast as the loop instead of at real time.
        network = new NetworkSelector(new NotDeterministic(), () -> clock);
        router = new Router(network, com.oddlabs.util.Utils.getLoopbackAddress(), 0, logger, (IOException e) -> {
            throw new RuntimeException(e);
        });
        SessionID session = new SessionID(seed);
        peers = new Peer[num_players];
        for (int i = 0; i < num_players; i++) {
            DistributableTable table = new DistributableTable();
            World world = HeadlessWorld.create(seed, num_players, params, new NotificationListener() {
                @Override
                public void registerTarget(@NonNull Target target) {
                    table.register(target);
                }

                @Override
                public void unregisterTarget(@NonNull Target target) {
                    table.unregister(target);
                }
            });
            Player[] players = world.getPlayers();
            PlayerSlot[] slots = new PlayerSlot[players.length];
            for (int j = 0; j < players.length; j++) {
                slots[j] = PlayerSlot.human(j, players[j].getPlayerInfo());
            }
            AnimationManager manager = new AnimationManager();
            final int index = i;
            PeerHub hub = new PeerHub(manager, players[i], slots, network, table, session, new StallHandler() {
                @Override
                public void stopStall() {
                }

                @Override
                public void processStall(int tick) {
                }

                @Override
                public void peerhubFailed() {
                    throw new IllegalStateException("peer hub " + index + " failed");
                }
            }, router.getPort());
            peers[i] = new Peer(world, hub, manager, table, players[i]);
        }
    }

    public @NonNull Peer peer(int index) {
        return peers[index];
    }

    public int size() {
        return peers.length;
    }

    /** Advances the clock one tick and pumps the router and every hub once. */
    public void tick() {
        clock += MILLIS_PER_TICK;
        network.tick();
        router.process();
        for (Peer peer : peers) {
            peer.manager().runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        }
    }

    public void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            tick();
        }
    }

    /** Pumps until every world has reached at least {@code tick}, or gives up after {@code max_pumps}. */
    public void runUntilTick(int tick, int max_pumps) {
        for (int i = 0; i < max_pumps; i++) {
            boolean done = true;
            for (Peer peer : peers) {
                done &= peer.world().getTick() >= tick;
            }
            if (done) {
                return;
            }
            tick();
        }
        throw new IllegalStateException("worlds did not reach tick " + tick + ": " + ticksReached());
    }

    public @NonNull String ticksReached() {
        StringBuilder out = new StringBuilder();
        for (Peer peer : peers) {
            out.append(peer.world().getTick()).append(' ');
        }
        return out.toString().trim();
    }

    /** The peer's own copy of a selectable that lives in another peer's world, found by wire name. */
    public @NonNull Selectable<?> sameObject(@NonNull Peer owner, @NonNull Selectable<?> object, @NonNull Peer other) {
        return (Selectable<?>) other.table().getDistributable(owner.table().getName(object));
    }

    @Override
    public void close() {
        for (Peer peer : peers) {
            peer.hub().close();
        }
        router.close();
    }
}
