package com.interswitch.verveguarddemo.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    private IpUtil() {
    }

    public static String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
