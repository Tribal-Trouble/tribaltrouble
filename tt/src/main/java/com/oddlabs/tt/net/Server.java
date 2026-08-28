package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.RosterTemplate;
import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionListener;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.p2p.P2P;
import com.oddlabs.tt.p2p.P2PIdentifier;
import com.oddlabs.tt.p2p.P2PProvider;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

public final class Server implements ConnectionListenerInterface {
    private static final int NEGOTIATING = 1;
    private static final int SYNCHRONIZING = 2;
    private static final int CLOSED = 3;

    private static final int JOIN_DEFAULT_NONE = -1;

    private final PlayerSlot @NonNull [] players;
    // Per-slot race/team a joining human inherits, seeded in-process from the host's create-dialog roster. Slots left
    // at JOIN_DEFAULT_NONE fall back to the legacy random race / slot-derived team.
    private final int @NonNull [] join_default_race;
    private final int @NonNull [] join_default_team;
    private final String[] ai_names;
    private final WorldGenerator generator;
    private final Game game;
    private final @NonNull AbstractConnectionListener local_listener;
    private final Map<AbstractConnection, ClientConnection> connection_to_client = new LinkedHashMap<>();
    private final @NonNull Random random;
    private AbstractConnectionListener tunnelled_listener;

    private int state = NEGOTIATING;
    private final boolean register_server;

    public Server(@NonNull NetworkSelector network, Game game, InetAddress ip, WorldGenerator generator,
            boolean register_server, String[] ai_names) {
        this(network, game, ip, generator, register_server, ai_names, MatchmakingServerInterface.MAX_PLAYERS);
    }

    public Server(@NonNull NetworkSelector network, Game game, InetAddress ip, WorldGenerator generator,
            boolean register_server, String[] ai_names, int player_count) {
        this.local_listener = new ConnectionListener(network, ip, Globals.NET_PORT, this);
        this.game = game;
        this.generator = generator;
        this.register_server = register_server;
        this.ai_names = ai_names;
        this.random = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
        players = new PlayerSlot[player_count];
        for (short i = 0; i < players.length; i++) {
            players[i] = new PlayerSlot(i);
            players[i].setReady(i != 0);
        }
        join_default_race = new int[player_count];
        join_default_team = new int[player_count];
        Arrays.fill(join_default_race, JOIN_DEFAULT_NONE);
        Arrays.fill(join_default_team, JOIN_DEFAULT_NONE);
    }

    /**
     * Seeds the race/team a joining human inherits per slot from the host's create-dialog roster. The server runs in
     * the host's own process, so this is a direct in-process call: nothing crosses the wire. Joiners simply receive the
     * host's intended race/team in the normal player broadcast instead of a random race and slot-derived team. Slots
     * the roster leaves unspecified keep the legacy defaults, so un-updated clients and the join handshake are
     * unaffected.
     */
    public void applyRosterJoinDefaults(@NonNull RosterTemplate roster) {
        RosterTemplate.Slot[] slots = roster.getSlots();
        for (int i = 0; i < players.length && i < slots.length; i++) {
            Integer race = slots[i].getRace();
            Integer team = slots[i].getTeam();
            if (race != null)
                join_default_race[i] = race;
            if (team != null)
                join_default_team[i] = team;
        }
    }

    private @NonNull Iterator<ClientConnection> getClientIterator() {
        return connection_to_client.values().iterator();
    }

    private int getNumClients() {
        return connection_to_client.size();
    }

    private ClientConnection getClientFromConnection(AbstractConnection conn) {
        return connection_to_client.get(conn);
    }

    private void unregisterGame() {
        // local_listener can still be null if construction failed inside `new ConnectionListener(...)` (e.g. the port
        // is already bound): the listener's constructor calls back into error() -> close() before the field is
        // assigned. Guard so the real IOException surfaces instead of an NPE that masks it.
        if (local_listener != null)
            local_listener.close();
        if (tunnelled_listener != null)
            tunnelled_listener.close();
        if (register_server && Network.getMatchmakingClient().isConnected()) {
            Network.getMatchmakingClient().getInterface().unregisterGame();
        }
    }

    private void unregister() {
        state = CLOSED;
    }

