package com.interswitch.verveguarddemo.advice;

import com.interswitch.verveguarddemo.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseBodyAdviceImplTest {

    private ResponseBodyAdviceImpl advice;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new ResponseBodyAdviceImpl();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Nested
    class BadRequestExceptionHandlers {

        @Test
        void handlesBadRequestException() {
            var ex = new BadRequestException("Invalid input");

            ResponseEntity<ApiError> response = advice.handleBadRequestException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Invalid input");
        }

        @Test
        void handlesIllegalArgumentException() {
            var ex = new IllegalArgumentException("Bad argument");

            ResponseEntity<ApiError> response = advice.handleBadRequestException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Bad argument");
        }

        @Test
        void handlesIllegalStateException() {
            var ex = new IllegalStateException("Bad state");

            ResponseEntity<ApiError> response = advice.handleBadRequestException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void handlesHttpRequestMethodNotSupportedException() {
            var ex = new HttpRequestMethodNotSupportedException("POST");

            ResponseEntity<ApiError> response = advice.handleBadRequestException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class HttpMessageNotReadableHandlers {

        @Test
        void handlesHttpMessageNotReadableException() {
            var ex = new HttpMessageNotReadableException("Cannot read", (Throwable) null, null);

            ResponseEntity<ApiError> response = advice.handleHttpMessageNotReadableException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class ConflictHandlers {

        @Test
        void handlesConflictException() {
            var ex = new ConflictException("Already exists");

            ResponseEntity<ApiError> response = advice.handleConflictException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Already exists");
        }
    }

    @Nested
    class AuthenticationHandlers {

        @Test
        void handlesBadCredentialsException() {
            var ex = new BadCredentialsException("Bad creds");

            ResponseEntity<ApiError> response = advice.handleBadCredentialsException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Invalid email or password");
        }

        @Test
        void handlesLockedException() {
            var ex = new LockedException("Account locked");

            ResponseEntity<ApiError> response = advice.handleAccountStatusException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Account is not accessible");
        }

        @Test
        void handlesDisabledException() {
            var ex = new DisabledException("Account disabled");

            ResponseEntity<ApiError> response = advice.handleAccountStatusException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Account is not accessible");
        }

        @Test
        void handlesUnauthorizedException() {
            var ex = new UnauthorizedException("Not authenticated");

            ResponseEntity<ApiError> response = advice.handleUnauthenticatedException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Unauthorized");
        }

        @Test
        void handlesInvalidTokenException() {
            var ex = new InvalidTokenException("Token expired");

            ResponseEntity<ApiError> response = advice.handleInvalidTokenException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Token expired");
        }
    }

    @Nested
    class ForbiddenHandlers {

        @Test
        void handlesAccessDeniedException() {
            var ex = new AccessDeniedException("Access denied");

            ResponseEntity<ApiError> response = advice.handleForbiddenException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Access denied");
        }

        @Test
        void handlesForbiddenException() {
            var ex = new ForbiddenException("Not allowed");

            ResponseEntity<ApiError> response = advice.handleForbiddenException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Not allowed");
        }

        @Test
        void handlesFraudDetectedException() {
            var ex = new FraudDetectedException("Fraud detected");

            ResponseEntity<ApiError> response = advice.handleFraudDetectedException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Fraud detected");
        }
    }

    @Nested
    class NotFoundHandlers {

        @Test
        void handlesNotFoundException() {
            var ex = new NotFoundException("Not found");

            ResponseEntity<ApiError> response = advice.handleNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("Resource not found");
        }

        @Test
        void handlesNoSuchElementException() {
            var ex = new NoSuchElementException("Element missing");

            ResponseEntity<ApiError> response = advice.handleNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GenericHandlers {

        @Test
        void handlesGenericException() {
            var ex = new RuntimeException("Something went wrong");

            ResponseEntity<ApiError> response = advice.handleException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorMessage()).isEqualTo("An internal server error has occurred");
        }
    }
}
