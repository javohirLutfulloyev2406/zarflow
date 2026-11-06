package uz.zarflow.telegram;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import uz.zarflow.user.UserAccounts;

@Service
class TelegramUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateService.class);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final JdbcTemplate jdbcTemplate;
    private final UserAccounts userAccounts;
    private final TelegramGateway telegramGateway;
    private final MessageSource messageSource;
    private final TelegramBotProperties properties;

    TelegramUpdateService(
            JdbcTemplate jdbcTemplate,
            UserAccounts userAccounts,
            TelegramGateway telegramGateway,
            MessageSource messageSource,
            TelegramBotProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccounts = userAccounts;
        this.telegramGateway = telegramGateway;
        this.messageSource = messageSource;
        this.properties = properties;
    }

    void handle(Update update) {
        if (update.hasCallbackQuery()) {
            log.debug("Ignoring callback_query update {}", update.getUpdateId());
            return;
        }
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        Chat chat = message.getChat();
        User from = message.getFrom();
        if (chat == null || !Boolean.TRUE.equals(chat.isUserChat())) {
            return;
        }
        if (from == null || Boolean.TRUE.equals(from.getIsBot())) {
            return;
        }
        if (!claimUpdate(update.getUpdateId())) {
            log.debug("Duplicate update_id {}, skipping", update.getUpdateId());
            return;
        }

        UserAccounts.LoginResult loginResult = userAccounts.registerOrUpdate(new UserAccounts.TelegramProfile(
                from.getId(), from.getFirstName(), from.getLastName(), from.getUserName(), from.getLanguageCode()));

        Locale locale = Locale.of(loginResult.language());
        String command = extractCommand(message.getText());

        switch (command) {
            case "/start" -> replyWithAppButton(chat.getId(), locale, "bot.start", loginResult.firstName());
            case "/app" -> replyWithAppButton(chat.getId(), locale, "bot.app");
            case "/help" -> reply(chat.getId(), locale, "bot.help");
            default -> reply(chat.getId(), locale, "bot.fallback");
        }
    }

    private boolean claimUpdate(Integer updateId) {
        if (updateId == null) {
            return true;
        }
        int rows = jdbcTemplate.update(
                "insert into telegram_updates(update_id) values (?) on conflict do nothing", updateId.longValue());
        return rows > 0;
    }

    private String extractCommand(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String firstToken = WHITESPACE.split(text.trim(), 2)[0];
        int at = firstToken.indexOf('@');
        return at >= 0 ? firstToken.substring(0, at) : firstToken;
    }

    private void replyWithAppButton(Long chatId, Locale locale, String messageKey, Object... args) {
        String text = messageSource.getMessage(messageKey, args, locale);
        String buttonText = messageSource.getMessage("bot.open-app-button", null, locale);
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(buttonText)
                .webApp(new WebAppInfo(properties.miniAppUrl()))
                .build();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(button)));
        sendSafely(chatId, text, markup, messageKey);
    }

    private void reply(Long chatId, Locale locale, String messageKey) {
        String text = messageSource.getMessage(messageKey, null, locale);
        sendSafely(chatId, text, null, messageKey);
    }

    private void sendSafely(Long chatId, String text, InlineKeyboardMarkup markup, String messageKey) {
        try {
            telegramGateway.sendMessage(chatId, text, markup);
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send bot reply: key={}", messageKey);
        }
    }

}
