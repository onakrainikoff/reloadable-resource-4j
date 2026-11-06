package ru.on8off.reloadable.resource.mapper;

import org.junit.jupiter.api.Test;
import ru.on8off.reloadable.resource.ReloadableData;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

class BaseDataMapperTest {

    @Test
    void testApply() {
        ReloadableData<String> data = new ReloadableData<>(LocalDateTime.now(), "Test");
        BaseDataMapper<String, String> mapper = new BaseDataMapper<>() {
            @Override
            public String map(String dataFrom) {
                return dataFrom;
            }
        };
        ReloadableData<String> data2 = mapper.apply(data);
        assertNotSame(data, data2);
        assertEquals(data.getLastModified(), data2.getLastModified());
        assertEquals(data.getValue(), data2.getValue());
    }


    @Test
    void testPipeline(){
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
    }


}