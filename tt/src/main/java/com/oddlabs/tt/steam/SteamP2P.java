package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNativeHandle;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import com.oddlabs.net.ARMIEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Steam peer-to-peer transport registry and packet pump. All remote traffic in a serverless Steam
 * match flows through here as one reliable P2P packet per message, addressed by SteamID and
 * multiplexed on two channels: lobby negotiation and in-game router traffic.
 *
 * <p>Single-threaded: every method must be called from the main loop thread, which also drives
 * {@link #pump()} once per animation tick.
 */
public final class SteamP2P implements SteamNetworkingCallback {
    /** Channel for game setup negotiation (Server/Client handshake). */
    public static final int CHANNEL_LOBBY = 0;
    /** Channel for in-game router traffic (RouterClient to the host's embedded Router). */
    public static final int CHANNEL_GAME = 1;

    private static final int NUM_CHANNELS = 2;

    // Packet types. Every packet is [type byte][payload].
    static final byte PACKET_HELLO = 0;
    static final byte PACKET_ACCEPT = 1;
    static final byte PACKET_REJECT = 2;
    static final byte PACKET_EVENT = 3;
    static final byte PACKET_CLOSE = 4;

    private static final int BUFFER_SIZE = 65536;

    private static final Logger logger = Logger.getLogger(SteamP2P.class.getName());

    private static @Nullable SteamP2P instance;

    private final SteamNetworking networking;
    private final ByteBuffer send_buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final ByteBuffer receive_buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final int[] packet_size_out = new int[1];

    /** Established and half-open connections, keyed by remote SteamID handle, per channel. */
    private final Map<Long, SteamP2PConnection>[] connections;
    private final SteamP2PConnectionListener[] listeners = new SteamP2PConnectionListener[NUM_CHANNELS];
    /** HELLOs that arrived before a listener registered on their channel, delivered on register. */
    private final List<SteamP2PConnection>[] pending_incoming;
    /** Connections whose drained notification is due on the next pump; see SteamP2PConnection.handle. */
    private final List<SteamP2PConnection> pending_drained = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private SteamP2P() {
        this.networking = new SteamNetworking(this);
        networking.allowP2PPacketRelay(true);
        this.connections = new Map[NUM_CHANNELS];
        this.pending_incoming = new List[NUM_CHANNELS];
        for (int i = 0; i < NUM_CHANNELS; i++) {
            connections[i] = new HashMap<>();
            pending_incoming[i] = new ArrayList<>();
        }
    }

    /**
     * The transport singleton, created on first use. Requires the Steam API to be initialized;
     * callers must check {@link SteamManager#getInstance()} before starting a Steam session.
     */
    public static @NonNull SteamP2P getInstance() {
        if (instance == null)
            instance = new SteamP2P();
        return instance;
    }

    public static boolean isCreated() {
        return instance != null;
    }

    public static void shutdown() {
        if (instance != null) {
            instance.networking.dispose();
            instance = null;
        }
    }

    /**
     * Drains all pending packets on all channels. Called once per animation tick, alongside the
     * NIO selector tick, so Steam traffic is handled with the same cadence as socket traffic.
     */
    public static void pump() {
        if (instance != null)
            instance.pumpPackets();
    }

    void notifyDrainedLater(@NonNull SteamP2PConnection conn) {
        if (!pending_drained.contains(conn))
            pending_drained.add(conn);
    }

    private void pumpPackets() {
        if (!pending_drained.isEmpty()) {
            List<SteamP2PConnection> drained = new ArrayList<>(pending_drained);
            pending_drained.clear();
            for (SteamP2PConnection conn : drained)
                conn.notifyDrained();
        }
        for (int channel = 0; channel < NUM_CHANNELS; channel++) {
            while (networking.isP2PPacketAvailable(channel, packet_size_out)) {
                SteamID sender = new SteamID();
                receive_buffer.clear();
                int size;
                try {
                    size = networking.readP2PPacket(sender, receive_buffer, channel);
                } catch (SteamException e) {
                    logger.log(Level.WARNING, "readP2PPacket failed", e);
                    break;
                }
                if (size <= 0)
                    break;
                receive_buffer.position(0);
                receive_buffer.limit(size);
                handlePacket(sender, channel, receive_buffer);
            }
        }
    }

    private void handlePacket(@NonNull SteamID sender, int channel, @NonNull ByteBuffer packet) {
        byte type = packet.get();
        long sender_handle = SteamNativeHandle.getNativeHandle(sender);
        SteamP2PConnection conn = connections[channel].get(sender_handle);
        switch (type) {
            case PACKET_HELLO -> {
                if (conn != null)
                    return; // Duplicate HELLO for an existing connection, ignore
                SteamP2PConnection incoming = new SteamP2PConnection(sender, channel);
                connections[channel].put(sender_handle, incoming);
                SteamP2PConnectionListener listener = listeners[channel];
                if (listener != null) {
                    listener.incoming(incoming);
                } else {
                    // The joiner can race ahead of the host here (e.g. its PeerHub connects before
                    // the host's world finished loading), so park the connection until a listener
                    // registers instead of rejecting.
                    pending_incoming[channel].add(incoming);
                }
            }
            case PACKET_ACCEPT -> {
                if (conn != null)
                    conn.remoteAccepted();
            }
            case PACKET_REJECT -> {
                if (conn != null) {
                    connections[channel].remove(sender_handle);
                    conn.remoteClosed(new IOException("Connection rejected by host"));
                }
            }
            case PACKET_EVENT -> {
                if (conn != null)
                    conn.remoteEvent(packet);
            }
            case PACKET_CLOSE -> {
                if (conn != null) {
                    connections[channel].remove(sender_handle);
                    conn.remoteClosed(new IOException("Connection closed"));
                }
            }
            default -> logger.warning("Unknown Steam P2P packet type: " + type);
        }
    }

    void register(@NonNull SteamP2PConnection conn) {
        connections[conn.getChannel()].put(SteamNativeHandle.getNativeHandle(conn.getRemoteID()), conn);
    }

    void unregister(@NonNull SteamP2PConnection conn) {
        long handle = SteamNativeHandle.getNativeHandle(conn.getRemoteID());
        if (connections[conn.getChannel()].get(handle) == conn) {
            connections[conn.getChannel()].remove(handle);
            networking.closeP2PChannelWithUser(conn.getRemoteID(), conn.getChannel());
        }
    }

    void setListener(int channel, @Nullable SteamP2PConnectionListener listener) {
        listeners[channel] = listener;
        if (listener != null && !pending_incoming[channel].isEmpty()) {
            List<SteamP2PConnection> pending = new ArrayList<>(pending_incoming[channel]);
            pending_incoming[channel].clear();
            for (SteamP2PConnection conn : pending)
                listener.incoming(conn);
        }
    }

    void sendControl(@NonNull SteamID remote, int channel, byte type) {
        send_buffer.clear();
        send_buffer.put(type);
        send_buffer.flip();
        send(remote, channel, send_buffer);
    }

    void sendEvent(@NonNull SteamID remote, int channel, @NonNull ARMIEvent event) {
        send_buffer.clear();
        send_buffer.put(PACKET_EVENT);
        event.write(send_buffer);
        send_buffer.flip();
        send(remote, channel, send_buffer);
    }

    private void send(@NonNull SteamID remote, int channel, @NonNull ByteBuffer packet) {
        try {
            if (!networking.sendP2PPacket(remote, packet, SteamNetworking.P2PSend.Reliable, channel))
                logger.warning("sendP2PPacket returned false for " + remote);
        } catch (SteamException e) {
            logger.log(Level.WARNING, "sendP2PPacket failed", e);
        }
    }

    // SteamNetworkingCallback

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote) {
        // Accept sessions while a Steam lobby session is active; the HELLO/ACCEPT handshake above
        // is what actually gates who gets into the game.
        if (SteamLobbySession.isActive() || hasAnyEndpoint()) {
            networking.acceptP2PSessionWithUser(steamIDRemote);
        } else {
            logger.info("Ignoring P2P session request from " + steamIDRemote + " (no active session)");
        }
    }

    private boolean hasAnyEndpoint() {
        for (int i = 0; i < NUM_CHANNELS; i++) {
            if (listeners[i] != null || !connections[i].isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, SteamNetworking.P2PSessionError sessionError) {
        long handle = SteamNativeHandle.getNativeHandle(steamIDRemote);
        for (int channel = 0; channel < NUM_CHANNELS; channel++) {
            SteamP2PConnection conn = connections[channel].remove(handle);
            if (conn != null)
                conn.remoteClosed(new IOException("Steam P2P session failed: " + sessionError));
        }
    }
}
