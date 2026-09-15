package com.oddlabs.tt.p2p;

import org.jspecify.annotations.NonNull;

/**
 * Identifies a remote peer on an incoming P2P connection, provider-neutrally: an opaque numeric
 * peer id plus the platform display name. Server dispatches on this the same way it dispatches on
 * InetAddress and TunnelIdentifier.
 */
public record P2PIdentifier(long peerID, @NonNull String name) {
}
