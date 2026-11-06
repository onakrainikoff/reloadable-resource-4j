package ru.on8off.reloadable.resource.mapper;

import ru.on8off.reloadable.resource.ReloadableData;

/**
 * Base mapper that converts the data value in a {@link ReloadableData} while
 * preserving its modification time.
 *
 * @param <F> source data type
 * @param <T> mapped data type
 */
public abstract class BaseDataMapper<F, T> implements DataMapper<F, T> {

    /**
     * Maps the source data and carries its modification time into the result.
     *
     * @param reloadableDataFrom source data, or {@code null}
     * @return mapped data, or {@code null} when the input is {@code null}
     */
    @Override
    public ReloadableData<T> apply(ReloadableData<F> reloadableDataFrom) {
        ReloadableData<T> reloadableDataTo = null;
        if (reloadableDataFrom != null) {
            reloadableDataTo = new ReloadableData<>(reloadableDataFrom.getLastModified(), map(reloadableDataFrom.getValue()));
        }
        return reloadableDataTo;
    }

    /**
     * Converts a source data value to the target type.
     *
     * @param dataFrom source data value
     * @return converted value
     */
    public abstract T map(F dataFrom);
}
