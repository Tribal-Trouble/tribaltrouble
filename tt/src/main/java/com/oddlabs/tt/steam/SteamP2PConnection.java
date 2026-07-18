package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamID;
import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ConnectionInterface;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A logical connection to a remote Steam user, carried as reliable P2P packets on a fixed channel.
 * The outbound side sends HELLO and becomes connected when the remote accepts; the inbound side is
 * created by the {@link SteamP2P} pump on HELLO and accepted or rejected through
 * {@link SteamP2PConnectionListener}.
 */
public final class SteamP2PConnection extends AbstractConnection {
    private final @NonNull SteamID remote_id;
    private final int channel;
    private boolean open = true;

    /** Outbound connection: connects to remote_id and waits for the remote ACCEPT. */
    public SteamP2PConnection(@NonNull SteamID remote_id, int channel, ConnectionInterface conn_interface) {
        this.remote_id = remote_id;
        this.channel = channel;
        setConnectionInterface(conn_interface);
        SteamP2P.getInstance().register(this);
        SteamP2P.getInstance().sendControl(remote_id, channel, SteamP2P.PACKET_HELLO);
    }

    /** Inbound connection, pending until the listener accepts or rejects it. */
    SteamP2PConnection(@NonNull SteamID remote_id, int channel) {
        this.remote_id = remote_id;
        this.channel = channel;
    }

    public @NonNull SteamID getRemoteID() {
        return remote_id;
    }

    int getChannel() {
        return channel;
    }

    void accept() {
        SteamP2P.getInstance().sendControl(remote_id, channel, SteamP2P.PACKET_ACCEPT);
        notifyConnected();
    }

    void reject() {
        open = false;
        SteamP2P.getInstance().sendControl(remote_id, channel, SteamP2P.PACKET_REJECT);
        SteamP2P.getInstance().unregister(this);
    }

    void remoteAccepted() {
        notifyConnected();
    }

    void remoteEvent(@NonNull ByteBuffer packet) {
        ARMIEvent event = ARMIEvent.read(packet, (short) packet.remaining());
        receiveEvent(event);
    }

    void remoteClosed(@NonNull IOException reason) {
        open = false;
        notifyError(reason);
    }

    @Override
    public void handle(ARMIEvent event) {
        SteamP2P.getInstance().sendEvent(remote_id, channel, event);
        // The drained notification must not fire synchronously: listeners like the router's
        // heartbeat scheduling assume the socket-transport behavior where it arrives on a later
        // tick, and mutating their timeout state reentrantly corrupts it.
        SteamP2P.getInstance().notifyDrainedLater(this);
    }

    void notifyDrained() {
        if (open)
            writeBufferDrained();
    }

    @Override
    protected void doClose() {
        if (open) {
            open = false;
            SteamP2P.getInstance().sendControl(remote_id, channel, SteamP2P.PACKET_CLOSE);
            SteamP2P.getInstance().unregister(this);
        }
    }
}
