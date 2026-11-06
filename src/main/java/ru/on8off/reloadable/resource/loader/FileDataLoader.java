package ru.on8off.reloadable.resource.loader;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import ru.on8off.reloadable.resource.ReloadableData;
import ru.on8off.reloadable.resource.ReloadableException;
import ru.on8off.reloadable.resource.mapper.DataMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Implementation of {@link DataLoader} that loads data from a file by path.
 *
 * <p>Used lastModifiedTime attribute of file to determine if the data has been modified.</p>
 *
 * <p>Paths prefixed with {@code classpath:} are resolved as class-loader
 * resources. Files ending in {@code .zip} or {@code .gz} are decompressed
 * before mapping.</p>
 *
 * @param <T> type of the mapped file content
 */
public class FileDataLoader<T> implements DataLoader<T> {
    protected final String path;
    protected final DataMapper<InputStream, T> dataMapper;
    protected final URL url;
    /**
     * Creates a loader for an existing file.
     *
     * @param path file-system path or {@code classpath:} resource path
     * @param dataMapper converts the file stream into the target type
     * @throws IllegalArgumentException if the path or mapper is {@code null}
     * @throws IllegalStateException if the path does not identify an existing file
     */
    public FileDataLoader(String path, DataMapper<InputStream, T> dataMapper) {
        this.path = Validate.notNull(path, "Param 'path' must not be null");
        this.dataMapper = Validate.notNull(dataMapper, "Param 'dataMapper' must not be null");
        if (path.startsWith("classpath:")) {
            ClassLoader classLoader = getClass().getClassLoader();
            String resourceName = path.replace("classpath:/", "").replace("classpath:", "");
            URL resourceFileUrl = classLoader.getResource(resourceName);
            Validate.validState(resourceFileUrl != null, "File doesn't exist in classpath: path=%s", path);
            this.url = resourceFileUrl;
        } else {
            File file = new File(path);
            Validate.validState(file.exists(), "File doesn't exist: path=%s, absolutePath=%s", path, file.getAbsolutePath());
            Validate.validState(file.isFile(), "Path isn't a file: path=%s, absolutePath=%s", path, file.getAbsolutePath());
            try {
                this.url = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new ReloadableException(e);
            }
        }
    }


    /**
     * Loads the current data from the file.
     *
     * @param lastModified modification time from the previously loaded data,
     *                     or {@code null} when no data has been loaded yet or for force reloading
     * @return loaded data, or {@code null} when the file has not changed
     * @throws ReloadableException if the file cannot be read
     */
    @Override
    public ReloadableData<T> load(LocalDateTime lastModified) {
        String protocol = url.getProtocol();
        try {
            LocalDateTime fileLastModified = null;
            if ("jar".equals(protocol)) {
                fileLastModified = Objects.requireNonNullElseGet(lastModified, LocalDateTime::now);
            } else if ("file".equals(protocol)) {
                File file = new File(url.toURI());
                BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                fileLastModified = localDateTime(fileAttributes.lastModifiedTime().toMillis());

            } else {
                throw new ReloadableException("Unsupported protocol: " + protocol);
            }
            if (lastModified == null || fileLastModified.isAfter(lastModified)) {
                return dataMapper.apply(new ReloadableData<>(fileLastModified, read(url)));
            } else {
                return null;
            }
        } catch (IOException | URISyntaxException e) {
            throw new ReloadableException(e);
        }
    }

    /** Reads and, for ZIP/GZIP files, decompresses the file content.
     *
     * @param url to file to read
     * @return input stream for the file content
     * @throws IOException if the file cannot be read
     */
    protected static InputStream read(URL url) throws IOException {
        String extension = FilenameUtils.getExtension(url.getFile()).toLowerCase();
        byte[] bytes;
        if ("zip".equals(extension)) {
            try (ZipInputStream zis =  new ZipInputStream(url.openStream())) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    throw new ReloadableException("Can't read from file " + url.getFile() + ": zip entry is null");
                }
                if (entry.isDirectory()) {
                    throw new ReloadableException("Can't read from file " + url.getFile() + ": zip entry is directory");
                }
                bytes = IOUtils.toByteArray(zis);
            }
        } else if ("gz".equals(extension)) {
            try (InputStream is = new GZIPInputStream(url.openStream())) {
                bytes = IOUtils.toByteArray(is);
            }
        } else {
            try (InputStream is = url.openStream()) {
                bytes = IOUtils.toByteArray(is);
            }
        }
        return new ByteArrayInputStream(bytes);
    }

    /** Converts an epoch-millisecond timestamp using the system time zone.
     *
     * @param millis epoch-millisecond timestamp
     * @return local date and time
     */
    protected static LocalDateTime localDateTime(long millis) {
        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /** @return path supplied to this loader */
    public String getPath() {
        return path;
    }

    /** @return mapper used to convert file content */
    public DataMapper<InputStream, T> getDataMapper() {
        return dataMapper;
    }

    /** @return resolved url */
    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "FileDataSource{" +
                "path='" + path + '\'' +
                ", dataMapper=" + dataMapper +
                '}';
    }
}
