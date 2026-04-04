package com.oddlabs.router;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.IllegalARMIEventException;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

final class RouterClient implements ConnectionInterface {
    private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(RouterInterface.class);
    private final @NonNull RouterClientInterface client_interface;
    private final Logger logger;
    private final AbstractConnection connection;
    private final Router router;
    private final List<Integer> checksums = new LinkedList<>();
    private int client_id;
    private SessionManager.Timeout timeout;
    private Session session;
    private Interface current_interface;

    RouterClient(final @NonNull SessionManager session_manager, AbstractConnection conn, Logger logger, Router router) {
        this.router = router;
        this.connection = conn;
        this.logger = logger;
        this.client_interface = (RouterClientInterface) ARMIEvent.createProxy(conn, RouterClientInterface.class);
        this.current_interface = new Interface(RouterInterface.class, (RouterInterface) (SessionID session_id, SessionInfo session_info, int client_id1) -> {
            Session session1 = session_manager.get(session_id, session_info, client_id1);
            doLogin(session1, session_info, client_id1);
        });
    }

    @NonNull List<Integer> getChecksums() {
        return checksums;
    }

    Session getSession() {
        return session;
    }

    void setTimeout(SessionManager.Timeout timeout) {
        this.timeout = timeout;
    }

    SessionManager.Timeout getTimeout() {
        return timeout;
    }

    void heartbeat(int millis) {
        client_interface.heartbeat(millis);
    }

    @NonNull RouterClientInterface getInterface() {
        return client_interface;
    }

    @Override
    public void writeBufferDrained(AbstractConnection conn) {
        if (session != null && session.isComplete())
            session.startTimeout(this);
    }

    int getClientID() {
        return client_id;
    }

    private void doLogin(@NonNull Session session, SessionInfo session_info, int client_id) {
        this.session = session;
        this.client_id = client_id;
        this.current_interface = new Interface(GameInterface.class, new GameInterface() {
            @Override
            public void checksum(int checksum) {
                doChecksum(checksum);
            }

            @Override
            public void relayEventTo(int client_id, ARMIEvent event) {
                doRelayEventTo(client_id, event);
            }

            @Override
            public void relayGameStateEvent(ARMIEvent event) {
                doRelayGameStateEvent(event);
            }

            @Override
            public void relayEvent(ARMIEvent event) {
                doRelayEvent(event);
            }
        });
        session.addPlayer(this);
        logger.log(Level.INFO, "Player logged in: session = {0} client_id = {1}", new Object[]{session, client_id});
    }

    private void doChecksum(int checksum) {
        checksums.add(checksum);
        session.checksum();
/*		if (!session.checksum()) {
			doError(true, new IOException("Checksum mismatch"));
		}*/
    }

    private void doRelayGameStateEvent(final ARMIEvent event) {
        final int next_tick = session.getNextTick();
        session.visit((RouterClient client) -> client.client_interface.receiveGameStateEvent(client_id, next_tick, event));
    }

    private void doRelayEvent(final ARMIEvent event) {
        session.visit((RouterClient client) -> client.client_interface.receiveEvent(client_id, event));
    }

    private void doRelayEventTo(final int receiver_client_id, final ARMIEvent event) {
        session.visit((RouterClient client) -> {
            if (client.client_id == receiver_client_id)
                client.client_interface.receiveEvent(client_id, event);
        });
    }

    @Override
    public void handle(Object sender, @NonNull ARMIEvent event) {
        try {
            event.execute(current_interface.methods, current_interface.instance);
        } catch (IllegalARMIEventException e) {
            doError(false, e);
        }
    }

    @Override
    public void connected(AbstractConnection conn) {
    }

    @Override
    public void error(AbstractConnection conn, IOException e) {
        doError(false, e);
    }

    void close(final boolean checksum_error) {
        connection.close();
        if (session != null) {
            logger.log(Level.INFO, "Removing client: {0}", this);
            session.removePlayer(this);
            session.visit((RouterClient client) -> client.client_interface.playerDisconnected(client_id, checksum_error));
        }
    }

    void doError(final boolean checksum_error, Exception e) {
        close(checksum_error);
        router.removeClient(this);
        logger.log(Level.INFO, "Client disconnected, reason: {0}", e);
    }

    private record Interface(@NonNull ARMIInterfaceMethods methods, Object instance) {
        private Interface(@NonNull Class<?> methods, Object instance) {
            this(new ARMIInterfaceMethods(methods), instance);
        }
    }
}
