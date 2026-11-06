package ru.on8off.reloadable.resource.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.on8off.reloadable.resource.ReloadableException;

import java.io.IOException;
import java.io.InputStream;


/**
 * Deserializes an input stream as a JSON value.
 *
 * @param <T> target type
 */
public class JsonObjectDataMapper<T> extends BaseDataMapper<InputStream, T> {
    private final ObjectMapper objectMapper;

    /**
     * Creates a mapper using a new default-configured Jackson object mapper.
     */
    public JsonObjectDataMapper() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a mapper using the provided Jackson object mapper.
     *
     * @param objectMapper Jackson object mapper
     */
    public JsonObjectDataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deserializes JSON into the configured target type.
     *
     * @param dataFrom JSON input stream
     * @return deserialized value
     * @throws ReloadableException if the stream cannot be deserialized
     */
    @Override
    public T map(InputStream dataFrom) {
        try {
             return objectMapper.readValue(dataFrom, new TypeReference<T>(){});
        } catch (IOException e) {
            throw new ReloadableException(e);
        }
    }
}
