package ru.on8off.reloadable.resource.mapper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StringListDataMapperTest {
    static final String TEST_TEXT = "Test text1\nTest text2";

    @Test
    public void testMap() {
        StringListDataMapper mapper = new StringListDataMapper();
        InputStream from = new ByteArrayInputStream(TEST_TEXT.getBytes(StandardCharsets.UTF_8));
        List<String> to = mapper.map(from);
        assertNotNull(to);
        assertEquals(2, to.size());
    }
}