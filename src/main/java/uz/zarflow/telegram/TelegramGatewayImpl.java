package uz.zarflow.telegram;

import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.description.SetMyDescription;
import org.telegram.telegrambots.meta.api.methods.description.SetMyShortDescription;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.name.SetMyName;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
class TelegramGatewayImpl implements TelegramGateway {

    private static final Logger log = LoggerFactory.getLogger(TelegramGatewayImpl.class);

    private final TelegramClient telegramClient;

    TelegramGatewayImpl(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void sendMessage(long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(replyMarkup)
                .build();
        execute(message, "sendMessage");
    }

    @Override
    public void editMessageText(long chatId, int messageId, String text, InlineKeyboardMarkup replyMarkup) {
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(text)
                .replyMarkup(replyMarkup)
                .build();
        execute(message, "editMessageText");
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        AnswerCallbackQuery method = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .build();
        execute(method, "answerCallbackQuery");
    }

    @Override
    public void setWebhook(String url, String secretToken, List<String> allowedUpdates, boolean dropPendingUpdates) {
        SetWebhook setWebhook = SetWebhook.builder()
                .url(url)
                .secretToken(secretToken)
                .allowedUpdates(allowedUpdates)
                .dropPendingUpdates(dropPendingUpdates)
                .build();
        executeSetWebhook(setWebhook);
    }

    @Override
    public void setChatMenuButton(String text, String webAppUrl) {
        MenuButtonWebApp menuButton = MenuButtonWebApp.builder()
                .text(text)
                .webAppInfo(new WebAppInfo(webAppUrl))
                .build();
        SetChatMenuButton method = SetChatMenuButton.builder()
                .menuButton(menuButton)
                .build();
        execute(method, "setChatMenuButton");
    }

    @Override
    public void setMyCommands(List<BotCommand> commands, String languageCode) {
        SetMyCommands method = SetMyCommands.builder()
                .commands(commands)
                .languageCode(languageCode)
                .build();
        execute(method, "setMyCommands");
    }

    @Override
    public void setMyName(String name, String languageCode) {
        SetMyName method = SetMyName.builder()
                .name(name)
                .languageCode(languageCode)
                .build();
        execute(method, "setMyName");
    }

    @Override
    public void setMyShortDescription(String shortDescription, String languageCode) {
        SetMyShortDescription method = SetMyShortDescription.builder()
                .shortDescription(shortDescription)
                .languageCode(languageCode)
                .build();
        execute(method, "setMyShortDescription");
    }

    @Override
    public void setMyDescription(String description, String languageCode) {
        SetMyDescription method = SetMyDescription.builder()
                .description(description)
                .languageCode(languageCode)
                .build();
        execute(method, "setMyDescription");
    }

    private <T extends Serializable, M extends BotApiMethod<T>> void execute(M method, String methodName) {
        call(methodName, () -> telegramClient.execute(method));
    }

    /** SetWebhook extends PartialBotApiMethod directly, not BotApiMethod, so it needs its own client overload. */
    private void executeSetWebhook(SetWebhook method) {
        call("setWebhook", () -> telegramClient.execute(method));
    }

    private void call(String methodName, TelegramCall call) {
        try {
            call.run();
        } catch (TelegramApiException ex) {
            log.error("Telegram API call failed: method={} exceptionType={}", methodName, ex.getClass().getSimpleName());
            throw new TelegramGatewayException(methodName);
        }
    }

    @FunctionalInterface
    private interface TelegramCall {
        void run() throws TelegramApiException;
    }

}
