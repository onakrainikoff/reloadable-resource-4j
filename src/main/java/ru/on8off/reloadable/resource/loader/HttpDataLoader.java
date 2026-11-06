package ru.on8off.reloadable.resource.loader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.Timeout;
import ru.on8off.reloadable.resource.ReloadableData;
import ru.on8off.reloadable.resource.ReloadableException;
import ru.on8off.reloadable.resource.mapper.DataMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Implementation of {@link DataLoader} that loads data from the HTTP resource.
 *
 * <p>Loads and maps an HTTP resource using GET and conditional
 * {@code If-Modified-Since} requests.</p>
 *
 * <p>A {@code 304 Not Modified} response produces no new value, while a
 * {@code 200 OK} response is mapped. ZIP and GZIP entities are decompressed
 * before mapping.</p>
 *
 * @param <T> type of the mapped response body
 */
public class HttpDataLoader<T> implements DataLoader<T> {
    protected final String url;
    protected final DataMapper<InputStream, T> dataMapper;
    protected final Map<String, String> headers;
    protected final CloseableHttpClient httpClient;

    /**
     * Creates a loader without additional headers and with the default http client,
     * which is created and closed for each request.
     *
     * @param url HTTP URL to request
     * @param dataMapper converts the response stream into the target type
     */
    public HttpDataLoader(String url, DataMapper<InputStream, T> dataMapper) {
        this(url, dataMapper, null, null);
    }

    /**
     * Creates a loader with additional request headers and with the default http client,
     * which is created and closed for each request.
     *
     * @param url HTTP URL to request
     * @param dataMapper converts the response stream into the target type
     * @param headers additional request headers; may be {@code null}
     */
    public HttpDataLoader(String url, DataMapper<InputStream, T> dataMapper, Map<String, String> headers) {
        this(url, dataMapper, headers, null);
    }

    /**
     * Creates a loader with additional request headers and with the default http client,
     * which is created and closed for each request.
     *
     * @param url HTTP URL to request
     * @param dataMapper converts the response stream into the target type
     * @param httpClient reusable HTTP client; may be {@code null}, in this case the default http client is used,
     *                     which is created and closed for each request.
     */
    public HttpDataLoader(String url, DataMapper<InputStream, T> dataMapper, CloseableHttpClient httpClient) {
        this(url, dataMapper, null, httpClient);
    }


    /**
     * Creates a loader with optional request headers and an optional http client.
     *
     * @param url HTTP URL to request
     * @param dataMapper converts the response stream into the target type
     * @param headers additional request headers; may be {@code null}
     * @param httpClient reusable HTTP client; may be {@code null}, in this case the default http client is used,
     *                  which is created and closed for each request.
     */
    public HttpDataLoader(String url, DataMapper<InputStream, T> dataMapper, Map<String, String> headers, CloseableHttpClient httpClient) {
        this.url = Validate.notNull(url, "Param 'url' must not be null");
        this.dataMapper = Validate.notNull(dataMapper, "Param 'dataMapper' must not be null");
        this.headers = headers;
        this.httpClient = httpClient;
    }


    /**
     * Loads the current data from the HTTP resource.
     *
     * @param lastModified modification time from the previously loaded data,
     *                     or {@code null} when no data has been loaded yet or for force reloading
     * @return mapped data, or {@code null} when the server returns
     *         {@code 304 Not Modified}
     * @throws ReloadableException if the request fails
     */
    @Override
    public ReloadableData<T> load(LocalDateTime lastModified) {
        try {
            HttpGet request = new HttpGet(url);
            if (lastModified != null) {
                String previousLastModified = DateUtils.formatStandardDate(lastModified.atZone(ZoneId.systemDefault()).toInstant());
                request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, previousLastModified);
            }
            if (headers != null) {
                headers.forEach(request::addHeader);
            }

            if (this.httpClient != null ) {
                return dataMapper.apply(doGetRequest(httpClient, request));
            } else {
                try (CloseableHttpClient client = createOneTimeHttpClient()) {
                    return dataMapper.apply(doGetRequest(client, request));
                }
            }
        } catch (IOException e) {
            throw new ReloadableException(e);
        }
    }

    /** Executes the request and wraps a successful response body with its modification time.
     *
     * @param client HTTP client to use
     * @param request HTTP request to execute
     * @return reloadable data with the response body and modification time
     * @throws IOException if the request fails
     */
    protected ReloadableData<InputStream> doGetRequest(CloseableHttpClient client, HttpGet request) throws IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getCode();
            if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                return null;
            } else if (statusCode == HttpStatus.SC_OK) {
                LocalDateTime modified = LocalDateTime.now();
                Header lastModifiedHeader = response.getFirstHeader(HttpHeaders.LAST_MODIFIED);
                if (lastModifiedHeader != null) {
                    try {
                        modified = LocalDateTime.ofInstant(DateUtils.parseStandardDate(lastModifiedHeader.getValue()), ZoneId.systemDefault());
                    } catch (Exception ignored) {}
                }
                return new ReloadableData<>(modified, read(response));
            } else {
                throw new IllegalStateException(String.format("For GET request at url=%s the statusCode=%d", url, statusCode));
            }
        }
    }

    /** Reads and decompresses the response entity according to its content type.
     *
     * @param response HTTP response to read
     * @return input stream for the response body
     * @throws IOException if the response body cannot be read
     *
     */
    protected InputStream read(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String contentType = entity.getContentType();
        byte[] bytes;
        if ("application/zip".equals(contentType)) {
            try (ZipInputStream zis =  new ZipInputStream(entity.getContent())) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    throw new ReloadableException("Can't read from url " + url + ": zip entry is null");
                }
                if (entry.isDirectory()) {
                    throw new ReloadableException("Can't read from url " + url + ": zip entry is directory");
                }
                bytes = IOUtils.toByteArray(zis);
            }
        } else if ("application/gzip".equals(contentType)) {
            try (InputStream is = new GZIPInputStream(entity.getContent())) {
                bytes = IOUtils.toByteArray(is);
            }
        } else {
            try (InputStream is = entity.getContent()) {
                bytes = IOUtils.toByteArray(is);
            }
        }
        return new ByteArrayInputStream(bytes);
    }

    /** @return URL supplied to this loader */
    public String getUrl() {
        return url;
    }

    /** @return mapper used to convert the response body */
    public DataMapper<InputStream, T> getDataMapper() {
        return dataMapper;
    }

    /** Creates a short-lived HTTP client with the loader's default timeouts.
     *
     * @return a new HTTP client
     */
    protected CloseableHttpClient createOneTimeHttpClient() {
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(1))
                .setSocketTimeout(Timeout.ofSeconds(60))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(1))
                .setResponseTimeout(Timeout.ofSeconds(60))
                .setCircularRedirectsAllowed(true)
                .build();

        BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
        cm.setConnectionConfig(connConfig);

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(cm)
                .build();
    }

    @Override
    public String toString() {
        return "HttpDataLoader{" +
                "url='" + url + '\'' +
                ", dataMapper=" + dataMapper +
                '}';
    }
}
