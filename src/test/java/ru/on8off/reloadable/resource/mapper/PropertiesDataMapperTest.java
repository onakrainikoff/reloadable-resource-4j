package ru.on8off.reloadable.resource.mapper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PropertiesDataMapperTest {

    @Test
    void map() {
        String properties = "properties.property1=1\nproperties.property2=2";
        InputStream from = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
        PropertiesDataMapper mapper = new PropertiesDataMapper();
        Properties data = mapper.map(from);
        assertNotNull(data);
        assertEquals(2, data.size());
        assertEquals("2", data.getProperty("properties.property2"));
    }
}