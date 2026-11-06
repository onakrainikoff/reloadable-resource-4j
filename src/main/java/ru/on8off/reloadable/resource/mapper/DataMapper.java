package ru.on8off.reloadable.resource.mapper;

import ru.on8off.reloadable.resource.ReloadableData;

import java.util.function.Function;

/**
 * Converts a {@link ReloadableData} of one type to a {@link ReloadableData} of another type.
 *
 * @param <F> source data type
 * @param <T> mapped data type
 */
public interface DataMapper<F, T> extends Function<ReloadableData<F>, ReloadableData<T>> {

    default <A> DataMapper<F, A> andThen(DataMapper<T, A> after) {
        return (ReloadableData<F> data) -> {
            ReloadableData<T> mappedData = this.apply(data);
            return after.apply(mappedData);
        };
    }

}
