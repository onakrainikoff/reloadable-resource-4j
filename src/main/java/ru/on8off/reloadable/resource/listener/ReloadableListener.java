package ru.on8off.reloadable.resource.listener;


import ru.on8off.reloadable.resource.ReloadableData;
import ru.on8off.reloadable.resource.loader.DataLoader;

/**
 * Receives notifications after a resource reload attempt.
 */
public interface ReloadableListener<T> {
    /**
     * Called after a reload attempt completes or fails.
     *
     * @param name resource name
     * @param dataLoader loader used for the attempt
     * @param oldData value held before the attempt, if any
     * @param newData value returned by the loader, if any
     * @param ex exception raised during the attempt, or {@code null} on success
     * @param totalTimeMs elapsed time in milliseconds
     */
    void onReload(String name, DataLoader<? extends T> dataLoader, ReloadableData<? extends T> oldData, ReloadableData<? extends T> newData, Exception ex, long totalTimeMs);
}
