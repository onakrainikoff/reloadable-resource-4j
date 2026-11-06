package ru.on8off.reloadable.resource.mapper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonObjectDataMapperTest {

    @Test
    void testMapObject() throws IOException {
        String json = "{\"test1\": 1, \"test2\": 2, \"test3\": 3}";
        InputStream from = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonObjectDataMapper<Map<String, Integer>> mapper = new JsonObjectDataMapper<>();
        Map<String, Integer> data = mapper.map(from);
        assertNotNull(data);
        assertEquals(3, data.size());
        assertEquals(3, data.get("test3"));
    }

}