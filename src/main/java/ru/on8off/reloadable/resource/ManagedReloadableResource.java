/*
 * SPDX-FileCopyrightText: 2026 onakrainikoff
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.on8off.reloadable.resource;

import ru.on8off.reloadable.resource.listener.ReloadableListener;
import ru.on8off.reloadable.resource.loader.DataLoader;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Self reloadable resource.
 *
 * <p>Unlike {@link ReloadableResource}, construction does not load the resource data first time and
 * does not start to periodically reload it. This allows to create reloadable resources without blocking the constructor,
 * but requires an explicit call to {@link #load()} or {@link #loadAsync()} to perform the first load and call to {@link #startScheduling(long)} to start scheduled reloads. </p>
 *
 * @param <T> type of the resource data
 */
public class ManagedReloadableResource<T> extends ReloadableResource<T> {
    private volatile boolean schedulingStarted = false;

    /**
     * Creates ManagedReloadableResource with no listeners and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     */
    public ManagedReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit) {
        super(name, dataLoader, period, unit);
    }

    /**
     * Creates ManagedReloadableResource with the reload listener and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listener listener notified after reload attempts; may be {@code null}
     */
    public ManagedReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, ReloadableListener<? super T> listener) {
        super(name, dataLoader, period, unit, listener);
    }

    /**
     * Creates ManagedReloadableResource with reload listeners and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listeners listeners notified after reload attempts; may be {@code null}
     */
    public ManagedReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, List<ReloadableListener<? super T>> listeners) {
        super(name, dataLoader, period, unit, listeners);
    }

    /**
     * Creates ManagedReloadableResource with a supplied executor and no listeners.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param executorService executor used for reloads; may be {@code null} to create a default
     */
    public ManagedReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, ScheduledExecutorService executorService) {
        super(name, dataLoader, period, unit, executorService);
    }

    /**
     * Creates ManagedReloadableResource with a supplied executor and a reload listener.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listeners listeners notified after reload attempts; may be {@code null}
     * @param executorService executor used for reloads; may be {@code null} to create a default
     */
    public ManagedReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, List<ReloadableListener<? super T>> listeners, ScheduledExecutorService executorService) {
        super(name, dataLoader, period, unit, listeners, executorService);
    }


    /** Leaves startup to an explicit call to {@link #load()} ()} or {@link #loadAsync()} ()} and {@link #startScheduling(long)}. */
    @Override
    protected void startup() {}

    /**
     * Returns whether scheduled reloads have been started.
     *
     * @return {@code true} after a successful call to either start method
     */
    public boolean isSchedulingStarted() {
        return schedulingStarted;
    }


    /**
     * Load the resource data.
     * This method does not start scheduled reloads. See {@link  #startScheduling(long)}
     * @throws ReloadableException if loading fails
     * @return resource data
     */
    public ReloadableData<T> load() {
        reload();
        return getData();
    }

    /**
     * Load the resource data asynchronously.
     * This method does not start scheduled reloads. See {@link  #startScheduling(long)}
     *
     * @throws java.util.concurrent.ExecutionException if loading fails
     * @return resource data
     */
    public Future<ReloadableData<T>> loadAsync() {
        return this.executorService.submit(() -> {
            reload();
            return getData();
        });
    }


    /**
     * Start scheduled reloads
     * Does nothing if already started or the executor has been shut down.
     */
    public synchronized void startScheduling(long initialDelay) {
        if (!schedulingStarted && !executorService.isShutdown()) {
            this.executorService.scheduleAtFixedRate(() -> {
                try {reload();} catch (Exception ignored) {}
            }, initialDelay, period, unit);
            this.schedulingStarted = true;
        }
    }

}
