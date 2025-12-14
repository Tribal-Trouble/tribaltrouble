package com.oddlabs.matchserver;

import com.oddlabs.event.Deterministic;
import com.oddlabs.event.NotDeterministic;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchserver.discord.DiscordBotService;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionListener;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.net.SecureConnection;
import com.oddlabs.registration.RegistrationKey;
import com.oddlabs.util.DBUtils;
import com.oddlabs.util.KeyManager;

import java.io.IOException;
import java.net.InetAddress;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

public final class MatchmakingServer implements ConnectionListenerInterface {

    private static final Map online_users = new HashMap();
    private static int current_id = 1;

    private static final Logger logger = Logger.getLogger("com.oddlabs.matchserver");

    private final Logger chat_logger = Logger.getLogger("chatlog");

    private final AbstractConnectionListener connection_listener;
    private final AlgorithmParameterSpec param_spec;
    private final PublicKey public_reg_key;
    private final NetworkSelector network;
    private final Map client_map = new HashMap();

    /**
     * The server tick timeout in milliseconds. Set to 0 when no users are logged in and only
     * processes things when network traffic comes in. After the first user logs in, it is set to
     * 100ms to allow for regular updates and Discord message processing into the tt chat rooms
     */
    private int server_tick_timeout = 0;

    static {
        try {
            Handler fh = new FileHandler("logs/matchserver.%g.log", 10 * 1024 * 1024, 50);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            e.printStackTrace();
        }
    }

    private MatchmakingServer() throws Exception {
        Deterministic deterministic = new NotDeterministic();
        this.network = new NetworkSelector(deterministic);
        Handler fh = new FileHandler("logs/chatlog.%g.log", 10 * 1024 * 1024, 50);
        fh.setFormatter(new SimpleFormatter());
        chat_logger.addHandler(fh);
        chat_logger.setLevel(Level.ALL);

        this.public_reg_key = RegistrationKey.loadPublicKey();
        String password = ServerConfiguration.getInstance().get(ServerConfiguration.SQL_PASS);
        DBUtils.initConnection("jdbc:mysql://localhost/oddlabs", "matchmaker", password);
        logger.info("Generating encryption keys.");
        this.param_spec = KeyManager.generateParameterSpec();
        connection_listener =
                new ConnectionListener(
                        network, null, MatchmakingServerInterface.MATCHMAKING_SERVER_PORT, this);
        DBInterface.initDropGames();
        DBInterface.clearOnlineProfiles();
        logger.info("Matchmaking server started.");
        while (true) {
            network.tickBlocking(server_tick_timeout);
        }
    }

    public static final Logger getLogger() {
        return logger;
    }

    public final Logger getChatLogger() {
        return chat_logger;
    }

    public final PublicKey getPublicRegKey() {
        return public_reg_key;
    }

    public final AlgorithmParameterSpec getSpec() {
        return param_spec;
    }

    public final void incomingConnection(
            AbstractConnectionListener connection_listener, Object remote_address) {
        int id = current_id++;
        AbstractConnection conn = connection_listener.acceptConnection(null);
        SecureConnection secure_conn =
                new SecureConnection(network.getDeterministic(), conn, param_spec);
        Authenticator client =
                new Authenticator(this, secure_conn, (InetAddress) remote_address, id);
    }

    // public final boolean isKeyOnline(String key_encoded) {
    // return online_keys.contains(key_encoded);
    // }
    public final void loginClient(
            InetAddress remote_address,
            InetAddress local_remote_address,
            String username,
            AbstractConnection conn,
            String key_code_encoded,
            int revision,
            int host_id) {
        // online_keys.add(key_code_encoded);
        Client old_logged_in = (Client) online_users.remove(username.toLowerCase());
        if (old_logged_in != null) {
            old_logged_in.close();
            logger.info(username + " overtaked old login");
        }
        Client client =
                new Client(
                        this,
                        conn,
                        remote_address,
                        local_remote_address,
                        username,
                        false,
                        revision,
                        host_id);
        online_users.put(username.toLowerCase(), client);
        client_map.put(new Integer(client.getHostID()), client);
        logger.info(username + " logged in, with key " + key_code_encoded);
        if (online_users.size() == 1) {
            System.out.println("A user is online, starting automatic server ticking.");
            server_tick_timeout =
                    100; // Set to 100ms for regular updates so discord messages sync into the chat
            // room
        }
    }

    public final Client getClientFromID(int host_id) {
        return (Client) client_map.get(new Integer(host_id));
    }

    public final void error(AbstractConnectionListener conn_id, IOException e) {
        logger.severe("Server socket failed!");
        throw new RuntimeException(e);
    }

    public final void logoutClient(Client client) {
        online_users.remove(client.getUsername().toLowerCase());
        removeInstance(client.getHostID());
        if (online_users.isEmpty()) {
            logger.info("No users online, pausing automatic server ticking.");
            server_tick_timeout = 0;
        }
    }

    public final void removeInstance(int instance_id) {
        client_map.remove(new Integer(instance_id));
    }

    private static final void postPanic() {
        try {
            DBUtils.postHermesMessage("elias, xar, jacob, thufir: Matchmaking service crashed!");
        } catch (Throwable t) {
            System.out.println("Exception (Throwable): " + t);
            logger.throwing("MatchmakingServer", "postPanic", t);
        }
    }

    public static final void main(String[] args) {
        try {
            TryInitalizeDiscordBot();
            new MatchmakingServer();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            logger.throwing("MatchmakingServer", "main", e);
            postPanic();
            e.printStackTrace();
        }
    }

    private static void TryInitalizeDiscordBot() {
        try {
            String token =
                    ServerConfiguration.getInstance().get(ServerConfiguration.DISCORD_BOT_TOKEN);
            String serverIdAsString =
                    ServerConfiguration.getInstance().get(ServerConfiguration.DISCORD_SERVER_ID);
            if (token == null || token.isEmpty()) {
                logger.info(
                        "No discord bot token found in server config."
                                + " Skipping Discord bot initialization.");

            } else if (serverIdAsString == null || serverIdAsString.isEmpty()) {
                logger.info(
                        "No discord guild name found in server config. Skipping"
                                + " Discord bot initialization.");
            } else {
                try {
                    long serverId = Long.parseLong(serverIdAsString);
                    if (serverId <= 0) {
                        logger.log(
                                Level.INFO,
                                "Invalid discord guild ID (must be positive): {0}. Skipping Discord"
                                        + " bot initialization.",
                                serverIdAsString);
                    } else {
                        DiscordBotService.getInstance().initialize(token, serverId);
                        logger.log(
                                Level.INFO,
                                "Discord bot initialized with token for server id: {0}",
                                serverId);
                    }
                } catch (NumberFormatException e) {
                    logger.log(
                            Level.WARNING,
                            "Invalid discord guild ID format: {0}, skipping Discord bot"
                                    + " initialization. Error: {1}",
                            new Object[] {serverIdAsString, e.getMessage()});
                }
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Failed to initialize Discord bot due to an exception.", e);
        }
    }
}
