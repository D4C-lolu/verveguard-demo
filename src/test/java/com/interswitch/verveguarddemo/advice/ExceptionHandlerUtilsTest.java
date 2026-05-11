package com.interswitch.verveguarddemo.advice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionHandlerUtilsTest {

    @Test
    void badRequest_returnsCorrectResponse() {
        ResponseEntity<ApiError> response = ExceptionHandlerUtils.badRequest(
                "Invalid input", "/api/test", "trace-123"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorMessage()).isEqualTo("Invalid input");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().errorTraceId()).isEqualTo("trace-123");
        assertThat(response.getBody().statusCode()).isEqualTo(400);
    }

    @Test
    void conflict_returnsCorrectResponse() {
        ResponseEntity<ApiError> response = ExceptionHandlerUtils.conflict(
                "Already exists", "/api/resource", "trace-456"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorMessage()).isEqualTo("Already exists");
        assertThat(response.getBody().path()).isEqualTo("/api/resource");
        assertThat(response.getBody().errorTraceId()).isEqualTo("trace-456");
        assertThat(response.getBody().statusCode()).isEqualTo(409);
    }

    @Test
    void notFound_returnsCorrectResponse() {
        ResponseEntity<ApiError> response = ExceptionHandlerUtils.notFound(
                "Resource not found", "/api/missing", "trace-789"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorMessage()).isEqualTo("Resource not found");
        assertThat(response.getBody().path()).isEqualTo("/api/missing");
        assertThat(response.getBody().errorTraceId()).isEqualTo("trace-789");
        assertThat(response.getBody().statusCode()).isEqualTo(404);
    }

    @Test
    void resolveMessage_returnsDefaultForGenericException() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getCause()).thenReturn(new RuntimeException("Some error"));

        String message = ExceptionHandlerUtils.resolveMessage(ex);

        assertThat(message).isEqualTo("Invalid request format");
    }

    @Test
    void resolveMessage_returnsDefaultForNullCause() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getCause()).thenReturn(null);

        String message = ExceptionHandlerUtils.resolveMessage(ex);

        assertThat(message).isEqualTo("Invalid request format");
    }

    @Test
    void resolveValidationMessage_handlesFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "must be valid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(bindingResult.getGlobalErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        String message = ExceptionHandlerUtils.resolveValidationMessage(ex);

        assertThat(message).contains("'email'");
        assertThat(message).contains("must be valid");
    }

    @Test
    void resolveValidationMessage_handlesGlobalErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        ObjectError globalError = new ObjectError("object", "Passwords must match");
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        String message = ExceptionHandlerUtils.resolveValidationMessage(ex);

        assertThat(message).isEqualTo("Passwords must match");
    }

    @Test
    void resolveValidationMessage_handlesMultipleErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("object", "email", "must be valid");
        FieldError fieldError2 = new FieldError("object", "name", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        when(bindingResult.getGlobalErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        String message = ExceptionHandlerUtils.resolveValidationMessage(ex);

        assertThat(message).contains("'email'");
        assertThat(message).contains("'name'");
        assertThat(message).contains(",");
    }

    @Test
    void resolveValidationMessage_returnsDefaultWhenEmpty() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        when(bindingResult.getGlobalErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        String message = ExceptionHandlerUtils.resolveValidationMessage(ex);

        assertThat(message).isEqualTo("Validation failed");
    }

    @Test
    void resolveConstraintViolationMessage_handlesViolations() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("user.email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be valid email");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        String message = ExceptionHandlerUtils.resolveConstraintViolationMessage(ex);

        assertThat(message).contains("'email'");
        assertThat(message).contains("must be valid email");
    }

    @Test
    void resolveConstraintViolationMessage_handlesSimplePath() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("invalid");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        String message = ExceptionHandlerUtils.resolveConstraintViolationMessage(ex);

        assertThat(message).contains("'email'");
        assertThat(message).contains("invalid");
    }

    @Test
    void resolveConstraintViolationMessage_returnsDefaultWhenEmpty() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());

        String message = ExceptionHandlerUtils.resolveConstraintViolationMessage(ex);

        assertThat(message).isEqualTo("Validation failed");
    }
}
