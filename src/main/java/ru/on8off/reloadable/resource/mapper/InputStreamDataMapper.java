package ru.on8off.reloadable.resource.mapper;

import ru.on8off.reloadable.resource.ReloadableData;

import java.io.InputStream;

/**
 * Identity mapper for an already-wrapped input stream value.
 */
public class InputStreamDataMapper implements DataMapper<InputStream, InputStream> {

    /**
     * Returns the supplied wrapper unchanged.
     *
     * @param dataFrom wrapped input stream data
     * @return the same wrapper
     */
    @Override
    public ReloadableData<InputStream> apply(ReloadableData<InputStream> dataFrom) {
        return dataFrom;
    }
}
