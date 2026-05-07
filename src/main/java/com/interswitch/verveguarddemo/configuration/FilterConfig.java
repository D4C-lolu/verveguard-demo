package com.interswitch.verveguarddemo.configuration;

import com.interswitch.verveguarddemo.filters.JwtAuthFilter;
import com.interswitch.verveguarddemo.filters.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false); // stops Spring Boot from auto-registering it
        return reg;
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterFilterRegistrationBeanFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false); // stops Spring Boot from auto-registering it
        return reg;
    }
}
