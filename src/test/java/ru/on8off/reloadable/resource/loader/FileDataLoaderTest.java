package ru.on8off.reloadable.resource.loader;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import ru.on8off.reloadable.resource.ReloadableData;
import ru.on8off.reloadable.resource.mapper.StringListDataMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileDataLoaderTest {
    static final String TEST_FILE_TXT = "files/file.txt";
    static final String TEST_FILE_TXT_ZIP = "files/file.zip";
    static final String TEST_FILE_TXT_GZ = "files/file.gz";

    @Test
    public void testLoadByClasspath() throws IOException {
        List<String> content = FileUtils.readLines(new File(FileDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()), Charset.defaultCharset());
        String path = "classpath:" + TEST_FILE_TXT;
        StringListDataMapper mapper = new StringListDataMapper();
        FileDataLoader<List<String>> dataLoader = new FileDataLoader<>(path, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());
    }

    @Test
    public void testLoadByAbsolutePath() throws IOException {
        List<String> content = FileUtils.readLines(new File(FileDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()), Charset.defaultCharset());
        File file = createTempFileTxt(content);
        String path = file.getAbsolutePath();

        StringListDataMapper mapper = new StringListDataMapper();
        FileDataLoader<List<String>> dataLoader = new FileDataLoader<>(path, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());
    }

    @Test
    public void testLastModified() throws IOException, InterruptedException {
        List<String> content = FileUtils.readLines(new File(FileDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()), Charset.defaultCharset());
        File file = createTempFileTxt(content);
        String path = file.getAbsolutePath();

        StringListDataMapper mapper = new StringListDataMapper();
        FileDataLoader<List<String>> dataLoader = new FileDataLoader<>(path, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());

        Thread.sleep(100);
        LocalDateTime last = data.getLastModified();
        ReloadableData<List<String>> newdata = dataLoader.load(last);
        assertNull(newdata);

        List<String> newText = Collections.singletonList("Test string number 1");
        FileUtils.writeLines(file, newText);
        newdata = dataLoader.load(last);
        assertNotNull(newdata);
        assertNotNull(newdata.getLastModified());
        assertTrue(data.getLastModified().isBefore(newdata.getLastModified()));
        assertNotNull(newdata.getValue());
        assertNotEquals(content, newdata.getValue());
    }

    @Test
    public void testExceptions() throws IOException {
        Throwable exception = assertThrows(NullPointerException.class, () -> new FileDataLoader<>(null, null));
        assertTrue(exception.getMessage().contains("Param 'path' must not be null"));

        exception = assertThrows(NullPointerException.class, () -> new FileDataLoader<>("test.txt", null));
        assertTrue(exception.getMessage().contains("Param 'dataMapper' must not be null"));

        StringListDataMapper mapper = new StringListDataMapper();
        exception = assertThrows(IllegalStateException.class, () -> new FileDataLoader<>("test.txt", mapper));
        assertTrue(exception.getMessage().contains("File doesn't exist:"));

        exception = assertThrows(IllegalStateException.class, () -> new FileDataLoader<>("classpath:test.txt", mapper));
        assertTrue(exception.getMessage().contains("File doesn't exist in classpath:"));

        String dir = new File("").getAbsolutePath();
        exception = assertThrows(IllegalStateException.class, () -> new FileDataLoader<>(dir, mapper));
        assertTrue(exception.getMessage().contains("Path isn't a file:"));
    }

    @Test
    public void testLoadZip() throws IOException {
        List<String> content = FileUtils.readLines(new File(FileDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()), Charset.defaultCharset());
        String path = "classpath:" + TEST_FILE_TXT_ZIP;

        StringListDataMapper mapper = new StringListDataMapper();
        FileDataLoader<List<String>> dataLoader = new FileDataLoader<>(path, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());
    }

    @Test
    public void testLoadGZip() throws IOException {
        List<String> content = FileUtils.readLines(new File(FileDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()), Charset.defaultCharset());
        String path = "classpath:" + TEST_FILE_TXT_GZ;

        StringListDataMapper mapper = new StringListDataMapper();
        FileDataLoader<List<String>> dataLoader = new FileDataLoader<>(path, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());
    }

    static File createTempFileTxt(List<String> content) throws IOException {
        File file = File.createTempFile("file-", ".txt");
        System.out.println("Created temp file: " + file.getAbsolutePath());
        file.deleteOnExit();
        if (content != null && !content.isEmpty()) {
            FileUtils.writeLines(file, content);
        }
        return file;
    }

}