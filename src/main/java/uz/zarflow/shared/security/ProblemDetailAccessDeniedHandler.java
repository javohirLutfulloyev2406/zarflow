package uz.zarflow.shared.security;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.LocaleResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Same reasoning as {@link ProblemDetailAuthenticationEntryPoint} — this also runs outside MVC dispatch. */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;
    private final ObjectMapper objectMapper;

    public ProblemDetailAccessDeniedHandler(
            MessageSource messageSource, LocaleResolver localeResolver, ObjectMapper objectMapper) {
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        Locale locale = localeResolver.resolveLocale(request);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
        problemDetail.setDetail(messageSource.getMessage("auth.access-denied", null, locale));
        problemDetail.setProperty("errorCode", "access-denied");
        problemDetail.setProperty("requestId", MDC.get("requestId"));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }

}
