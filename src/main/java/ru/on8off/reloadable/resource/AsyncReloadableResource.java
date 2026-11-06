package ru.on8off.reloadable.resource;

import ru.on8off.reloadable.resource.loader.DataLoader;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Self reloadable resource.
 *
 * <p>Unlike {@link ReloadableResource}, construction does not load the resource data first time and
 * does not start to periodically reload it. This allows to create reloadable resources without blocking the constructor,
 * but requires an explicit call to {@link #start()} or {@link #startAsync()} to perform the first load and schedule periodic reloads. </p>
 *
 * @param <T> type of the resource data
 */
public class AsyncReloadableResource<T> extends ReloadableResource<T> {
    private volatile boolean started = false;

    /**
     * Creates AsyncReloadableResource with no listeners and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     */
    public AsyncReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit) {
        super(name, dataLoader, period, unit);
    }

    /**
     * Creates AsyncReloadableResource with the reload listener and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listener listener notified after reload attempts; may be {@code null}
     */
    public AsyncReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, ReloadableListener listener) {
        super(name, dataLoader, period, unit, listener);
    }

    /**
     * Creates AsyncReloadableResource with reload listeners and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listeners listeners notified after reload attempts; may be {@code null}
     */
    public AsyncReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, List<ReloadableListener> listeners) {
        super(name, dataLoader, period, unit, listeners);
    }

    /**
     * Creates AsyncReloadableResource with a supplied executor and no listeners.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param executorService executor used for reloads; may be {@code null} to create a default
     */
    public AsyncReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, ScheduledExecutorService executorService) {
        super(name, dataLoader, period, unit, executorService);
    }

    /**
     * Creates AsyncReloadableResource with a supplied executor and a reload listener.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listeners listeners notified after reload attempts; may be {@code null}
     * @param executorService executor used for reloads; may be {@code null} to create a default
     */
    public AsyncReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, List<ReloadableListener> listeners, ScheduledExecutorService executorService) {
        super(name, dataLoader, period, unit, listeners, executorService);
    }


    /** Leaves startup to an explicit call to {@link #start()} or {@link #startAsync()}. */
    protected void startup() {}

    /**
     * Returns whether periodic reloads have been started.
     *
     * @return {@code true} after a successful call to either start method
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Do first load in current thread and waits for the first load to complete before returning.
     * If the first load fails, the exception is propagated to the caller.
     * Does nothing if already started or the executor has been shut down.
     */
    public synchronized void start() {
        if (!started && !executorService.isShutdown()) {
            reload();
            this.executorService.scheduleAtFixedRate(() -> {
                try {reload();} catch (Exception ignored) {}
            }, period, period, unit);
            this.started = true;
        }
    }

    /**
     * Do first load asynchronously and does not wait for the first load to complete before returning.
     * Does nothing if already started or the executor has been shut down.
     */
    public synchronized void startAsync() {
        if (!started && !executorService.isShutdown()) {
            this.executorService.scheduleAtFixedRate(() -> {
                try {reload();} catch (Exception ignored) {}
            }, 0, period, unit);
            this.started = true;
        }
    }

}
