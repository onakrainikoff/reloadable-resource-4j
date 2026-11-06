package ru.on8off.reloadable.resource.mapper;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import ru.on8off.reloadable.resource.ReloadableData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputStreamDataMapperTest {

    @Test
    void apply() throws IOException {
        String testString = "Test string for InputStreamDataMapper";
        byte[] bytes = testString.getBytes();
        InputStream is = new ByteArrayInputStream(bytes);

        ReloadableData<InputStream> data = new ReloadableData<>(LocalDateTime.now(), is);
        InputStreamDataMapper mapper = new InputStreamDataMapper();
        ReloadableData<InputStream> result = mapper.apply(data);
        assertEquals(data , result);
        assertEquals(is , result.getValue());
        assertEquals(testString, IOUtils.toString(result.getValue()));

    }
}