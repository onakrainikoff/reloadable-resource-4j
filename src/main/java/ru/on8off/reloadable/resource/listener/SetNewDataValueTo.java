package ru.on8off.reloadable.resource.listener;

import org.apache.commons.lang3.Validate;
import ru.on8off.reloadable.resource.ReloadableData;
import ru.on8off.reloadable.resource.loader.DataLoader;

import java.util.function.Consumer;

public class SetNewDataValueTo<T> implements ReloadableListener<T> {
    private final Consumer<T> consumer;

    public SetNewDataValueTo(Consumer<T> consumer) {
        this.consumer = Validate.notNull(consumer, "Param 'consumer' must not be null");
    }

    @Override
    public void onReload(String name, DataLoader<T> dataLoader, ReloadableData<T> oldData, ReloadableData<T> newData, Exception ex, long totalTimeMs) {
        if (newData != null) {
            consumer.accept(newData.getValue());
        }
    }
}
