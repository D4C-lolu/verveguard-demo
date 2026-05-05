package com.interswitch.verveguarddemo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jspecify.annotations.NonNull;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
public class ProxyAwareAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    @NonNull
    @Override
    public WebAuthenticationDetails buildDetails(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String realIp = forwarded.split(",")[0].trim();
            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getRemoteAddr() {
                    return realIp;
                }
            };
        }
        return super.buildDetails(request);
    }
}