package com.oddlabs.tt.audio;

import com.oddlabs.tt.Main;
import org.jspecify.annotations.NonNull;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

public final class RefillerList implements AutoCloseable {

    private static final long THREAD_SLEEP_MILLIS = TimeUnit.MILLISECONDS.toMillis(50);

    private volatile boolean finished = false;
    private final Thread refill_thread = new Refiller();
    private final Set<@NonNull QueuedAudioPlayer> players = new CopyOnWriteArraySet<>();

    public RefillerList() {
        refill_thread.start();
    }

    @Override
    public void close() {
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


    synchronized void registerQueuedPlayer(@NonNull QueuedAudioPlayer q) {
        assert !players.contains(q);
        players.add(q);
        notify();
    }

    synchronized void removeQueuedPlayer(@NonNull QueuedAudioPlayer q) {
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
                        if (ALC10.alcGetCurrentContext() != MemoryUtil.NULL) {
                            for (QueuedAudioPlayer player: players) try {
                                player.refill();
                            } catch (IOException _) {
                                System.err.println("Refill failed for " + player);
                            }
                        }
                        while (players.isEmpty() && !finished) try {
                            RefillerList.this.wait();
                        } catch (InterruptedException _) {
                            Thread.interrupted();
                        }
                    }
                    try {
                        //noinspection BusyWait
                        Thread.sleep(THREAD_SLEEP_MILLIS);
                    } catch (InterruptedException _) {
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
