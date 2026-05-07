package com.interswitch.verveguarddemo.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("Africa/Lagos"));
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        Hibernate7Module hibernate6Module = new Hibernate7Module();
        hibernate6Module.configure(Hibernate7Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate6Module.configure(Hibernate7Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);

        return JsonMapper.builder()
                .addModule(javaTimeModule)
                .addModule(hibernate6Module)
                .build()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDateFormat(sdf);
    }
}
