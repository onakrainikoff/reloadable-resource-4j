package ru.on8off.reloadable.resource;


import ru.on8off.reloadable.resource.loader.DataLoader;

/**
 * Receives notifications after a resource reload attempt.
 */
public interface ReloadableListener {
    /**
     * Called after a reload attempt completes or fails.
     *
     * @param name resource name
     * @param dataLoader loader used for the attempt
     * @param oldValue value held before the attempt, if any
     * @param newValue value returned by the loader, if any
     * @param ex exception raised during the attempt, or {@code null} on success
     * @param totalTimeMs elapsed time in milliseconds
     */
    void onReload(String name, DataLoader<?> dataLoader, ReloadableData<?> oldValue, ReloadableData<?> newValue, Exception ex, long totalTimeMs);
}
