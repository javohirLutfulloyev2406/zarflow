package uz.zarflow.telegram;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

@Component
@ConditionalOnProperty(prefix = "zarflow.telegram", name = "register-webhook", havingValue = "true")
class TelegramBotStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotStartupRunner.class);

    private static final List<BotCommand> COMMANDS_UZ = List.of(
            new BotCommand("start", "Botni ishga tushirish"),
            new BotCommand("today", "Bugungi kirim-chiqim"),
            new BotCommand("week", "Haftalik hisobot"),
            new BotCommand("month", "Oylik hisobot"),
            new BotCommand("balance", "Hisoblar balansi"),
            new BotCommand("undo", "Oxirgi yozuvni bekor qilish"),
            new BotCommand("app", "ZarFlow ilovasini ochish"),
            new BotCommand("help", "Yordam"));

    private static final List<BotCommand> COMMANDS_RU = List.of(
            new BotCommand("start", "Запустить бота"),
            new BotCommand("today", "Доход и расход за сегодня"),
            new BotCommand("week", "Отчёт за неделю"),
            new BotCommand("month", "Отчёт за месяц"),
            new BotCommand("balance", "Баланс по счетам"),
            new BotCommand("undo", "Отменить последнюю запись"),
            new BotCommand("app", "Открыть приложение ZarFlow"),
            new BotCommand("help", "Помощь"));

    private final TelegramGateway gateway;
    private final TelegramBotProperties properties;

    TelegramBotStartupRunner(TelegramGateway gateway, TelegramBotProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Off the startup critical path: these are blocking Telegram API calls, and
        // ApplicationRunner.run() otherwise delays "Started Application" (and any
        // deployment tooling waiting on it) until all four complete or time out.
        Thread.ofVirtual().start(this::registerWithTelegram);
    }

    void registerWithTelegram() {
        tryRun("setWebhook", () -> gateway.setWebhook(
                webhookUrl() + TelegramWebhookController.WEBHOOK_PATH,
                properties.webhookSecret(),
                List.of("message", "callback_query"),
                false));
        tryRun("setChatMenuButton", () -> gateway.setChatMenuButton("ZarFlow", properties.miniAppUrl()));
        tryRun("setMyCommands(uz/default)", () -> gateway.setMyCommands(COMMANDS_UZ, null));
        tryRun("setMyCommands(ru)", () -> gateway.setMyCommands(COMMANDS_RU, "ru"));
    }

    private String webhookUrl() {
        String url = properties.webhookUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void tryRun(String step, Runnable action) {
        try {
            action.run();
        } catch (TelegramGatewayException ex) {
            log.error("Telegram bot startup step failed: {}", step);
        }
    }

}
