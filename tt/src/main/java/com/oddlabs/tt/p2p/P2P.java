package com.oddlabs.tt.p2p;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.ConnectionListenerInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Holds the process-wide {@link P2PProvider}. Defaults to a no-op provider so call sites can use
 * {@code P2P.get()} unconditionally; {@link P2PProvider#isAvailable()} gates the feature.
 */
public final class P2P {
    private static @NonNull P2PProvider provider = new NoneProvider();

    private P2P() {
    }

    public static void install(@NonNull P2PProvider p) {
        provider = p;
    }

    public static @NonNull P2PProvider get() {
        return provider;
    }

    private static final class NoneProvider implements P2PProvider {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public @NonNull String getPlatformName() {
            return "";
        }

        @Override
        public @NonNull String getLocalName() {
            return "";
        }

        @Override
        public boolean isHost() {
            return false;
        }

        @Override
        public boolean isJoiner() {
            return false;
        }

        @Override
        public void startHosting(@NonNull Game game) {
        }

        @Override
        public void openInviteDialog() {
        }

        @Override
        public void gameStarted() {
        }

        @Override
        public void leave() {
        }

        @Override
        public void setJoinHandler(@Nullable JoinHandler handler) {
        }

        @Override
        public void setFailureAction(@Nullable Runnable action) {
        }

        @Override
        public void handleLaunchArguments(@NonNull String @NonNull [] args) {
        }

        @Override
        public @NonNull AbstractConnection connectToHost(int channel,
                @Nullable ConnectionInterface conn_interface) {
            throw new IllegalStateException("No P2P provider installed");
        }

        @Override
        public @NonNull AbstractConnectionListener listen(int channel,
                @NonNull ConnectionListenerInterface listener_interface) {
            throw new IllegalStateException("No P2P provider installed");
        }

        @Override
        public void pump() {
        }
    }
}
