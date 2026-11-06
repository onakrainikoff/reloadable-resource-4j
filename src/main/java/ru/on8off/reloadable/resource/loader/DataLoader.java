package ru.on8off.reloadable.resource.loader;

import ru.on8off.reloadable.resource.ReloadableData;
import java.time.LocalDateTime;

/**
 * Loads a resource data, optionally using the previous source modification
 * time to avoid reloading unchanged data.
 *
 * @param <T> type of the loaded data
 */
public interface DataLoader<T> {
    /**
     * Loads the current data from the source.
     *
     * @param lastModified modification time from the previously loaded data,
     *                     or {@code null} when no data has been loaded yet or for force reloading
     * @return loaded data, or {@code null} when the source has not changed
     */
    ReloadableData<T> load(LocalDateTime lastModified);
}
