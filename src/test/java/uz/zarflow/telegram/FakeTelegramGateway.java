package uz.zarflow.telegram;

import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

class FakeTelegramGateway implements TelegramGateway {

    private final List<SentMessage> sentMessages = new ArrayList<>();
    private final List<EditMessageCall> editMessageCalls = new ArrayList<>();
    private final List<String> answeredCallbackQueryIds = new ArrayList<>();
    private final List<SetWebhookCall> setWebhookCalls = new ArrayList<>();
    private final List<SetChatMenuButtonCall> setChatMenuButtonCalls = new ArrayList<>();
    private final List<SetMyCommandsCall> setMyCommandsCalls = new ArrayList<>();
    private final List<SetMyNameCall> setMyNameCalls = new ArrayList<>();
    private final List<SetMyShortDescriptionCall> setMyShortDescriptionCalls = new ArrayList<>();
    private final List<SetMyDescriptionCall> setMyDescriptionCalls = new ArrayList<>();

    @Override
    public void sendMessage(long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        sentMessages.add(new SentMessage(chatId, text, replyMarkup));
    }

    @Override
    public void editMessageText(long chatId, int messageId, String text, InlineKeyboardMarkup replyMarkup) {
        editMessageCalls.add(new EditMessageCall(chatId, messageId, text, replyMarkup));
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        answeredCallbackQueryIds.add(callbackQueryId);
    }

    @Override
    public void setWebhook(String url, String secretToken, List<String> allowedUpdates, boolean dropPendingUpdates) {
        setWebhookCalls.add(new SetWebhookCall(url, secretToken, allowedUpdates, dropPendingUpdates));
    }

    @Override
    public void setChatMenuButton(String text, String webAppUrl) {
        setChatMenuButtonCalls.add(new SetChatMenuButtonCall(text, webAppUrl));
    }

    @Override
    public void setMyCommands(List<BotCommand> commands, String languageCode) {
        setMyCommandsCalls.add(new SetMyCommandsCall(commands, languageCode));
    }

    @Override
    public void setMyName(String name, String languageCode) {
        setMyNameCalls.add(new SetMyNameCall(name, languageCode));
    }

    @Override
    public void setMyShortDescription(String shortDescription, String languageCode) {
        setMyShortDescriptionCalls.add(new SetMyShortDescriptionCall(shortDescription, languageCode));
    }

    @Override
    public void setMyDescription(String description, String languageCode) {
        setMyDescriptionCalls.add(new SetMyDescriptionCall(description, languageCode));
    }

    List<SentMessage> sentMessages() {
        return List.copyOf(sentMessages);
    }

    List<EditMessageCall> editMessageCalls() {
        return List.copyOf(editMessageCalls);
    }

    List<String> answeredCallbackQueryIds() {
        return List.copyOf(answeredCallbackQueryIds);
    }

    List<SetWebhookCall> setWebhookCalls() {
        return List.copyOf(setWebhookCalls);
    }

    List<SetChatMenuButtonCall> setChatMenuButtonCalls() {
        return List.copyOf(setChatMenuButtonCalls);
    }

    List<SetMyCommandsCall> setMyCommandsCalls() {
        return List.copyOf(setMyCommandsCalls);
    }

    List<SetMyNameCall> setMyNameCalls() {
        return List.copyOf(setMyNameCalls);
    }

    List<SetMyShortDescriptionCall> setMyShortDescriptionCalls() {
        return List.copyOf(setMyShortDescriptionCalls);
    }

    List<SetMyDescriptionCall> setMyDescriptionCalls() {
        return List.copyOf(setMyDescriptionCalls);
    }

    static String webAppUrlOf(SentMessage message) {
        if (message.replyMarkup() == null) {
            return null;
        }
        return message.replyMarkup().getKeyboard().stream()
                .flatMap(List::stream)
                .filter(button -> button.getWebApp() != null)
                .map(button -> button.getWebApp().getUrl())
                .findFirst()
                .orElse(null);
    }

    record SentMessage(long chatId, String text, InlineKeyboardMarkup replyMarkup) {
    }

    record EditMessageCall(long chatId, int messageId, String text, InlineKeyboardMarkup replyMarkup) {
    }

    record SetWebhookCall(String url, String secretToken, List<String> allowedUpdates, boolean dropPendingUpdates) {
    }

    record SetChatMenuButtonCall(String text, String webAppUrl) {
    }

    record SetMyCommandsCall(List<BotCommand> commands, String languageCode) {
    }

    record SetMyNameCall(String name, String languageCode) {
    }

    record SetMyShortDescriptionCall(String shortDescription, String languageCode) {
    }

    record SetMyDescriptionCall(String description, String languageCode) {
    }

}