    private void closeConnections() {
        for (AbstractConnection conn : connection_to_client.keySet()) {
            conn.close();
        }
        connection_to_client.clear();
        unregister();
    }

    public void close() {
        unregisterGame();
        closeConnections();
    }

    private int getNumReady() {
        int count = 0;
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            if (client.getClient().getPlayerSlot().isReady())
                count++;
        }
        return count;
    }

    @Override
    public void error(AbstractConnectionListener listener, IOException e) {
        IO.println("Listener failed: " + e);
        close();
    }

    public void handleError(AbstractConnection conn, Exception e) {
        IO.println("Disconnecting client because of exception: " + e);
        ClientConnection client = getClientFromConnection(conn);
        if (client != null) {
            disconnectClient(client);
            if (state == NEGOTIATING) {
                resetSlotState(client.getClient().getPlayerSlot(), true);
            }
        }
    }

    private void disconnectClient(@NonNull ClientConnection client) {
        assert client != null;
        client.getConnection().close();
        connection_to_client.remove(client.getConnection());
    }

    private @Nullable ClientConnection locateClientForSlot(PlayerSlot player_slot) {
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            if (client.getClient().getPlayerSlot() == player_slot)
                return client;
        }
        return null;
    }

    public void resetSlotState(@NonNull PlayerSlot client_slot, int slot, boolean open) {
        if (!canControlSlot(client_slot, slot))
            return;
        resetSlotState(players[slot], open);
    }

    private void resetSlotState(@NonNull PlayerSlot client_slot, boolean open) {
        client_slot.setType(open ? PlayerSlot.OPEN : PlayerSlot.CLOSED);
        client_slot.setInfo(null);
        client_slot.setAddress(null);
        client_slot.setReady(true);
        client_slot.setAIDifficulty(PlayerSlot.AI_NONE);
        ClientConnection player_client = locateClientForSlot(client_slot);
        if (player_client != null)
            disconnectClient(player_client);
        broadcastPlayers(true);
    }

    private boolean canControlSlot(@NonNull PlayerSlot client_slot, int slot) {
        return slot >= 0 && slot < players.length && state == NEGOTIATING &&
                ((client_slot.getSlot() == 0 || client_slot.getSlot() == slot));
    }

    public void startServer(@NonNull PlayerSlot slot) {
        if (!canControlSlot(slot, 0) || getNumReady() != getNumClients())// || PlayerSlot.getNumTeams(players) < 2)
            return;
        state = SYNCHRONIZING;
        unregisterGame();
        broadcastInits();
    }

    public void setPlayerSlot(@NonNull PlayerSlot client_slot, int slot, int type, int race, int team, boolean ready,
            int ai_difficulty) {
        if (!PlayerSlot.isValidType(type) || !RacesResources.isValidRace(race))
            return;
        if (!canControlSlot(client_slot, slot) || (client_slot.getSlot() == slot && type != PlayerSlot.HUMAN))
            return;
        PlayerSlot player_slot = players[slot];
//		PlayerInfo player_info = players[slot];
        ClientConnection player_client = locateClientForSlot(player_slot);
        if (player_client != null && type != PlayerSlot.HUMAN)
            disconnectClient(player_client);
        String name;
        if (type == PlayerSlot.AI) {
            name = ai_names[slot];
        } else {
            name = player_slot.getInfo().getName();
        }
        PlayerInfo player_info = new PlayerInfo(team, race, name);
        boolean reset_ready = player_slot.getInfo() == null || type != player_slot.getType()
                || ai_difficulty != player_slot.getAIDifficulty() || !player_info.equals(player_slot.getInfo());
        player_slot.setType(type);
        player_slot.setAIDifficulty(ai_difficulty);
        player_slot.setInfo(player_info);
        player_slot.setReady(type != PlayerSlot.HUMAN || ready);
        broadcastPlayers(reset_ready);
    }

    private void resetReady() {
        int num_humans = 0;
        for (PlayerSlot player_slot : players) {
            if (player_slot.getType() == PlayerSlot.HUMAN)
                num_humans++;
        }
        if (num_humans > 1) {
            for (PlayerSlot player_slot : players) {
                if (player_slot.getType() == PlayerSlot.HUMAN)
                    player_slot.setReady(false);
            }
        }
    }

    private void broadcastPlayers(boolean reset_ready) {
        if (reset_ready)
            resetReady();
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            client.getClientInterface().setPlayers(players);
        }
    }

    public void chat(@NonNull PlayerSlot player_slot, String chat) {
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            client.getClientInterface().chat(player_slot.getSlot(), chat);
        }
    }

    private void broadcastInits() {
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            int session_id = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick()).nextInt();
            client.getClientInterface().startGame(session_id);
        }
    }

    private short locateAvailableSlot() {
        for (short i = 0; i < players.length; i++) {
            if (players[i].getType() == PlayerSlot.OPEN)
                return i;
        }
        return (short) -1;
    }

    @Override
    public void incomingConnection(@NonNull AbstractConnectionListener connection_listener, Object remote_address) {
        IO.println("Incoming host connection from " + remote_address);
        short available_slot = locateAvailableSlot();
        if (state != NEGOTIATING || available_slot == -1 ||
                (remote_address instanceof InetAddress address && !address.isLoopbackAddress()) ||
                (remote_address instanceof TunnelIdentifier identifier && game != null && game.isRated() &&
                        identifier.profile().getWins() < GameSession.MIN_WINS_FOR_RANKING)) {
            IO.println(
                    "rejecting incoming connection since state = " + state + " | locateAvailableSlot() = " + available_slot + " remote_address = " + remote_address);
            connection_listener.rejectConnection();
            return;
        }
        PlayerSlot player_slot = players[available_slot];
        int rating = 0;
        String name;
        TunnelAddress address;
        if (remote_address instanceof InetAddress && P2P.get().isHost()) {
            // Serverless P2P match: the host's own client connects over loopback; remote joiners
            // arrive over the P2P transport instead of a matchmaking tunnel.
            address = null;
            if (register_server)
                tunnelled_listener = P2P.get().listen(P2PProvider.CHANNEL_LOBBY, this);
            name = P2P.get().isAvailable() ? P2P.get().getLocalName() : Utils.getBundleString(
                    ResourceBundle.getBundle(MatchmakingClient.class.getName()), "player");
        } else if (remote_address instanceof P2PIdentifier identifier) {
            address = null;
            name = identifier.name();
        } else if (remote_address instanceof InetAddress) {
            address = Network.getMatchmakingClient().getLocalAddress();
            if (register_server) {
                tunnelled_listener = new TunnelledConnectionListener(this);
                Network.getMatchmakingClient().getInterface().registerGame(game);
            }
            Profile profile = Network.getMatchmakingClient().getProfile();
            if (profile != null) {
                name = profile.getNick();
                rating = profile.getRating();
            } else
                name = Utils.getBundleString(ResourceBundle.getBundle(MatchmakingClient.class.getName()), "player");
        } else {
            TunnelIdentifier tunnel_id = (TunnelIdentifier) remote_address;
            name = tunnel_id.profile().getNick();
            rating = tunnel_id.profile().getRating();
            address = tunnel_id.address();
        }
        player_slot.setReady(false);
        int max_teams = players.length;
        if (game != null && game.isRated())
            max_teams = 2;
        int race = join_default_race[available_slot] != JOIN_DEFAULT_NONE ? join_default_race[available_slot] : random.nextInt(
                RacesResources.getNumRaces());
        int team = join_default_team[available_slot] != JOIN_DEFAULT_NONE ? join_default_team[available_slot] % max_teams : available_slot % max_teams;
        PlayerInfo player_info = new PlayerInfo(team, race, name);
        player_slot.setRating(rating);
        player_slot.setType(PlayerSlot.HUMAN);
        player_slot.setAddress(address);
        player_slot.setInfo(player_info);
        ClientInfo client = new ClientInfo(this, player_slot);
        AbstractConnection conn = connection_listener.acceptConnection(client);
        ClientConnection client_conn = new ClientConnection(conn, client);
        connection_to_client.put(conn, client_conn);
        client_conn.getClientInterface().setWorldGeneratorAndPlayerSlot(game, generator, available_slot,
                players.length);
        broadcastPlayers(true);
    }
}
