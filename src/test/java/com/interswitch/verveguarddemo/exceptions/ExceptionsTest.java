package com.interswitch.verveguarddemo.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    // --- NotFoundException ---
    @Test
    void notFoundException_defaultConstructor() {
        var ex = new NotFoundException();
        assertThat(ex.getMessage()).isNull();
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void notFoundException_messageConstructor() {
        var ex = new NotFoundException("User not found");
        assertThat(ex.getMessage()).isEqualTo("User not found");
    }

    @Test
    void notFoundException_messageAndCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new NotFoundException("User not found", cause);
        assertThat(ex.getMessage()).isEqualTo("User not found");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void notFoundException_causeConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new NotFoundException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // --- BadRequestException ---
    @Test
    void badRequestException_defaultConstructor() {
        var ex = new BadRequestException();
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void badRequestException_messageConstructor() {
        var ex = new BadRequestException("Invalid input");
        assertThat(ex.getMessage()).isEqualTo("Invalid input");
    }

    @Test
    void badRequestException_messageAndCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new BadRequestException("Invalid input", cause);
        assertThat(ex.getMessage()).isEqualTo("Invalid input");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void badRequestException_causeConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new BadRequestException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // --- UnauthorizedException ---
    @Test
    void unauthorizedException_defaultConstructor() {
        var ex = new UnauthorizedException();
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void unauthorizedException_messageConstructor() {
        var ex = new UnauthorizedException("Not authenticated");
        assertThat(ex.getMessage()).isEqualTo("Not authenticated");
    }

    @Test
    void unauthorizedException_messageAndCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new UnauthorizedException("Not authenticated", cause);
        assertThat(ex.getMessage()).isEqualTo("Not authenticated");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void unauthorizedException_causeConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new UnauthorizedException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // --- ForbiddenException ---
    @Test
    void forbiddenException_defaultConstructor() {
        var ex = new ForbiddenException();
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void forbiddenException_messageConstructor() {
        var ex = new ForbiddenException("Access denied");
        assertThat(ex.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void forbiddenException_messageAndCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new ForbiddenException("Access denied", cause);
        assertThat(ex.getMessage()).isEqualTo("Access denied");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void forbiddenException_causeConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new ForbiddenException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // --- ConflictException ---
    @Test
    void conflictException_defaultConstructor() {
        var ex = new ConflictException();
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void conflictException_messageConstructor() {
        var ex = new ConflictException("Already exists");
        assertThat(ex.getMessage()).isEqualTo("Already exists");
    }

    @Test
    void conflictException_messageAndCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new ConflictException("Already exists", cause);
        assertThat(ex.getMessage()).isEqualTo("Already exists");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void conflictException_causeConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new ConflictException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // --- InvalidTokenException ---
    @Test
    void invalidTokenException_defaultConstructor() {
        var ex = new InvalidTokenException();
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void invalidTokenException_messageConstructor() {
        var ex = new InvalidTokenException("Token expired");
        assertThat(ex.getMessage()).isEqualTo("Token expired");
    }

    @Test
    void invalidTokenException_messageAndCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new InvalidTokenException("Token expired", cause);
        assertThat(ex.getMessage()).isEqualTo("Token expired");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void invalidTokenException_causeConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new InvalidTokenException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // --- FraudDetectedException ---
    @Test
    void fraudDetectedException_defaultConstructor() {
        var ex = new FraudDetectedException();
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void fraudDetectedException_messageConstructor() {
        var ex = new FraudDetectedException("Suspicious activity");
        assertThat(ex.getMessage()).isEqualTo("Suspicious activity");
    }

    @Test
    void fraudDetectedException_messageAndCauseConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new FraudDetectedException("Suspicious activity", cause);
        assertThat(ex.getMessage()).isEqualTo("Suspicious activity");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void fraudDetectedException_causeConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new FraudDetectedException(cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
