package com.microservices.payment.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handleNotFound(PaymentNotFoundException ex, HttpServletRequest req) {
        return buildProblem(HttpStatus.NOT_FOUND, "Payment not found", ex.getMessage(), req);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ProblemDetail handlePaymentFailed(PaymentFailedException ex, HttpServletRequest req) {
        log.warn("PaymentFailedException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ProblemDetail problem = buildProblem(
                HttpStatus.UNPROCESSABLE_ENTITY, "Payment failed", ex.getMessage(), req);
        problem.setProperty("errorCode", ex.getErrorCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST, "Validation failed", "Invalid request payload", req);

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid argument", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred. Please contact support.", req);
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, String detail, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://api.example.com/errors/" + status.value()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    public record ValidationError(String field, String message) {}
}