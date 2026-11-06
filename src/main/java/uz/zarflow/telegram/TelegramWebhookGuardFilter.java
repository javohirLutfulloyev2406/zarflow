package uz.zarflow.telegram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before the webhook body is ever read: a bad secret is rejected with 401
 * without touching the stream, an oversized Content-Length is rejected with
 * 413 before touching the stream, and a body of unknown/understated length
 * (chunked transfer) is read through a hard cap that aborts to 413 instead of
 * buffering unbounded data.
 */
class TelegramWebhookGuardFilter extends OncePerRequestFilter {

    static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";
    static final int MAX_BODY_BYTES = 1_000_000;

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookGuardFilter.class);

    private final TelegramBotProperties properties;

    TelegramWebhookGuardFilter(TelegramBotProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isValidSecret(request.getHeader(SECRET_HEADER))) {
            log.warn("Rejected webhook request: missing or invalid secret token, requestId={}", MDC.get("requestId"));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            log.warn("Rejected webhook body: Content-Length {} exceeds limit, requestId={}",
                    request.getContentLengthLong(), MDC.get("requestId"));
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        try {
            chain.doFilter(new SizeLimitedRequest(request, MAX_BODY_BYTES), response);
        } catch (PayloadTooLargeException ex) {
            log.warn("Rejected webhook body: exceeded {} bytes while streaming, requestId={}",
                    MAX_BODY_BYTES, MDC.get("requestId"));
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        }
    }

    private boolean isValidSecret(String secretHeader) {
        if (secretHeader == null) {
            return false;
        }
        byte[] provided = secretHeader.getBytes(StandardCharsets.UTF_8);
        byte[] expected = properties.webhookSecret().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, expected);
    }

    /**
     * Deliberately NOT an IOException: Spring's message converters catch IOException
     * while reading the body and translate it into a 400 response themselves, which
     * would resolve the request before this filter's own catch around chain.doFilter
     * ever sees it. An unchecked, unrecognized type propagates untouched.
     */
    private static final class PayloadTooLargeException extends RuntimeException {
    }

    private static final class SizeLimitedRequest extends HttpServletRequestWrapper {
        private final int limit;
        private LimitedServletInputStream cached;

        SizeLimitedRequest(HttpServletRequest request, int limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (cached == null) {
                cached = new LimitedServletInputStream(super.getInputStream(), limit);
            }
            return cached;
        }
    }

    /**
     * Allows exactly {@code limit} bytes to be read to a clean EOF; the (limit+1)th
     * byte is what triggers rejection, so a body of exactly the limit size is never
     * wrongly rejected just for touching the boundary.
     */
    private static final class LimitedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long limit;
        private long readSoFar;

        LimitedServletInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b >= 0) {
                readSoFar++;
                if (readSoFar > limit) {
                    throw new PayloadTooLargeException();
                }
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            long remainingBudget = limit + 1 - readSoFar;
            if (remainingBudget <= 0) {
                throw new PayloadTooLargeException();
            }
            int capped = (int) Math.min(len, remainingBudget);
            int n = delegate.read(b, off, capped);
            if (n > 0) {
                readSoFar += n;
                if (readSoFar > limit) {
                    throw new PayloadTooLargeException();
                }
            }
            return n;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }

}
