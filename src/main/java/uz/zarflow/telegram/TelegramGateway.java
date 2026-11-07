package uz.zarflow.telegram;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

interface TelegramGateway {

    void sendMessage(long chatId, String text, InlineKeyboardMarkup replyMarkup);

    void editMessageText(long chatId, int messageId, String text, InlineKeyboardMarkup replyMarkup);

    void answerCallbackQuery(String callbackQueryId);

    void setWebhook(String url, String secretToken, List<String> allowedUpdates, boolean dropPendingUpdates);

    void setChatMenuButton(String text, String webAppUrl);

    void setMyCommands(List<BotCommand> commands, String languageCode);

}
