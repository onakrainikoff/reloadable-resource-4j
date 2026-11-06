package ru.on8off.reloadable.resource;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import ru.on8off.reloadable.resource.loader.DataLoader;
import ru.on8off.reloadable.resource.loader.FileDataLoader;
import ru.on8off.reloadable.resource.mapper.StringDataMapper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReloadableResourceTest {

    @Test
    void test() throws IOException, InterruptedException {
        File file = File.createTempFile("file-", ".txt");
        System.out.println("Created temp file: " + file.getAbsolutePath());
        file.deleteOnExit();
        String path = file.getAbsolutePath();
        FileUtils.writeStringToFile(file, "test1");
        AtomicInteger countNewValues = new AtomicInteger();
        AtomicInteger countExceptions = new AtomicInteger();
        ReloadableListener listener = new ReloadableListener() {
            @Override
            public void onReload(String name, DataLoader<?> dataSource, ReloadableData<?> oldValue, ReloadableData<?> newValue, Exception ex, long totalTimeMs) {
                System.out.printf("onReload: name=%s, dataSource=%s, oldValue=%s, newValue=%s, ex=%s, totalTimeMs=%d%n", name, dataSource, oldValue, newValue, ex, totalTimeMs);
                if (newValue != null) {
                    countNewValues.incrementAndGet();
                }
                if (ex != null) {
                    countExceptions.incrementAndGet();
                }
            }
        };

        ReloadableResource<String> reloadableResource = new ReloadableResource<>("test", new FileDataLoader<>(path, new StringDataMapper()), 50, TimeUnit.MILLISECONDS, listener);
        ReloadableData<String> data = reloadableResource.get();
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals("test1", data.getValue());
        Thread.sleep(100);

        FileUtils.writeStringToFile(file, "test2");
        Thread.sleep(100);
        ReloadableData<String> data2 = reloadableResource.get();
        assertNotNull(data2);
        assertNotNull(data2.getLastModified());
        assertTrue(data2.getLastModified().isAfter(data.getLastModified()));
        assertEquals("test2", data2.getValue());

        file.delete();
        Thread.sleep(100);
        ReloadableData<String> data3 = reloadableResource.get();
        assertNotNull(data3);
        assertSame(data2, data3);
        assertEquals("test2", data3.getValue());

        assertEquals(2, countNewValues.get());
        assertTrue(countExceptions.get() > 0);

//        Thread.sleep(1000);
        reloadableResource.shutdown();

    }

    @Test
    public void testExceptions() throws IOException {
        Throwable exception = assertThrows(NullPointerException.class, () -> new ReloadableResource<String>(null, null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'name' must not be null"));

        exception = assertThrows(NullPointerException.class, () -> new ReloadableResource<String>("test", null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'dataLoader' must not be null"));

        exception = assertThrows(IllegalArgumentException.class, () -> new ReloadableResource<String>("test", l -> null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'period' must be > 0"));

        exception = assertThrows(NullPointerException.class, () -> new ReloadableResource<String>("test", l -> null, 10, null));
        assertTrue(exception.getMessage().contains("Param 'unit' must not be null"));

    }


}