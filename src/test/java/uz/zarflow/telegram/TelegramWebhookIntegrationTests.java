package uz.zarflow.telegram;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TelegramWebhookIntegrationTests {

    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeTelegramGateway fakeTelegramGateway;

    @Value("${zarflow.telegram.webhook-secret}")
    private String webhookSecret;

    @Value("${zarflow.telegram.mini-app-url}")
    private String miniAppUrl;

    @TestConfiguration
    static class FakeGatewayConfig {
        @Bean
        @Primary
        FakeTelegramGateway fakeTelegramGateway() {
            return new FakeTelegramGateway();
        }
    }

    @Test
    void missingSecretHeaderIsUnauthorized() {
        long chatId = uniqueId();
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Ali", null, null, "uz", "/start");
        int sentBefore = fakeTelegramGateway.sentMessages().size();

        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.post("/telegram/webhook").contentType(MediaType.APPLICATION_JSON).body(body),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(userCount(chatId)).isZero();
        assertThat(fakeTelegramGateway.sentMessages()).hasSize(sentBefore);
    }

    @Test
    void wrongSecretHeaderIsUnauthorized() {
        long chatId = uniqueId();
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Ali", null, null, "uz", "/start");

        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.post("/telegram/webhook")
                        .header(SECRET_HEADER, "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(userCount(chatId)).isZero();
    }

    @Test
    void startFromNewUzUserCreatesUserAndSendsAppButton() {
        long chatId = uniqueId();
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Ali", "Valiyev", "alivaliyev", "uz", "/start");

        ResponseEntity<String> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCount(chatId)).isEqualTo(1);
        FakeTelegramGateway.SentMessage sent = lastSentTo(chatId);
        assertThat(sent.text()).contains("Ali");
        assertThat(FakeTelegramGateway.webAppUrlOf(sent)).isEqualTo(miniAppUrl);
    }

    @Test
    void startWithRuLanguageRepliesInRussianAndSecondStartDoesNotDuplicateUser() {
        long chatId = uniqueId();
        String first = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Ali", null, null, "ru", "/start");
        post(first);

        FakeTelegramGateway.SentMessage firstReply = lastSentTo(chatId);
        assertThat(firstReply.text()).contains("Здравствуйте");

        String second = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Alisher", null, null, "uz", "/start");
        post(second);

        assertThat(userCount(chatId)).isEqualTo(1);
        String storedLanguage = jdbcTemplate.queryForObject(
                "select language from users where telegram_id = ?", String.class, chatId);
        assertThat(storedLanguage).isEqualTo("ru");
    }

    @Test
    void duplicateUpdateIdIsProcessedOnce() {
        long chatId = uniqueId();
        long updateId = uniqueId();
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                updateId, chatId, chatId, "Ali", null, null, "uz", "/start");

        post(body);
        int sentAfterFirst = countSentTo(chatId);
        post(body);
        int sentAfterSecond = countSentTo(chatId);

        assertThat(sentAfterFirst).isEqualTo(1);
        assertThat(sentAfterSecond).isEqualTo(1);
    }

    @Test
    void groupChatMessageIsIgnored() {
        long chatId = uniqueId();
        int sentBefore = fakeTelegramGateway.sentMessages().size();
        String body = TelegramUpdateTestSupport.groupMessageUpdate(uniqueId(), chatId, chatId, "/start");

        ResponseEntity<String> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCount(chatId)).isZero();
        assertThat(fakeTelegramGateway.sentMessages()).hasSize(sentBefore);
    }

    @Test
    void botSenderMessageIsIgnored() {
        long chatId = uniqueId();
        int sentBefore = fakeTelegramGateway.sentMessages().size();
        String body = TelegramUpdateTestSupport.botSenderMessageUpdate(uniqueId(), chatId, chatId, "/start");

        ResponseEntity<String> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCount(chatId)).isZero();
        assertThat(fakeTelegramGateway.sentMessages()).hasSize(sentBefore);
    }

    @Test
    void malformedJsonReturns200AndDoesNothing() {
        int sentBefore = fakeTelegramGateway.sentMessages().size();

        ResponseEntity<String> response = post("{not-valid-json");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fakeTelegramGateway.sentMessages()).hasSize(sentBefore);
    }

    @Test
    void appCommandRepliesWithAppButton() {
        long chatId = uniqueId();
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Ali", null, null, "uz", "/app");

        post(body);

        FakeTelegramGateway.SentMessage sent = lastSentTo(chatId);
        assertThat(FakeTelegramGateway.webAppUrlOf(sent)).isEqualTo(miniAppUrl);
    }

    @Test
    void helpCommandRepliesWithPlainText() {
        long chatId = uniqueId();
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Ali", null, null, "uz", "/help");

        post(body);

        FakeTelegramGateway.SentMessage sent = lastSentTo(chatId);
        assertThat(sent.replyMarkup()).isNull();
        assertThat(sent.text()).isNotBlank();
    }

    @Test
    void unknownTextRepliesWithFallback() {
        long chatId = uniqueId();
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                uniqueId(), chatId, chatId, "Ali", null, null, "uz", "25000 tushlik");

        post(body);

        FakeTelegramGateway.SentMessage sent = lastSentTo(chatId);
        assertThat(sent.text()).isNotBlank();
        assertThat(sent.replyMarkup()).isNull();
    }

    private ResponseEntity<String> post(String body) {
        return restTemplate.exchange(
                RequestEntity.post("/telegram/webhook")
                        .header(SECRET_HEADER, webhookSecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body),
                String.class);
    }

    private FakeTelegramGateway.SentMessage lastSentTo(long chatId) {
        return fakeTelegramGateway.sentMessages().stream()
                .filter(m -> m.chatId() == chatId)
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private int countSentTo(long chatId) {
        return (int) fakeTelegramGateway.sentMessages().stream().filter(m -> m.chatId() == chatId).count();
    }

    private int userCount(long telegramId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from users where telegram_id = ?", Integer.class, telegramId);
    }

    private long uniqueId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, 999_999_999);
    }

}
