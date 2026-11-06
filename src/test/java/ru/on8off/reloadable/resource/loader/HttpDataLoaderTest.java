package ru.on8off.reloadable.resource.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.on8off.reloadable.resource.ReloadableData;
import ru.on8off.reloadable.resource.mapper.JsonNodeDataMapper;
import ru.on8off.reloadable.resource.mapper.StringDataMapper;
import ru.on8off.reloadable.resource.mapper.StringListDataMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpDataLoaderTest {
    static final String TEST_FILE_TXT = "files/file.txt";
    static final String TEST_FILE_TXT_ZIP = "files/file.zip";
    static final String TEST_FILE_TXT_GZ = "files/file.gz";

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().port(8080))
            .build();

    @Test
    public void testLoadTxt() throws IOException {
        List<String> content = IOUtils.readLines(
                Files.newInputStream(
                        new File(HttpDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()).toPath()
                )
        );
        File file = new File(HttpDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile());
        LocalDateTime lastModified = LocalDateTime.now();
        String previousLastModified = DateUtils.formatStandardDate(lastModified.atZone(ZoneId.systemDefault()).toInstant());

        stubFor(get(urlEqualTo("/config.txt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader(HttpHeaders.LAST_MODIFIED, previousLastModified)
                        .withBody(IOUtils.toByteArray(Files.newInputStream(file.toPath())))
                        .withBodyFile("config.txt")));

        String url = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/config.txt";
        StringListDataMapper mapper = new StringListDataMapper();
        HttpDataLoader<List<String>> dataLoader = new HttpDataLoader<>(url, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals(0 , ChronoUnit.SECONDS.between(lastModified, data.getLastModified()));
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());
    }


    @Test
    public void testLoadZip() throws IOException {
        List<String> content = IOUtils.readLines(
                Files.newInputStream(
                        new File(HttpDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()).toPath()
                )
        );

        File file = new File(HttpDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT_ZIP).getFile());
        LocalDateTime lastModified = LocalDateTime.now();
        String previousLastModified = DateUtils.formatStandardDate(lastModified.atZone(ZoneId.systemDefault()).toInstant());

        stubFor(get(urlEqualTo("/file.zip"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/zip")
                        .withHeader(HttpHeaders.LAST_MODIFIED, previousLastModified)
                        .withBody(IOUtils.toByteArray(Files.newInputStream(file.toPath())))
                        .withBodyFile("file.zip")));

        String url = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/file.zip";
        StringListDataMapper mapper = new StringListDataMapper();
        HttpDataLoader<List<String>> dataLoader = new HttpDataLoader<>(url, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals(0 , ChronoUnit.SECONDS.between(lastModified, data.getLastModified()));
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());
    }

    @Test
    public void testLoadGZip() throws IOException {
        List<String> content = IOUtils.readLines(
                Files.newInputStream(
                        new File(HttpDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT).getFile()).toPath()
                )
        );

        File file = new File(HttpDataLoaderTest.class.getClassLoader().getResource(TEST_FILE_TXT_GZ).getFile());
        LocalDateTime lastModified = LocalDateTime.now();
        String previousLastModified = DateUtils.formatStandardDate(lastModified.atZone(ZoneId.systemDefault()).toInstant());

        stubFor(get(urlEqualTo("/file.gz"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/gzip")
                        .withHeader(HttpHeaders.LAST_MODIFIED, previousLastModified)
                        .withBody(IOUtils.toByteArray(Files.newInputStream(file.toPath())))
                        .withBodyFile("file.gz")));

        String url = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/file.gz";
        StringListDataMapper mapper = new StringListDataMapper();
        HttpDataLoader<List<String>> dataLoader = new HttpDataLoader<>(url, mapper);
        ReloadableData<List<String>> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals(0 , ChronoUnit.SECONDS.between(lastModified, data.getLastModified()));
        assertNotNull(data.getValue());
        assertEquals(content, data.getValue());
    }


    @Test
    public void testLoadJson() throws IOException {
        LocalDateTime lastModified = LocalDateTime.now();
        String lastModifiedString = DateUtils.formatStandardDate(lastModified.atZone(ZoneId.systemDefault()).toInstant());
        String json = "{\"test1\": 1, \"test2\": 2, \"test3\": 3}";
        stubFor(get(urlEqualTo("/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader(HttpHeaders.LAST_MODIFIED, lastModifiedString)
                        .withBody(json)));


        String url = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/json";
        JsonNodeDataMapper mapper = new JsonNodeDataMapper();
        HttpDataLoader<JsonNode> dataLoader = new HttpDataLoader<>(url, mapper);
        ReloadableData<JsonNode> data = dataLoader.load(null);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals(0 , ChronoUnit.SECONDS.between(lastModified, data.getLastModified()));
        assertNotNull(data.getValue());
        assertEquals(3, data.getValue().get("test3").asInt());
    }

    @Test
    public void testLastModified() throws IOException {
        LocalDateTime lastModified = LocalDateTime.now();
        String lastModifiedString = DateUtils.formatStandardDate(lastModified.atZone(ZoneId.systemDefault()).toInstant());
        String text = "test";

        wireMockServer.stubFor(get(urlEqualTo("/lastModified1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader(HttpHeaders.LAST_MODIFIED, lastModifiedString)
                        .withBody(text)));
        wireMockServer.stubFor(get(urlEqualTo("/lastModified2"))
                .willReturn(aResponse()
                        .withStatus(304)));

        String url = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/lastModified1";
        StringDataMapper mapper = new StringDataMapper();
        HttpDataLoader<String> dataLoader = new HttpDataLoader<>(url, mapper);
        ReloadableData<String> data = dataLoader.load(lastModified);
        assertNotNull(data);
        assertNotNull(data.getLastModified());
        assertEquals(0 , ChronoUnit.SECONDS.between(lastModified, data.getLastModified()));
        assertNotNull(data.getValue());
        assertEquals(text, data.getValue());

        String url2 = wireMockServer.getRuntimeInfo().getHttpBaseUrl() + "/lastModified2";
        HttpDataLoader<String> dataLoader2 = new HttpDataLoader<>(url2, mapper);
        ReloadableData<String> data2 = dataLoader2.load(lastModified);
        assertNull(data2);
    }


    @Test
    public void testExceptions() throws IOException {
        Throwable exception = assertThrows(NullPointerException.class, () -> new HttpDataLoader<>(null, null));
        assertTrue(exception.getMessage().contains("Param 'url' must not be null"));

        exception = assertThrows(NullPointerException.class, () -> new HttpDataLoader<>("test.txt", null));
        assertTrue(exception.getMessage().contains("Param 'dataMapper' must not be null"));

    }


}