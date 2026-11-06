package ru.on8off.reloadable.resource.mapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Reads an input stream into a list of lines.
 */
public class StringListDataMapper extends BaseDataMapper<InputStream, List<String>> {
    private final Charset charset;

    /**
     * Creates a mapper using the platform's default charset.
     */
    public StringListDataMapper() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a mapper using the specified charset.
     *
     * @param charset charset used to decode the stream; must not be {@code null}
     */
    public StringListDataMapper(Charset charset) {
        this.charset = Validate.notNull(charset, "Param 'charset' must not be null");
    }

    /**
     * Reads the input stream and splits it into lines.
     *
     * @param dataFrom input stream to read
     * @return lines in encounter order
     */
    @Override
    public List<String> map(InputStream dataFrom) {
        return IOUtils.readLines(dataFrom, charset);
    }

}
