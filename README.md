# reloadable-resource-4j
[![](https://jitpack.io/v/onakrainikoff/reloadable-resource-4j.svg)](https://jitpack.io/#onakrainikoff/reloadable-resource-4j)

`reloadable-resource-4j` is a Java library implementing self reloadable resource pattern.

## Features
- Periodic detection of resource changes using modification time.
- Loading data from files, classpath resources, HTTP endpoints and any other custom data sources.
- Decompression of `.zip` or `.gz` files.
- Mapping loaded data to Java types using built-in or custom mappers.
- Listeners mechanism for observing data reload events.

## Requirements

- Java 11 or later.

## Installation

Maven:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.onakrainikoff</groupId>
    <artifactId>reloadable-resource-4j</artifactId>
    <version>1.0.1</version>
</dependency>
```

Gradle:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.onakrainikoff:reloadable-resource-4j:1.0.1'
}
```

## Usage

### Load from file

```java
ReloadableResource<String> resource = new ReloadableResource<>(
        "application-data",
        new FileDataLoader<>("/opt/service/data.txt", new StringDataMapper()),
        30, TimeUnit.SECONDS);

ReloadableData<String> data = resource.get();
String value = data.getValue();
```

`FileDataLoader` detects changes using the file's modification time. Files
ending in `.zip` or `.gz` are decompressed before being passed to the mapper.

### Load from Classpath

```java
ReloadableResource<String> resource = new ReloadableResource<>(
        "application-data",
        new FileDataLoader<>("classpath:data.txt", new StringDataMapper()),
        30, TimeUnit.SECONDS);

ReloadableData<String> data = resource.get();
String value = data.getValue();
```

`FileDataLoader` can not detect changes of resource in classpath. In fact, the resource data will be loaded for the first time only.

### Load from HTTP

```java
ReloadableResource<String> resource = new ReloadableResource<>(
        "application-data",
        new HttpDataLoader<>("https://example.com/data.txt", new StringDataMapper()),
        30, TimeUnit.SECONDS);

ReloadableData<String> data = resource.get();
String value = data.getValue();
```

`HttpDataLoader` sends GET requests and uses the previous modification time in an `If-Modified-Since` header. A `304 Not Modified` response leaves the
currently loaded value unchanged.

The HTTP loader can be constructed with a Map of request headers to include in each request. Headers can be used for authentication, content negotiation, or other purposes:

```java
Map<String, String> headers = Map.of(
    "Authorization", "Bearer <token>",
    "Accept", "application/json"
);

HttpDataLoader<String> dataLoader= new HttpDataLoader<>("https://example.com/data.txt", new StringDataMapper(), headers);
```

The HTTP loader can also be constructed with a reusable Apache
`CloseableHttpClient`; when no client is supplied, it creates and closes a
client for each request:

```java
CloseableHttpClient customHttpClient = HttpClientBuilder.create().build();
HttpDataLoader<String> dataLoader= new HttpDataLoader<>("https://example.com/data.txt", new StringDataMapper(), customHttpClient);
```
### Custom data loader

For another data sources, implement the `DataLoader` interface and provide a `load()` method that returns a `ReloadableData<T>` instance.
The `load()` method receives the last known modification time of the resource, which can be used to determine if the resource has changed.

```java
DataLoader<String> customDataLoader = new DataLoader<>() {
    @Override
    public ReloadableData<String> load(LocalDateTime lastModified) {
        // custom implementation of data loading logic
    }
};
```

### Data mapping

Built-in data mappers:

| Mapper | Result |
|---|---|
| `JsonNodeDataMapper` | JSON as a Jackson `JsonNode`|
| `JsonObjectDataMapper<T>` | JSON deserialized by Jackson to a Java type|
| `PropertiesDataMapper` | Stream contents as `java.util.Properties` |
| `StringDataMapper` | Entire stream as a `String` |
| `StringListDataMapper` | Stream contents as `List<String>` lines |
| `InputStreamDataMapper` | Pass-through mapper for an input stream. |

For custom mapping, implement the `DataMapper<F, T>` interface or extend `BaseDataMapper<F, T>` class.

Also, you can combine multiple mappers using `andThen()` method of `DataMapper` interface:

```java
DataMapper<String, Integer> mapperFirst = new BaseDataMapper<>() {
    @Override
    public Integer map(String dataFrom) {
        return Integer.parseInt(dataFrom);
    }
};

DataMapper<Integer, Integer> mapperSecond = new BaseDataMapper<>() {
    @Override
    public Integer map(Integer dataFrom) {
        return dataFrom * 2;
    }
};

DataMapper<Integer, String> mapperThird = new BaseDataMapper<>() {
    @Override
    public String map(Integer dataFrom) {
        return String.valueOf(dataFrom);
    }
};

DataMapper<String, String> dataMapper = mapperFirst.andThen(mapperSecond).andThen(mapperThird);

ReloadableData<String> data = new ReloadableData<>(LocalDateTime.now(), "5");
ReloadableData<String> data2 = dataMapper.apply(data);
assertEquals("10", data2.getValue());
``` 

### Observing data reload

For observing data reload events, implement the `ReloadableListener` interface and provide a `onReload()` method that will be called after each reload attempt:

```java
ReloadableListener listener = (name, loader, oldValue, newValue, error, elapsedMs) -> {
    if (error != null) {
        System.err.println("Reload failed for " + name + ": " + error.getMessage());
    } else if (newValue != null) {
        System.out.println("Updated " + name + " in " + elapsedMs + " ms");
    }
};

ReloadableResource<String> resource = new ReloadableResource<>(
        "application-data",
        new FileDataLoader<>("/opt/service/data.txt", new StringDataMapper()),
        15,
        TimeUnit.SECONDS,
        listener);
```

### Async first data load

`ReloadableResource` performs first data load during construction in current thread. If you want to avoid this, use `AsyncReloadableResource` instead:

```java
AsyncReloadableResource<String> resource = new AsyncReloadableResource<>(
        "application-data",
        new FileDataLoader<>("/opt/service/data.txt", new StringDataMapper()),
        30, TimeUnit.SECONDS);
resource.start();
ReloadableData<String> data = resource.get();
String value = data.getValue();
```

Call `start()` performs first data load in current thread and waits for it to complete.
Call `startAsync()` performs first data load asynchronously and does not wait for it to complete.

### Shutdown

After the resource is no longer needed or application shuts down, call `shutdown()` method of `ReloadableResource` instance:

```java
Runtime.getRuntime().addShutdownHook(new Thread(resource::shutdown));
```

## Dependencies

| Dependency | Version | Purpose |
|---|---:|---|
| `org.apache.commons:commons-lang3` | `3.18.0` |
| `commons-io:commons-io` | `2.16.1` |
| `org.apache.httpcomponents.client5:httpclient5` | `5.6.5` |
| `com.fasterxml.jackson.core:jackson-databind` | `2.22.4` |
