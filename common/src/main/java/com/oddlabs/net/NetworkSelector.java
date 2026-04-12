package com.oddlabs.net;

import com.oddlabs.event.Deterministic;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class NetworkSelector {
    private static final long PING_TIMEOUT = 4 * 60 * 1000;
    private static final long PING_DELAY = PING_TIMEOUT / 2;

    private final @NonNull MonotoneTimeManager time_manager;
    private int current_handler_id;
    private final Map<Object, Handler> handler_map = new HashMap<>();
    private TaskThread task_thread;
    private Selector selector;
    private final List<TimedConnection> ping_connections = new LinkedList<>();
    private final List<TimedConnection> ping_timeouts = new LinkedList<>();

    private final Deterministic deterministic;

    public NetworkSelector(final @NonNull Deterministic deterministic) {
        this(deterministic, () -> deterministic.log(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())));
    }

    public NetworkSelector(Deterministic deterministic, @NonNull TimeManager time_manager) {
        this.deterministic = deterministic;
        this.time_manager = new MonotoneTimeManager(time_manager);
    }

    public Deterministic getDeterministic() {
        return deterministic;
    }

    void asyncConnect(String dns_name, int port, Connection conn) {
        try {
            initSelector();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DNSTask task = new DNSTask(dns_name, port, conn);
        getTaskThread().addTask(task);
    }

    public @NonNull TaskThread getTaskThread() {
        if (task_thread == null) {
            task_thread = new TaskThread(deterministic, selector::wakeup);
        }
        return task_thread;
    }

    public void initSelector() throws IOException {
        if (selector == null)
            selector = Selector.open();
    }

    Selector getSelector() {
        try {
            initSelector();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return selector;
    }

    void unregisterForPinging(@NonNull Connection conn) {
        TimedConnection unregister_key = new TimedConnection(-1, conn);
        ping_timeouts.remove(unregister_key);
        ping_connections.remove(unregister_key);
    }

    void registerForPingTimeout(@NonNull Connection conn) {
        long ping_timeout = time_manager.getMillis() + PING_TIMEOUT;
        ping_timeouts.add(new TimedConnection(ping_timeout, conn));
    }

    void registerForPing(@NonNull Connection conn) {
        long ping_time = time_manager.getMillis() + PING_DELAY;
        ping_connections.add(new TimedConnection(ping_time, conn));
    }

    private void processTasks() {
        if (task_thread != null)
            task_thread.poll();
    }

    private long processPings(long millis) {
        long next_select_timeout = PING_DELAY;
        while (!ping_timeouts.isEmpty()) {
            TimedConnection first_conn = ping_timeouts.getFirst();
            long first = first_conn.getTimeout();
            if (first <= millis) {
                ping_timeouts.removeFirst();
                first_conn.getConnection().timeout();
            } else {
                next_select_timeout = first - millis;
                break;
            }
        }
        while (!ping_connections.isEmpty()) {
            TimedConnection first_conn = ping_connections.getFirst();
            long first = first_conn.getTimeout();
            if (first <= millis) {
                ping_connections.removeFirst();
                Connection conn = first_conn.getConnection();
                if (conn.isConnected()) {
                    conn.doPing();
                    registerForPing(conn);
                }
            } else {
                next_select_timeout = Math.min(first - millis, next_select_timeout);
                break;
            }
        }
        return next_select_timeout;
    }

    public void tickBlocking(long timeout) throws IOException {
        processTasks();
        long millis = time_manager.getMillis();
        long next_timeout;
        long ping_timeout = processPings(millis);
        if (ping_timeout == 0)
            next_timeout = timeout;
        else if (timeout == 0)
            next_timeout = ping_timeout;
        else
            next_timeout = Math.min(ping_timeout, timeout);
        if (deterministic.log(selector != null && selector.select(next_timeout) > 0))
            doTick();
    }

    public void tickBlocking() throws IOException {
        tickBlocking(0);
    }

    public void tick() {
        try {
            processTasks();
            processPings(time_manager.getMillis());
            if (deterministic.log(selector != null && selector.selectNow() > 0))
                doTick();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @NonNull MonotoneTimeManager getTimeManager() {
        return time_manager;
    }

    void cancelKey(@NonNull SelectionKey key, Handler handler) {
        Object handler_key = null;
        if (!deterministic.isPlayback()) {
            handler_key = key.attachment();
            key.cancel();
        }
        handler_key = deterministic.log(handler_key);
        handler_map.remove(handler_key);
    }

    void attachToKey(@NonNull SelectionKey key, Handler handler) {
        Object handler_key = null;
        if (!deterministic.isPlayback()) {
            handler_key = current_handler_id++;
            key.attach(handler_key);
        }
        handler_key = deterministic.log(handler_key);
        handler_map.put(handler_key, handler);
    }

    private void doTick() throws IOException {
        Iterator<SelectionKey> selected_keys = null;
        if (!deterministic.isPlayback())
            selected_keys = selector.selectedKeys().iterator();
        while (deterministic.log(deterministic.isPlayback() || selected_keys.hasNext())) {
            SelectionKey key;
            if (!deterministic.isPlayback()) {
                key = selected_keys.next();
                selected_keys.remove();
            } else
                key = null;
            if (deterministic.log(deterministic.isPlayback() || !key.isValid()))
                continue;
            Object handler_key = null;
            if (!deterministic.isPlayback())
                handler_key = key.attachment();
            handler_key = deterministic.log(handler_key);
            Handler handler = handler_map.get(handler_key);
            try {
                handler.handle();
            } catch (Exception e) {
                handler.handleError(e instanceof IOException ioe ? ioe : new IOException("Unexpected error", e));
                cancelKey(key, handler);
            }
        }
    }
}
