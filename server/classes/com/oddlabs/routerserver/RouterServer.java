package com.oddlabs.routerserver;

import com.oddlabs.event.*;
import com.oddlabs.matchserver.ServerConfiguration;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.Router;
import com.oddlabs.util.DBUtils;

import java.util.logging.*;

public final strictfp class RouterServer {
    private static final Logger logger;

    static {
        logger = Logger.getLogger("com.oddlabs.router.Router");
        try {
            Handler fh = new FileHandler("logs/router.%g.log", 10 * 1024 * 1024, 50);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
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
                // logger.finer("timeout: " + timeout);
                network.tickBlocking(timeout);
                router.process();
                deterministic.checkpoint();
            }
        } finally {
            deterministic.endLog();
        }
    }

    private static final void postPanic() {
        try {
            String password = ServerConfiguration.getInstance().get(ServerConfiguration.SQL_PASS);
            DBUtils.initConnection("jdbc:mysql://localhost/oddlabs", "matchmaker", password);
            DBUtils.postHermesMessage("elias, xar, jacob, thufir: Router crashed!");
        } catch (Throwable t) {
            System.out.println("Exception (Throwable): " + t);
            logger.throwing("Router", "postPanic", t);
        }
    }

    public static final void main(String[] args) throws Exception {
        try {
            run();
        } catch (Throwable t) {
            System.out.println("Exception (Throwable): " + t);
            logger.throwing("Router", "main", t);
            postPanic();
            System.exit(1);
        }
    }
}
