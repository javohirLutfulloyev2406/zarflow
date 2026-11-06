package uz.zarflow.telegram;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramBotStartupRunnerTest {

    private final TelegramBotProperties properties = new TelegramBotProperties(
            "token", "secret123", "https://example.test", "https://example.test/app", true);

    @Test
    void callsAllFourSetupStepsWithExpectedArgs() {
        FakeTelegramGateway gateway = new FakeTelegramGateway();
        TelegramBotStartupRunner runner = new TelegramBotStartupRunner(gateway, properties);

        runner.registerWithTelegram();

        assertThat(gateway.setWebhookCalls()).hasSize(1);
        assertThat(gateway.setWebhookCalls().get(0).url()).isEqualTo("https://example.test/telegram/webhook");
        assertThat(gateway.setWebhookCalls().get(0).secretToken()).isEqualTo("secret123");

        assertThat(gateway.setChatMenuButtonCalls()).hasSize(1);
        assertThat(gateway.setChatMenuButtonCalls().get(0).webAppUrl()).isEqualTo("https://example.test/app");

        assertThat(gateway.setMyCommandsCalls()).hasSize(2);
        assertThat(gateway.setMyCommandsCalls()).anyMatch(call -> call.languageCode() == null);
        assertThat(gateway.setMyCommandsCalls()).anyMatch(call -> "ru".equals(call.languageCode()));
    }

    @Test
    void oneFailingStepDoesNotBlockTheOthersOrThrow() {
        FakeTelegramGateway gateway = new FakeTelegramGateway() {
            @Override
            public void setWebhook(String url, String secretToken, List<String> allowedUpdates, boolean dropPendingUpdates) {
                throw new TelegramGatewayException("setWebhook");
            }
        };
        TelegramBotStartupRunner runner = new TelegramBotStartupRunner(gateway, properties);

        runner.registerWithTelegram();

        assertThat(gateway.setChatMenuButtonCalls()).hasSize(1);
        assertThat(gateway.setMyCommandsCalls()).hasSize(2);
    }

    @Test
    void runReturnsImmediatelyWithoutWaitingForTelegram() throws InterruptedException {
        CountDownLatch releaseGateway = new CountDownLatch(1);
        FakeTelegramGateway gateway = new FakeTelegramGateway() {
            @Override
            public void setWebhook(String url, String secretToken, List<String> allowedUpdates, boolean dropPendingUpdates) {
                try {
                    releaseGateway.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        TelegramBotStartupRunner runner = new TelegramBotStartupRunner(gateway, properties);

        long start = System.nanoTime();
        runner.run(null);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        // If run() still called the gateway synchronously, this would take ~5s (or the
        // full latch timeout) instead of returning almost instantly.
        assertThat(elapsedMillis).isLessThan(500);
        releaseGateway.countDown();
    }

}
