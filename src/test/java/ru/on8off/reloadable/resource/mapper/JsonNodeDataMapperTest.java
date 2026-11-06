package ru.on8off.reloadable.resource.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonNodeDataMapperTest {

    @Test
    void testMapObject() throws IOException {
        String json = "{\"test1\": 1, \"test2\": 2, \"test3\": 3}";
        InputStream from = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonNodeDataMapper mapper = new JsonNodeDataMapper();
        JsonNode data = mapper.map(from);
        assertNotNull(data);
        assertEquals(3, data.size());
        assertEquals(3, data.get("test3").asInt());
    }
}