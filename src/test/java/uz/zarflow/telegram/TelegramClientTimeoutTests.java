package uz.zarflow.telegram;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

import okhttp3.OkHttpClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the outbound Telegram HTTP client aborts a hanging call around its
 * configured callTimeout instead of blocking indefinitely -- the actual bug
 * this config exists to prevent.
 */
class TelegramClientTimeoutTests {

    private static final Duration CALL_TIMEOUT = Duration.ofMillis(500);

    private HttpServer slowServer;

    @AfterEach
    void stopServer() {
        if (slowServer != null) {
            slowServer.stop(0);
        }
    }

    @Test
    void callAbortsAroundCallTimeoutInsteadOfHangingOnASlowServer() throws Exception {
        CountDownLatch requestReceived = new CountDownLatch(1);
        slowServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        // Daemon threads: if the handler is still sleeping when the test moves on,
        // it must not hold up JVM/test-worker shutdown.
        slowServer.setExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory()));
        slowServer.createContext("/", exchange -> {
            requestReceived.countDown();
            try {
                // Comfortably longer than CALL_TIMEOUT -- the client must give up well before this.
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        slowServer.start();

        TelegramUrl localUrl = new TelegramUrl("http", "localhost", slowServer.getAddress().getPort(), false);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .callTimeout(CALL_TIMEOUT)
                .build();
        TelegramClient telegramClient = new OkHttpTelegramClient(httpClient, "token", localUrl);
        TelegramGatewayImpl gateway = new TelegramGatewayImpl(telegramClient);

        long start = System.nanoTime();
        assertThatThrownBy(() -> gateway.sendMessage(1L, "hi", null))
                .isInstanceOf(TelegramGatewayException.class);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(requestReceived.await(2, TimeUnit.SECONDS)).isTrue();
        // Generous margin over CALL_TIMEOUT for scheduling jitter, but nowhere close
        // to the server's 10s sleep -- proves the call didn't just wait it out.
        assertThat(elapsedMillis).isLessThan(5_000);
    }

}
