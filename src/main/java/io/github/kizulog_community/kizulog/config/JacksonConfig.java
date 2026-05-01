package io.github.kizulog_community.kizulog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson設定クラス
 *
 * <p>ObjectMapperのBean定義を行う。
 * JavaTimeModuleを登録してOffsetDateTime等のJava8日時型をサポートする。</p>
 *
 * @author Jun Kobayashi
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapperのBean定義
     *
     * @return ObjectMapper
     */
    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}