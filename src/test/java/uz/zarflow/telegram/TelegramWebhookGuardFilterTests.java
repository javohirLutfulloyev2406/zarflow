package uz.zarflow.telegram;

import java.time.Duration;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TelegramWebhookGuardFilterTests {

    private static final String CORRECT_SECRET = "correct-secret-123";

    private final TelegramBotProperties properties = new TelegramBotProperties(
            "token", CORRECT_SECRET, "https://example.test/webhook", "https://example.test/app", false,
            Duration.ofSeconds(5), Duration.ofSeconds(10));

    private final TelegramWebhookGuardFilter filter = new TelegramWebhookGuardFilter(properties);

    private static final FilterChain FAIL_IF_INVOKED =
            (req, res) -> fail("chain.doFilter must not be reached");

    @Test
    void wrongSecretRejectsWithoutReadingBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", TelegramWebhookController.WEBHOOK_PATH);
        request.addHeader(TelegramWebhookGuardFilter.SECRET_HEADER, "wrong-secret");
        request.setContent(new byte[5_000_000]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, FAIL_IF_INVOKED);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void missingSecretHeaderRejectsWithoutReadingBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", TelegramWebhookController.WEBHOOK_PATH);
        request.setContent(new byte[100]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, FAIL_IF_INVOKED);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void correctSecretWithContentLengthOverCapReturns413WithoutReadingBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", TelegramWebhookController.WEBHOOK_PATH);
        request.addHeader(TelegramWebhookGuardFilter.SECRET_HEADER, CORRECT_SECRET);
        request.setContent(new byte[2_000_000]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, FAIL_IF_INVOKED);

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void correctSecretChunkedBodyOverCapAbortsReadAndReturns413() throws Exception {
        byte[] oversized = new byte[2_000_000];
        MockHttpServletRequest request = new MockHttpServletRequest("POST", TelegramWebhookController.WEBHOOK_PATH) {
            @Override
            public long getContentLengthLong() {
                return -1; // simulate chunked transfer: declared length unknown
            }

            @Override
            public int getContentLength() {
                return -1;
            }
        };
        request.addHeader(TelegramWebhookGuardFilter.SECRET_HEADER, CORRECT_SECRET);
        request.setContent(oversized);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> req.getInputStream().readAllBytes());

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void correctSecretAndSizeWithinCapPassesThroughToChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", TelegramWebhookController.WEBHOOK_PATH);
        request.addHeader(TelegramWebhookGuardFilter.SECRET_HEADER, CORRECT_SECRET);
        request.setContent("{}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (req, res) -> chainInvoked[0] = true);

        assertThat(chainInvoked[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void exactlyAtCapBodyIsNotRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", TelegramWebhookController.WEBHOOK_PATH) {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public int getContentLength() {
                return -1;
            }
        };
        request.addHeader(TelegramWebhookGuardFilter.SECRET_HEADER, CORRECT_SECRET);
        request.setContent(new byte[TelegramWebhookGuardFilter.MAX_BODY_BYTES]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            byte[] read = req.getInputStream().readAllBytes();
            assertThat(read).hasSize(TelegramWebhookGuardFilter.MAX_BODY_BYTES);
        });

        assertThat(response.getStatus()).isEqualTo(200);
    }

}
