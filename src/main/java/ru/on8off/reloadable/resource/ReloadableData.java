package ru.on8off.reloadable.resource;

import java.time.LocalDateTime;
import java.util.Objects;


/**
 * A resource data together with the time it was last modified.
 *
 * @param <T> type of the resource data
 */
public class ReloadableData<T>{
    private final LocalDateTime lastModified;
    private final T value;

    public ReloadableData(LocalDateTime lastModified, T value) {
        this.lastModified = lastModified;
        this.value = value;
    }

    /**
     * Returns the time of the last modification of the resource data.
     *
     * @return modification time, or {@code null} if not set
     */
    public LocalDateTime getLastModified() {
        return lastModified;
    }

    /**
     * Returns the value of the loaded data.
     *
     * @return loaded data, or {@code null} if not set
     */
    public T getValue() {
        return value;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ReloadableData<?> that = (ReloadableData<?>) o;
        return Objects.equals(lastModified, that.lastModified) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastModified, value);
    }

    @Override
    public String toString() {
        return "ReloadableData{" +
                "lastModified=" + lastModified +
                ", value=" + value +
                '}';
    }
}
