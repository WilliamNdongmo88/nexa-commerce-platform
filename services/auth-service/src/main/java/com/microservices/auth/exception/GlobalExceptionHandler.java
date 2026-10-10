package com.microservices.auth.exception;

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

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest req) {
        return buildProblem(HttpStatus.CONFLICT, "Email already exists", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex, HttpServletRequest req) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid token", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "Invalid request payload",
                req
        );

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", ex.getMessage(), req);
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