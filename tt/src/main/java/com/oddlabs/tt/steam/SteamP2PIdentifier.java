package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamID;
import org.jspecify.annotations.NonNull;

/**
 * Remote address of an incoming Steam P2P connection, handed to
 * {@code ConnectionListenerInterface.incomingConnection} the way {@code InetAddress} and
 * {@code TunnelIdentifier} are for the other transports.
 */
public record SteamP2PIdentifier(@NonNull SteamID steamID, @NonNull String personaName) {
}
