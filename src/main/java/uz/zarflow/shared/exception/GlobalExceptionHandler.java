package uz.zarflow.shared.exception;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

/**
 * @Order(HIGHEST_PRECEDENCE): with spring.mvc.problemdetails.enabled=true, Spring's
 * own internal ProblemDetail handling also declares handlers for
 * MethodArgumentNotValidException/HttpMessageNotReadableException. Without an
 * explicit order, that internal advice can win the tie for those two specific
 * exception types instead of ours.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URI = "https://zarflow.uz/problems/";

    private final MessageSource messageSource;

    GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex, Locale locale) {
        return problem(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getArgs(), locale);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail handleUnauthorized(UnauthorizedException ex, Locale locale) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getArgs(), locale);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex, Locale locale) {
        return problem(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getArgs(), locale);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex, Locale locale) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode(), ex.getArgs(), locale);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, Locale locale) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return validationProblem(errors, locale);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex, Locale locale) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList();
        return validationProblem(errors, locale);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, Locale locale) {
        return validationProblem(List.of(), locale);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, Locale locale) {
        String requestId = MDC.get("requestId");
        log.error("Unhandled exception, requestId={}", requestId, ex);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        problemDetail.setDetail(messageSource.getMessage("error.internal", null, locale));
        problemDetail.setProperty("errorCode", "internal-error");
        problemDetail.setProperty("requestId", requestId);
        return problemDetail;
    }

    private ProblemDetail validationProblem(List<Map<String, String>> errors, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problemDetail.setDetail(messageSource.getMessage("error.validation", null, locale));
        problemDetail.setProperty("errorCode", "validation-error");
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("requestId", MDC.get("requestId"));
        return problemDetail;
    }

    private ProblemDetail problem(HttpStatus status, String errorCode, Object[] args, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setType(URI.create(PROBLEM_BASE_URI + errorCode));
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setDetail(messageSource.getMessage(errorCode, args, errorCode, locale));
        problemDetail.setProperty("errorCode", errorCode);
        problemDetail.setProperty("requestId", MDC.get("requestId"));
        return problemDetail;
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", fieldError.getDefaultMessage() == null ? "" : fieldError.getDefaultMessage());
    }

}
