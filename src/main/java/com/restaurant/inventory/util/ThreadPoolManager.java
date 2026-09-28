package com.restaurant.inventory.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency: Thread Pools and Multi-threading Utility.
 * Provides a managed, bounded thread pool for background networking,
 * database transactions, and image downloading without freezing the JavaFX Application Thread.
 */
public final class ThreadPoolManager {

    private static final int THREAD_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());

    private static final ThreadFactory DAEMON_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Restaurant-Worker-" + count.getAndIncrement());
            t.setDaemon(true); // Don't prevent JVM exit
            return t;
        }
    };

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            THREAD_POOL_SIZE,
            DAEMON_THREAD_FACTORY
    );

    private ThreadPoolManager() {}

    /**
     * Executes a runnable task asynchronously using the thread pool.
     */
    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }

    /**
     * Returns the underlying thread pool executor.
     */
    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    /**
     * Gracefully shuts down the thread pool on application exit.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}
