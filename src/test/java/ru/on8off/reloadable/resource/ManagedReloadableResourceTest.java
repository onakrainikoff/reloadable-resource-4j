package ru.on8off.reloadable.resource;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import ru.on8off.reloadable.resource.listener.ReloadableListener;
import ru.on8off.reloadable.resource.listener.SetNewDataValueTo;
import ru.on8off.reloadable.resource.loader.FileDataLoader;
import ru.on8off.reloadable.resource.mapper.StringDataMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ManagedReloadableResourceTest {

    @Test
    void test() throws IOException, InterruptedException, ExecutionException {
        File file = File.createTempFile("file-", ".txt");
        System.out.println("Created temp file: " + file.getAbsolutePath());
        file.deleteOnExit();
        String path = file.getAbsolutePath();
        FileUtils.writeStringToFile(file, "test1");
        AtomicInteger countNewValues = new AtomicInteger();
        AtomicInteger countExceptions = new AtomicInteger();
        ReloadableListener<Object> onException = (name, dataLoader, oldData, newData, ex, totalTimeMs) -> {
            if (ex != null) {
                countExceptions.incrementAndGet();
            }
        };
        ReloadableListener<String> onNewData = new SetNewDataValueTo<>((data)-> countNewValues.incrementAndGet());

        ManagedReloadableResource<String> managedReloadableResource = new ManagedReloadableResource<>("test", new FileDataLoader<>(path, new StringDataMapper()), 50, TimeUnit.MILLISECONDS, List.of(onNewData, onException));
        ReloadableData<String> data = managedReloadableResource.getData();
        Thread.sleep(100);
        assertNull(data);
        assertFalse(managedReloadableResource.isSchedulingStarted());

        data = managedReloadableResource.load();
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals("test1", data.getValue());
        assertEquals(data.getValue(), managedReloadableResource.getDataValue());
        assertFalse(managedReloadableResource.isSchedulingStarted());

        FileUtils.writeStringToFile(file, "test2");
        ReloadableData<String> data2 = managedReloadableResource.loadAsync().get();
        assertNotNull(data2);
        assertNotNull(data2.getLastModified());
        assertTrue(data2.getLastModified().isAfter(data.getLastModified()));
        assertEquals("test2", data2.getValue());
        assertEquals(data2.getValue(), managedReloadableResource.getDataValue());
        assertFalse(managedReloadableResource.isSchedulingStarted());

        FileUtils.writeStringToFile(file, "test3");
        managedReloadableResource.startScheduling(0);
        Thread.sleep(100);
        ReloadableData<String> data3 = managedReloadableResource.getData();
        assertNotNull(data3);
        assertNotNull(data3.getLastModified());
        assertTrue(data3.getLastModified().isAfter(data2.getLastModified()));
        assertEquals("test3", data3.getValue());
        assertEquals(data3.getValue(), managedReloadableResource.getDataValue());
        assertTrue(managedReloadableResource.isSchedulingStarted());

        file.delete();
        Thread.sleep(100);
        ReloadableData<String> data4 = managedReloadableResource.getData();
        assertNotNull(data4);
        assertSame(data3, data4);
        assertEquals("test3", data4.getValue());

        assertEquals(3, countNewValues.get());
        assertTrue(countExceptions.get() > 0);

////        Thread.sleep(1000);
        managedReloadableResource.shutdown();

    }

    @Test
    public void testExceptions() throws IOException {
        Throwable exception = assertThrows(NullPointerException.class, () -> new ManagedReloadableResource<String>(null, null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'name' must not be null"));

        exception = assertThrows(NullPointerException.class, () -> new ManagedReloadableResource<String>("test", null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'dataLoader' must not be null"));

        exception = assertThrows(IllegalArgumentException.class, () -> new ManagedReloadableResource<String>("test", l -> null, 0, null));
        assertTrue(exception.getMessage().contains("Param 'period' must be > 0"));

        exception = assertThrows(NullPointerException.class, () -> new ManagedReloadableResource<String>("test", l -> null, 10, null));
        assertTrue(exception.getMessage().contains("Param 'unit' must not be null"));


        File file = File.createTempFile("file-", ".txt");
        System.out.println("Created temp file: " + file.getAbsolutePath());
        file.deleteOnExit();
        String path = file.getAbsolutePath();
        FileUtils.writeStringToFile(file, "test1");
        AtomicInteger countExceptions = new AtomicInteger();
        ReloadableListener<Object> onException = (name, dataLoader, oldData, newData, ex, totalTimeMs) -> {
            if (ex != null) {
                countExceptions.incrementAndGet();
            }
        };
        ManagedReloadableResource<String> managedReloadableResource = new ManagedReloadableResource<>("test", new FileDataLoader<>(path, new StringDataMapper()), 50, TimeUnit.MILLISECONDS, List.of(onException));
        file.delete();
        exception = assertThrows(java.util.concurrent.ExecutionException.class, () -> managedReloadableResource.loadAsync().get());
        assertEquals(1, countExceptions.get());
    }


}