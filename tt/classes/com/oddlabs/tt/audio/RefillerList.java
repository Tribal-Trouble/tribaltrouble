package com.oddlabs.tt.audio;

import com.oddlabs.tt.Main;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.openal.AL;

public final class RefillerList {

    private final static int THREAD_SLEEP_MILLIS = 50;

    private boolean finished = false;
    private final Thread refill_thread;
    private final List<QueuedAudioPlayer> players = new ArrayList<>();

    public RefillerList() {
        refill_thread = new Refiller();
        refill_thread.start();
    }

    public void destroy() {
        synchronized (this) {
            finished = true;
            players.clear();
            refill_thread.interrupt();
        }
        try {
            refill_thread.join();
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException(e);
        }
    }


    synchronized void registerQueuedPlayer(QueuedAudioPlayer q) {
        assert !players.contains(q);
        players.add(q);
        notify();
    }

    synchronized void removeQueuedPlayer(QueuedAudioPlayer q) {
        players.remove(q);
        assert !players.contains(q);
    }

    private class Refiller extends Thread {

        public Refiller() {
            super("Refiller");
            setDaemon(true);
        }

        @Override
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        public void run() {
            try {
                while (!finished) {
                    synchronized (RefillerList.this) {
                        if (AL.isCreated()) {
                            for (QueuedAudioPlayer player: players) try {
                                player.refill();
                            } catch (IOException ioe) {
                                System.err.println("Refill failed for " + player);
                            }
                        }
                        while (players.isEmpty() && !finished) try {
                            RefillerList.this.wait();
                        } catch (InterruptedException e) {
                            Thread.interrupted();
                        }
                    }
                    try {
                        Thread.sleep(THREAD_SLEEP_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        // ignore
                    }
                }
            } catch (Throwable t) {
                Main.fail(t);
            }
        }
    }
}
