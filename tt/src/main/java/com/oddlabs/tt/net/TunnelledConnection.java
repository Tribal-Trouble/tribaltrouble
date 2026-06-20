package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.HostSequenceID;
import com.oddlabs.tt.render.Renderer;

import java.io.IOException;

public final class TunnelledConnection extends AbstractConnection {
    private final HostSequenceID address;
    private boolean open = true;

    public TunnelledConnection(HostSequenceID address, ConnectionInterface conn_interface) {
        setConnectionInterface(conn_interface);
        this.address = address;
        Renderer.getRenderer().getNetwork().getMatchmakingClient().registerTunnel(this.address, this);
        notifyConnected();
    }

    public TunnelledConnection(int address, ConnectionInterface conn_interface) {
        setConnectionInterface(conn_interface);
        this.address = Renderer.getRenderer().getNetwork().getMatchmakingClient().registerTunnel(address, this);
    }

    public void tunnelClosed() {
        open = false;
        notifyError(new IOException("Connection closed"));
    }

    public void connected() {
        notifyConnected();
    }

    public void accept() {
        Renderer.getRenderer().getNetwork().getMatchmakingClient().getInterface().acceptTunnel(address);
    }

    @Override
    public void handle(ARMIEvent event) {
        Renderer.getRenderer().getNetwork().getMatchmakingClient().getInterface().routeEvent(address, event);
        writeBufferDrained();
    }

    public HostSequenceID getAddress() {
        return address;
    }

    @Override
    protected void doClose() {
        if (open) {
            Renderer.getRenderer().getNetwork().getMatchmakingClient().unregisterTunnel(address, this);
            open = false;
        }
    }
}
