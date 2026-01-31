package com.oddlabs.router;

import com.oddlabs.net.MonotoneTimeManager;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

final class SessionManager {
	private final Map<SessionID, Session> id_to_session = new HashMap<>();
	private final SortedMap<Timeout, RouterClient> timeouts = new TreeMap<>();
	private final MonotoneTimeManager time_manager;
	private final Logger logger;

	private int internal_session_id;

	SessionManager(MonotoneTimeManager time_manager, Logger logger) {
		this.logger = logger;
		this.time_manager = time_manager;
	}

	@NonNull Session get(SessionID session_id, @NonNull SessionInfo session_info, int client_id) {
		Session session = id_to_session.get(session_id);
		if (session == null) {
			session = new Session(logger, session_id, session_info, this);
			id_to_session.put(session_id, session);
			logger.log(Level.INFO, "Creating session: {0}", session);
		} else {
			if (!session.info.equals(session_info) || session.hasClient(client_id) || client_id >= session_info.num_participants)
				throw new RuntimeException("SessionInfo mismatch " + session.info + " != " + session_info + " client_id = " + client_id);
		}
		return session;
	}

	long getNextTimeout() {
		if (timeouts.isEmpty())
			return 0;
		Timeout timeout = timeouts.firstKey();
		long timeout_value = timeout.next_timeout - time_manager.getMillis();
		return Math.max(1, timeout_value);
	}

	void process() {
		long millis = time_manager.getMillis();
		while (!timeouts.isEmpty()) {
			Timeout timeout = timeouts.firstKey();
			RouterClient client = timeouts.get(timeout);
			if (client.getSession().getNumPlayers() > 0) {
				if (!heartbeat(timeout.next_timeout, client, millis))
					return;
			}
			boolean removed = unregister(timeout);
			assert removed;
		}
//		verify();
	}

	private boolean heartbeat(long next_timeout, @NonNull RouterClient client, long millis) {
		if (millis < next_timeout)
			return false;
		//logger.finer(id + ": next_tick " + next_tick);
		final int next_tick = computeNextTick(client, millis);
		client.heartbeat(next_tick);
		return true;
	}

	private int computeNextTick(@NonNull RouterClient client, long millis) {
		return doComputeNextTick(client.getSession(), millis);
	}


	private @NonNull Timeout createTimeout(@NonNull RouterClient client, long millis) {
		return new Timeout(internal_session_id++, client, millis);
	}

	long start(@NonNull Session session) {
		remove(session);
		final long initial_time = time_manager.getMillis();
		session.visit((RouterClient client) -> {
                    Timeout timeout = createTimeout(client, initial_time);
                    client.setTimeout(timeout);
                    timeouts.put(timeout, client);
                });
		logger.log(Level.INFO, "Started session: {0}", session);
		return initial_time;
	}

	private boolean unregister(Timeout timeout) {
		return timeouts.remove(timeout) != null;
	}

	void remove(@NonNull Session session) {
		id_to_session.remove(session.session_id);
		logger.log(Level.INFO, "Removing session: {0}", session);
	}

	int getNextTick(@NonNull Session session) {
		final long millis = time_manager.getMillis();
		session.visit((RouterClient client) -> unregister(client.getTimeout()));
		return doComputeNextTick(session, millis);
	}

	public void startTimeout(@NonNull RouterClient client) {
		unregister(client.getTimeout());
		long millis = time_manager.getMillis();
		Timeout timeout = createTimeout(client, millis);
		client.setTimeout(timeout);
		timeouts.put(timeout, client);
	}

	private static int doComputeNextTick(@NonNull Session session, long millis) {
		return (int)(millis - session.getInitialTime());
	}

	static final class Timeout implements Comparable<Timeout> {
		private final int id;

		private final long next_timeout;

		Timeout(int id, @NonNull RouterClient client, long millis) {
			this.id = id;
			this.next_timeout = millis + client.getSession().info.milliseconds_per_heartbeat;
		}

		@Override
		public int compareTo(@NonNull Timeout other) {
            int diff = (int)(next_timeout - other.next_timeout);
			if (diff != 0)
				return diff;
			else
				return id - other.id;
		}

		@Override
		public boolean equals(Object other) {
			Timeout other_session = (Timeout)other;
			return compareTo(other_session) == 0;
		}

		@Override
		public int hashCode() {
			return (int)(id + next_timeout);
		}

		@Override
		public @NonNull String toString() {
			return "Timout: id = " + id + " timeout " + next_timeout;
		}
	}
}
