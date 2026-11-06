package ru.on8off.reloadable.resource.mapper;

import ru.on8off.reloadable.resource.ReloadableException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads a Java properties from an input stream.
 */
public class PropertiesDataMapper extends BaseDataMapper<InputStream, Properties> {

    /**
     * Loads all properties from the supplied stream.
     *
     * @param dataFrom properties input stream
     * @return loaded properties
     * @throws ReloadableException if the stream cannot be read
     */
    @Override
    public Properties map(InputStream dataFrom) {
        try {
            Properties properties = new Properties();
            properties.load(dataFrom);
            return properties;
        } catch (IOException e) {
            throw new ReloadableException(e);
        }
    }
}
