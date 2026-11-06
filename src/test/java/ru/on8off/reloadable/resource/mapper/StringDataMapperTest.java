package ru.on8off.reloadable.resource.mapper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StringDataMapperTest {
    static final String TEST_TEXT = "Test text";

    @Test
    public void testMap() {
        StringDataMapper mapper = new StringDataMapper();
        InputStream from = new ByteArrayInputStream(TEST_TEXT.getBytes(StandardCharsets.UTF_8));
        String to = mapper.map(from);
        assertNotNull(to);
        assertEquals(TEST_TEXT, to);
    }

}