package com.interswitch.verveguarddemo.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalResponseWrapperTest {

    private GlobalResponseWrapper wrapper;
    private ServerHttpRequest request;
    private ServerHttpResponse response;

    @BeforeEach
    void setUp() {
        wrapper = new GlobalResponseWrapper();
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
    }

    @Test
    void supports_returnsTrueForRegularObjects() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);

        boolean result = wrapper.supports(param, StringHttpMessageConverter.class);

        assertThat(result).isTrue();
    }

    @Test
    void supports_returnsFalseForApiSuccess() throws Exception {
        Method method = TestController.class.getMethod("apiSuccessResponse");
        MethodParameter param = new MethodParameter(method, -1);

        boolean result = wrapper.supports(param, StringHttpMessageConverter.class);

        assertThat(result).isFalse();
    }

    @Test
    void supports_returnsFalseForApiError() throws Exception {
        Method method = TestController.class.getMethod("apiErrorResponse");
        MethodParameter param = new MethodParameter(method, -1);

        boolean result = wrapper.supports(param, StringHttpMessageConverter.class);

        assertThat(result).isFalse();
    }

    @Test
    void beforeBodyWrite_wrapsRegularObject() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);
        Map<String, String> body = Map.of("key", "value");

        Object result = wrapper.beforeBodyWrite(
                body, param, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response
        );

        assertThat(result).isInstanceOf(ApiSuccess.class);
        ApiSuccess apiSuccess = (ApiSuccess) result;
        assertThat(apiSuccess.message()).isEqualTo("success");
        assertThat(apiSuccess.data()).isEqualTo(body);
    }

    @Test
    void beforeBodyWrite_passesResponseEntityUnchanged() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);
        ResponseEntity<String> body = ResponseEntity.ok("test");

        Object result = wrapper.beforeBodyWrite(
                body, param, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response
        );

        assertThat(result).isSameAs(body);
    }

    @Test
    void beforeBodyWrite_passesResourceUnchanged() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);
        Resource body = new ByteArrayResource("test".getBytes());

        Object result = wrapper.beforeBodyWrite(
                body, param, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response
        );

        assertThat(result).isSameAs(body);
    }

    @Test
    void beforeBodyWrite_passesStringUnchanged() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);
        String body = "plain text";

        Object result = wrapper.beforeBodyWrite(
                body, param, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response
        );

        assertThat(result).isSameAs(body);
    }

    @Test
    void beforeBodyWrite_passesByteArrayUnchanged() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);
        byte[] body = "bytes".getBytes();

        Object result = wrapper.beforeBodyWrite(
                body, param, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response
        );

        assertThat(result).isSameAs(body);
    }

    @Test
    void beforeBodyWrite_passesApiErrorUnchanged() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);
        ApiError body = new ApiError("/test", "error", "trace", 400, LocalDateTime.now());

        Object result = wrapper.beforeBodyWrite(
                body, param, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response
        );

        assertThat(result).isSameAs(body);
    }

    @Test
    void beforeBodyWrite_passesPdfUnchanged() throws Exception {
        Method method = TestController.class.getMethod("regularResponse");
        MethodParameter param = new MethodParameter(method, -1);
        Map<String, String> body = Map.of("key", "value");

        Object result = wrapper.beforeBodyWrite(
                body, param, MediaType.APPLICATION_PDF,
                StringHttpMessageConverter.class, request, response
        );

        assertThat(result).isSameAs(body);
    }

    // Helper class for method parameter resolution
    static class TestController {
        public Map<String, String> regularResponse() {
            return Map.of();
        }

        public ApiSuccess apiSuccessResponse() {
            return new ApiSuccess("success", null);
        }

        public ApiError apiErrorResponse() {
            return new ApiError("/test", "error", "trace", 400, LocalDateTime.now());
        }
    }
}
