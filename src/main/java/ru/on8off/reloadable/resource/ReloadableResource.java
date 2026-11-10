package ru.on8off.reloadable.resource;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import ru.on8off.reloadable.resource.listener.ReloadableListener;
import ru.on8off.reloadable.resource.loader.DataLoader;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Self reloadable resource.
 *
 * <p>First resource data load is performed during construction ReloadableResource.
 * If the data loader returns {@code null} during the first load, the ReloadableResource will be considered empty.
 * If the data loader throws an exception during the first load, the ReloadableResource will be not constructed and the exception will be propagated to the caller.
 * {@link AsyncReloadableResource} allows to perform the first load asynchronously and not block the constructor.
 * </p>
 *
 * <p> {@code dataLoader} may return {@code null} to indicate that the data has not
 * changed; in that case the current value is retained.
 *
 * @param <T> type of the resource data
 */
public class ReloadableResource<T> {
    protected final String name;
    protected final DataLoader<T> dataLoader;
    protected final long period;
    protected final TimeUnit unit;
    protected final ScheduledExecutorService executorService;
    protected final List<ReloadableListener<T>> listeners;
    private final ReentrantLock reloadLock = new ReentrantLock();
    protected volatile ReloadableData<T> data;

    /**
     * Creates ReloadableResource with no listeners and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     */
    public ReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit) {
        this(name, dataLoader, period, unit, Collections.emptyList(), null);
    }

    /**
     * Creates ReloadableResource with the reload listener and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listener listener notified after reload attempts; may be {@code null}
     */
    public ReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, ReloadableListener<T> listener) {
        this(name, dataLoader, period, unit, (listener == null ? Collections.emptyList() : Collections.singletonList(listener)), null);
    }

    /**
     * Creates ReloadableResource with reload listeners and a default ScheduledExecutorService.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listeners listeners notified after reload attempts; may be {@code null}
     */
    public ReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, List<ReloadableListener<T>> listeners) {
        this(name, dataLoader, period, unit, listeners, null);
    }

    /**
     * Creates ReloadableResource with a supplied executor and no listeners.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param executorService executor used for reloads; may be {@code null} to create a default
     */
    public ReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, ScheduledExecutorService executorService) {
        this(name, dataLoader, period, unit, Collections.emptyList(), executorService);
    }

    /**
     * Creates ReloadableResource with a supplied executor and a reload listener.
     *
     * @param name resource name
     * @param dataLoader used to load resource data
     * @param period interval between periodic reloads; must be positive
     * @param unit time unit of {@code period}
     * @param listeners listeners notified after reload attempts; may be {@code null}
     * @param executorService executor used for reloads; may be {@code null} to create a default
     */
    public ReloadableResource(String name, DataLoader<T> dataLoader, long period, TimeUnit unit, List<ReloadableListener<T>> listeners, ScheduledExecutorService executorService) {
        this.name = Validate.notNull(name, "Param 'name' must not be null");
        this.dataLoader = Validate.notNull(dataLoader, "Param 'dataLoader' must not be null");
        Validate.isTrue(period > 0, "Param 'period' must be > 0");
        this.period = period;
        this.unit = Validate.notNull(unit, "Param 'unit' must not be null");
        this.listeners = new CopyOnWriteArrayList<>();
        if (listeners != null && !listeners.isEmpty()) {
            this.listeners.addAll(listeners);
        }
        if (executorService == null) {
            this.executorService = defaultExecutorService();
        } else {
            this.executorService = executorService;
        }
        startup();
    }

    /**
     * Returns the most recently loaded resource data.
     *
     * @return resource data, or {@code null} if data has not been loaded yet
     */
    public ReloadableData<T> getData() {
        return data;
    }

    /**
     * Returns the value of the most recently loaded resource data.
     *
     * @return the value of resource data, or {@code null} if data has not been loaded yet
     */
    public T getDataValue(){
        return data != null ? data.getValue() : null;
    }


    /**
     * The time of the last modification of the resource data
     *
     * @return time or {@code null} if data has not been loaded yet
     */
    public LocalDateTime getDataLastModified() {
        return data != null ? data.getLastModified() : null;
    }

    /** Performs the initial load and schedules subsequent periodic reloads. */
    protected void startup() {
        reload();
        executorService.scheduleAtFixedRate(() -> {
            try {reload();} catch (Exception ignored) {}
        }, period, period, unit);
    }

    /**
     * Attempts to load the resource data.
     *
     * <p>If the data loader returns {@code null}, the currently held value is
     * retained. Notifies registered listeners about the reload attempt, whether it succeeds or fails.
     *
     * @throws ReloadableException if loading fails
     */
    public void reload() {
        Long startedTime = null;
        try {
            reloadLock.lock();
            startedTime = System.currentTimeMillis();
            LocalDateTime lastModified = getDataLastModified();
            ReloadableData<T> oldData = data;
            ReloadableData<T> newData = dataLoader.load(lastModified);
            if (newData != null) {
                data = newData;
            }
            long totalTime = System.currentTimeMillis() - startedTime;
            onReloadNotify(oldData, newData, null,  totalTime);
        } catch (Exception ex) {
            long totalTime = startedTime != null ? System.currentTimeMillis() - startedTime : 0;
            onReloadNotify(data, null, ex,  totalTime);
            throw new ReloadableException(ex);
        } finally {
            reloadLock.unlock();
        }
    }

    /**
     * Shuts down the executor and stops scheduled reloads.
     */
    public void shutdown() {
        if (!executorService.isShutdown()) {
            this.executorService.shutdown();
        }
    }

    /**
     * Notifies registered listeners about a reload attempt.
     *
     * @param oldData value held before the attempt
     * @param newData value returned by the loader
     * @param ex exception raised during the attempt, or {@code null} on success
     * @param totalTimeMs elapsed time in milliseconds
     */
    protected void onReloadNotify(ReloadableData<T> oldData, ReloadableData<T> newData, Exception ex, long totalTimeMs) {
        if (!listeners.isEmpty()) {
            for (ReloadableListener<T> listener : listeners) {
                try {
                    listener.onReload(name, dataLoader, oldData, newData, ex, totalTimeMs);
                } catch (Throwable t) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, t, ()-> "ReloadableResource onReloadNotify error:");
                }
            }
        }
    }

    /** Creates default scheduled executor service
     *
     * @return default executor service
     */
    protected ScheduledExecutorService defaultExecutorService() {
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("ReloadableResource-" + name)
                .daemon(true)
                .priority(Thread.MAX_PRIORITY)
                .build();
        return Executors.newSingleThreadScheduledExecutor(factory);
    }

    /** @return resource name */
    public String getName() {
        return name;
    }

    /** @return data loader*/
    public DataLoader<T> getDataLoader() {
        return dataLoader;
    }

    /** @return configured reload interval */
    public long getPeriod() {
        return period;
    }

    /** @return time unit of the reload interval */
    public TimeUnit getUnit() {
        return unit;
    }

    /** @return executor responsible for reloads */
    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    /** @return thread-safe list of registered reload listeners */
    public List<ReloadableListener<T>> getListeners() {
        return listeners;
    }

    @Override
    public String toString() {
        return "ReloadableResource{" +
                "name='" + name + '\'' +
                ", dataLoader=" + dataLoader +
                ", period=" + period +
                ", unit=" + unit +
                '}';
    }
}
