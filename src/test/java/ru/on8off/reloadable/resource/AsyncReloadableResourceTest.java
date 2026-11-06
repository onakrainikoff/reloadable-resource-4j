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

class AsyncReloadableResourceTest {

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

        AsyncReloadableResource<String> asyncReloadableResource = new AsyncReloadableResource<>("test", new FileDataLoader<>(path, new StringDataMapper()), 50, TimeUnit.MILLISECONDS, listener);
        ReloadableData<String> data = asyncReloadableResource.get();
        Thread.sleep(100);
        assertNull(data);
        assertFalse(asyncReloadableResource.isStarted());

        asyncReloadableResource.start();
        data = asyncReloadableResource.get();
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals("test1", data.getValue());

        Thread.sleep(100);
        FileUtils.writeStringToFile(file, "test2");
        Thread.sleep(100);
        ReloadableData<String> data2 = asyncReloadableResource.get();
        assertNotNull(data2);
        assertNotNull(data2.getLastModified());
        assertTrue(data2.getLastModified().isAfter(data.getLastModified()));
        assertEquals("test2", data2.getValue());

        file.delete();
        Thread.sleep(100);
        ReloadableData<String> data3 = asyncReloadableResource.get();
        assertNotNull(data3);
        assertSame(data2, data3);
        assertEquals("test2", data3.getValue());

        assertEquals(2, countNewValues.get());
        assertTrue(countExceptions.get() > 0);

//        Thread.sleep(1000);
        asyncReloadableResource.shutdown();

    }

    @Test
    public void testExceptions() throws IOException {
        Throwable exception = assertThrows(NullPointerException.class, () -> new AsyncReloadableResource<String>(null, null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'name' must not be null"));

        exception = assertThrows(NullPointerException.class, () -> new AsyncReloadableResource<String>("test", null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'dataLoader' must not be null"));

        exception = assertThrows(IllegalArgumentException.class, () -> new AsyncReloadableResource<String>("test", l -> null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'period' must be > 0"));

        exception = assertThrows(NullPointerException.class, () -> new AsyncReloadableResource<String>("test", l -> null, 10, null));
        assertTrue(exception.getMessage().contains("Param 'unit' must not be null"));

    }


}