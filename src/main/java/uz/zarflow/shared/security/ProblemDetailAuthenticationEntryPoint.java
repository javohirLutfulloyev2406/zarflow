package uz.zarflow.shared.security;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.LocaleResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs before Spring MVC dispatch, so it can't reuse GlobalExceptionHandler by
 * extension — builds the same ProblemDetail shape by hand instead.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;
    private final ObjectMapper objectMapper;

    public ProblemDetailAuthenticationEntryPoint(
            MessageSource messageSource, LocaleResolver localeResolver, ObjectMapper objectMapper) {
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Locale locale = localeResolver.resolveLocale(request);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        problemDetail.setDetail(messageSource.getMessage("auth.unauthenticated", null, locale));
        problemDetail.setProperty("errorCode", "unauthenticated");
        problemDetail.setProperty("requestId", MDC.get("requestId"));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }

}
