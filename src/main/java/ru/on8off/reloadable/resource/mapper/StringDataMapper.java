package ru.on8off.reloadable.resource.mapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import ru.on8off.reloadable.resource.ReloadableException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Reads an entire input stream into a string.
 */
public class StringDataMapper extends BaseDataMapper<InputStream, String> {
    private final Charset charset;

    /**
     * Creates a mapper using the platform's default charset.
     */
    public StringDataMapper() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a mapper using the specified charset.
     *
     * @param charset charset used to decode the stream; must not be {@code null}
     */
    public StringDataMapper(Charset charset) {
        this.charset = Validate.notNull(charset, "Param 'charset' must not be null");
    }

    /**
     * Reads the entire stream into a string.
     *
     * @param dataFrom input stream to read
     * @return read string
     * @throws ReloadableException if the stream cannot be read
     */
    @Override
    public String map(InputStream dataFrom) {
        try {
            return IOUtils.toString(dataFrom, charset);
        } catch (IOException e) {
            throw new ReloadableException(e);
        }
    }
}
