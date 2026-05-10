package com.interswitch.verveguarddemo.configuration;

import com.interswitch.verveguarddemo.interceptors.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@RequiredArgsConstructor
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    @Value("${verveguard.observability.enabled:true}")
    private boolean observabilityEnabled;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", HandlerTypePredicate.forBasePackage("com.interswitch.verveguarddemo.controllers.v1"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (observabilityEnabled) {
            registry.addInterceptor(loggingInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns("/api/**/actuator/**")
                    .excludePathPatterns("/api/**/fraud/ping");
        }
    }
}