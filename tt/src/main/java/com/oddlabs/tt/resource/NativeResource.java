package com.oddlabs.tt.resource;

import com.oddlabs.tt.util.GLUtils;
import org.jspecify.annotations.NonNull;

import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class with associated native state which is not reclaimed automatically by the garbage collector.
 * @param <R> The native state type
 */
public abstract class NativeResource<R extends NativeResource.NativeState> implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(NativeResource.class.getSimpleName());
    private static final Cleaner cleaner = Cleaner.create();

    // Queue for OpenGL cleanup tasks to be executed on the GL thread
    private static final Queue<@NonNull Runnable> glCleanupTasks = new ConcurrentLinkedQueue<>();

    /**
     * Adds an OpenGL cleanup task to a queue to be processed on the GL thread.
     * This is used to avoid "No OpenGL context found" errors when cleanup is triggered by the Cleaner.
     * @param task The Runnable task to execute on the GL thread for cleanup.
     */
    public static void addGLCleanupTask(@NonNull Runnable task) {
        glCleanupTasks.add(task);
    }

    /**
     * Processes all pending OpenGL cleanup tasks. This method must be called from the thread
     * that has the OpenGL context current (e.g., the main rendering thread).
     */
    public static void processGLCleanupTasks() {
        Runnable task;
        while ((task = glCleanupTasks.poll()) != null) {
            try {
                task.run();
                // Check for errors after each task to isolate issues
                GLUtils.checkGLError("After closing resource " + task.getClass().getSimpleName());
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error during OpenGL cleanup task execution", t);
            }
        }
    }

    /**
     * Holds native state for a resource.
     */
    public static abstract class NativeState implements AutoCloseable, Runnable {
        /**
         * Count of unfinalized native resources
         */
        static final AtomicInteger count = new AtomicInteger(0);

        protected NativeState() {
            count.incrementAndGet();
        }

        /**
         * clean up native resource
         */
        @Override
        public abstract void close();

        @Override
        public final void run() {
            count.decrementAndGet();
            try {
                close();
            } catch (Throwable all) {
                logger.log(Level.WARNING, "Exception thrown in close()", all);
            }
        }
    }

    private final Cleaner.@NonNull Cleanable cleanable;
    protected final @NonNull R state;

    public NativeResource(@NonNull R state) {
        this.state = Objects.requireNonNull(state, "state");
        this.cleanable = cleaner.register(this, () -> NativeResource.addGLCleanupTask(state));
    }

    @Override
    public void close() {
        cleanable.clean(); // execute the cleaning action immediately
    }

    public static int getCount() {
        return NativeState.count.get();
    }
}
