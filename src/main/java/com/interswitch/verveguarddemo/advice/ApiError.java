package com.interswitch.verveguarddemo.advice;

import java.time.LocalDateTime;

public record ApiError(String path, String errorMessage, String errorTraceId, int statusCode, LocalDateTime timestamp) {
}
