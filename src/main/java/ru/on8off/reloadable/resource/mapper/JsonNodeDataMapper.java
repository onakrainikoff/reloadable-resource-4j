package ru.on8off.reloadable.resource.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.on8off.reloadable.resource.ReloadableException;

import java.io.IOException;
import java.io.InputStream;


/**
 * Parses an input stream as a Jackson JSON tree.
 */
public class JsonNodeDataMapper extends BaseDataMapper<InputStream, JsonNode> {
    private final ObjectMapper objectMapper;

    /**
     * Parses JSON using a new default-configured Jackson object mapper.
     */
    public JsonNodeDataMapper() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses JSON using the provided Jackson object mapper.
     *
     * @param objectMapper Jackson object mapper
     */
    public JsonNodeDataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Reads a JSON tree from the stream.
     *
     * @param dataFrom JSON input stream
     * @return parsed JSON tree
     * @throws ReloadableException if the stream does not contain readable JSON
     */
    @Override
    public JsonNode map(InputStream dataFrom) {
        try {
             return objectMapper.readTree(dataFrom);
        } catch (IOException e) {
            throw new ReloadableException(e);
        }
    }
}
