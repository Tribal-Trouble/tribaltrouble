package com.oddlabs.routerserver;

import com.oddlabs.event.Deterministic;
import com.oddlabs.event.NotDeterministic;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.Router;
import com.oddlabs.util.DBUtils;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class RouterServer {
	private final static Logger logger;
	
	static {
		logger = Logger.getLogger("com.oddlabs.router.Router");
		try {
			Handler fh = new FileHandler("logs/router.%g.log", 10*1024*1024, 50);
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.ALL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void run() throws Exception {
		final Deterministic deterministic;
		deterministic = new NotDeterministic();
/*		File log_file = new File("event.log");
		if (log_file.exists())
			deterministic = new LoadDeterministic(log_file, false);
		else
			deterministic = new SaveDeterministic(log_file);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public final void run() {
				deterministic.endLog();
			}
		});*/
		try {
			NetworkSelector network = new NetworkSelector(deterministic);
			Router router = new Router(network, logger);
			logger.info("Router started.");
			while (true) {
				long timeout = router.getNextTimeout();
//logger.finer("timeout: " + timeout);
				network.tickBlocking(timeout);
				router.process();
				deterministic.checkpoint();
			}
		} finally {
			deterministic.endLog();
		}
	}

	private final static void postPanic() {
		try {
			DBUtils.initConnection("jdbc:mysql://localhost/oddlabs", "matchmaker", "U46TawOp");
			DBUtils.postHermesMessage("elias, xar, jacob, thufir: Router crashed!");
		} catch (Throwable t) {
			logger.throwing("Router", "postPanic", t);
		}
	}

	public final static void main(String[] args) throws Exception {
		try {
			run();
		} catch (Throwable t) {
			logger.throwing("Router", "main", t);
			postPanic();
			System.exit(1);
		}
	}
}
