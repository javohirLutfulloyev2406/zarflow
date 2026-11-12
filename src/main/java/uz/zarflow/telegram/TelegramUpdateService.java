package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.messageorigin.MessageOrigin;
import org.telegram.telegrambots.meta.api.objects.messageorigin.MessageOriginChannel;
import org.telegram.telegrambots.meta.api.objects.messageorigin.MessageOriginChat;
import org.telegram.telegrambots.meta.api.objects.messageorigin.MessageOriginHiddenUser;
import org.telegram.telegrambots.meta.api.objects.messageorigin.MessageOriginUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import uz.zarflow.account.AccountType;
import uz.zarflow.account.Accounts;
import uz.zarflow.budget.Budgets;
import uz.zarflow.category.CategoryType;
import uz.zarflow.category.Categories;
import uz.zarflow.debt.DebtStatus;
import uz.zarflow.debt.Debts;
import uz.zarflow.habit.Habits;
import uz.zarflow.humo.HumoFragmentDetector;
import uz.zarflow.humo.HumoImport;
import uz.zarflow.humo.HumoReconciliation;
import uz.zarflow.recurring.Recurring;
import uz.zarflow.savings.Savings;
import uz.zarflow.shared.common.DateRange;
import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.common.MoneyFormatter;
import uz.zarflow.shared.common.MonthNames;
import uz.zarflow.shared.common.Percentages;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserAccounts;
import uz.zarflow.user.UserProfiles;

@Service
class TelegramUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateService.class);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int CATEGORY_PAGE_SIZE = 6;

    private final JdbcTemplate jdbcTemplate;
    private final UserAccounts userAccounts;
    private final UserProfiles userProfiles;
    private final Accounts accounts;
    private final Categories categories;
    private final Transactions transactions;
    private final Budgets budgets;
    private final Recurring recurring;
    private final Debts debts;
    private final Habits habits;
    private final Savings savings;
    private final HumoReconciliation humoReconciliation;
    private final HumoImport humoImport;
    private final HumoFragmentBuffer humoFragmentBuffer;
    private final UserParsingMemoryRepository parsingMemoryRepository;
    private final TelegramPendingEntryRepository pendingEntryRepository;
    private final TelegramGateway telegramGateway;
    private final MessageSource messageSource;
    private final MoneyFormatter moneyFormatter;
    private final TelegramBotProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final QuickInputParser parser = new QuickInputParser();

    TelegramUpdateService(
            JdbcTemplate jdbcTemplate,
            UserAccounts userAccounts,
            UserProfiles userProfiles,
            Accounts accounts,
            Categories categories,
            Transactions transactions,
            Budgets budgets,
            Recurring recurring,
            Debts debts,
            Habits habits,
            Savings savings,
            HumoReconciliation humoReconciliation,
            HumoImport humoImport,
            HumoFragmentBuffer humoFragmentBuffer,
            UserParsingMemoryRepository parsingMemoryRepository,
            TelegramPendingEntryRepository pendingEntryRepository,
            TelegramGateway telegramGateway,
            MessageSource messageSource,
            MoneyFormatter moneyFormatter,
            TelegramBotProperties properties,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccounts = userAccounts;
        this.userProfiles = userProfiles;
        this.accounts = accounts;
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
        this.recurring = recurring;
        this.debts = debts;
        this.habits = habits;
        this.savings = savings;
        this.humoReconciliation = humoReconciliation;
        this.humoImport = humoImport;
        this.humoFragmentBuffer = humoFragmentBuffer;
        this.parsingMemoryRepository = parsingMemoryRepository;
        this.pendingEntryRepository = pendingEntryRepository;
        this.telegramGateway = telegramGateway;
        this.messageSource = messageSource;
        this.moneyFormatter = moneyFormatter;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    void handle(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
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
        // Claiming update_id and registering the user must commit or roll back together:
        // if registerOrUpdate throws, the claim must not survive on its own, or this
        // update_id would be permanently marked "processed" despite never actually
        // being handled, with no way to reprocess it later.
        UserAccounts.LoginResult loginResult = transactionTemplate.execute(status -> {
            if (!claimUpdate(update.getUpdateId())) {
                return null;
            }
            return userAccounts.registerOrUpdate(new UserAccounts.TelegramProfile(
                    from.getId(), from.getFirstName(), from.getLastName(), from.getUserName(),
                    from.getLanguageCode()));
        });
        if (loginResult == null) {
            log.debug("Duplicate update_id {}, skipping", update.getUpdateId());
            return;
        }

        Locale locale = Locale.of(loginResult.language());
        UUID userId = loginResult.userId();
        long chatId = chat.getId();
        String command = extractCommand(message.getText());

        switch (command) {
            case "/start" -> replyWithAppButton(chatId, locale, "bot.start", loginResult.firstName());
            case "/app" -> replyWithAppButton(chatId, locale, "bot.app");
            case "/help" -> reply(chatId, locale, "bot.help");
            case "/today" -> runOnboarded(chatId, userId, locale, () -> handleToday(chatId, userId, locale));
            case "/balance" -> runOnboarded(chatId, userId, locale, () -> handleBalance(chatId, userId, locale));
            case "/undo" -> runOnboarded(chatId, userId, locale, () -> handleUndo(chatId, userId, locale));
            case "/week" -> runOnboarded(chatId, userId, locale, () -> handleWeek(chatId, userId, locale));
            case "/month" -> runOnboarded(chatId, userId, locale, () -> handleMonth(chatId, userId, locale));
            case "/debt" -> runOnboarded(chatId, userId, locale, () -> handleDebtList(chatId, userId, locale));
            default -> runOnboarded(chatId, userId, locale,
                    () -> handleTextOrHumoBalance(message, update.getUpdateId(), chatId, userId, locale));
        }
    }

    // ---- Humo balance message reconciliation (docs/PRD.md §7) ----

    // Content format is the gate here, not the forwarder's identity -- pasted text has no
    // forward_origin to check anyway, and every write below is scoped to this userId, so
    // fabricated input only ever corrupts the sender's own ledger (ADR 0004).
    private void handleTextOrHumoBalance(Message message, Integer updateId, long chatId, UUID userId, Locale locale) {
        Instant forwardDate = resolveForwardDate(message);
        boolean wasForwarded = forwardDate != null;
        Instant asOf = wasForwarded ? forwardDate : Instant.now(clock);
        String text = message.getText();
        if (tryHumo(text, asOf, wasForwarded, userId, locale, chatId)) {
            return;
        }
        // @HUMOcardbot forwards routinely split one notification across several
        // consecutive messages (one per line) -- a lone line like "🕓 12:00 12.11.2026"
        // won't parse above, but handing it straight to quick-input would misread it as
        // a manual entry (ADR 0005). Buffer it a moment for its neighbors instead.
        if (HumoFragmentDetector.looksLikeFragment(text)) {
            humoFragmentBuffer.offer(chatId, userId, locale, text, wasForwarded ? forwardDate : null);
            return;
        }
        handleQuickInput(text, updateId, chatId, userId, locale);
    }

    /** @return true if the balance or import parser recognized {@code text} and sent its result. */
    private boolean tryHumo(String text, Instant asOf, boolean wasForwarded, UUID userId, Locale locale, long chatId) {
        HumoReconciliation.ProcessResult balanceResult = humoReconciliation.process(userId, text, asOf, wasForwarded);
        HumoImport.ProcessResult importResult = humoImport.process(userId, text, categoryMemoryLookup(userId));
        if (!balanceResult.parsed() && !importResult.parsed()) {
            return false;
        }
        if (balanceResult.parsed()) {
            sendHumoResult(chatId, locale, balanceResult, wasForwarded);
        }
        if (importResult.parsed()) {
            sendHumoImportResult(userId, locale, chatId, importResult);
        }
        return true;
    }

    /** Called by {@link HumoFragmentScheduler} once a chat's buffered lines have gone
     * quiet -- merges them back into one re-parse attempt, falling back to quick-input
     * (one entry per line, today's pre-buffering behavior) if they still don't add up to
     * a recognizable HUMO block. */
    void processBufferedHumoFragments(HumoFragmentBuffer.ReadyBatch batch) {
        String merged = String.join("\n", batch.lines());
        Instant asOf = batch.forwardDate() != null ? batch.forwardDate() : Instant.now(clock);
        if (tryHumo(merged, asOf, batch.forwardDate() != null, batch.userId(), batch.locale(), batch.chatId())) {
            return;
        }
        handleQuickInput(merged, null, batch.chatId(), batch.userId(), batch.locale());
    }

    private Instant resolveForwardDate(Message message) {
        Integer epochSeconds = switch (message.getForwardOrigin()) {
            case null -> null;
            case MessageOriginUser u -> u.getDate();
            case MessageOriginHiddenUser u -> u.getDate();
            case MessageOriginChat c -> c.getDate();
            case MessageOriginChannel c -> c.getDate();
            default -> null;
        };
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }

    private void sendHumoResult(long chatId, Locale locale, HumoReconciliation.ProcessResult result, boolean wasForwarded) {
        if (result.duplicate()) {
            reply(chatId, locale, "bot.humo-balance-duplicate");
            return;
        }
        StringBuilder summary = new StringBuilder();
        for (var card : result.cards()) {
            if (card.kind() == HumoReconciliation.Kind.MATCHED) {
                summary.append(messageSource.getMessage("bot.humo-balance-matched", new Object[]{card.cardLast4()}, locale))
                        .append('\n');
            }
        }
        if (result.emptyOrIgnoredCount() > 0) {
            summary.append(messageSource.getMessage(
                    "bot.humo-balance-empty-ignored", new Object[]{result.emptyOrIgnoredCount()}, locale)).append('\n');
        }
        if (!wasForwarded) {
            summary.append(messageSource.getMessage("bot.humo-pasted-now-note", null, locale)).append('\n');
        }
        if (!summary.isEmpty()) {
            sendSafely(chatId, summary.toString().strip(), null, "bot.humo-balance-matched");
        }
        for (var card : result.cards()) {
            if (card.kind() == HumoReconciliation.Kind.DIFF) {
                sendHumoDiff(chatId, locale, card.cardLast4(), card.bankBalance(), card.zarflowBalance(), card.diff(),
                        card.draftId(), card.showHint());
            } else if (card.kind() == HumoReconciliation.Kind.UNLINKED) {
                sendHumoUnlinked(chatId, locale, card.cardLast4(), card.bankBalance(), card.draftId());
            }
        }
    }

    private void sendHumoDiff(
            long chatId, Locale locale, String cardLast4, BigDecimal bankBalance, BigDecimal zarflowBalance,
            BigDecimal diff, UUID draftId, boolean showHint) {
        String bank = moneyFormatter.formatBare(Money.of(bankBalance), locale);
        String zarflow = moneyFormatter.formatBare(Money.of(zarflowBalance), locale);
        String text = messageSource.getMessage(
                "bot.humo-balance-diff", new Object[]{cardLast4, bank, zarflow, formatSignedBare(diff, locale)}, locale);
        if (showHint) {
            text += "\n" + messageSource.getMessage("bot.humo-tuzatish-hint", null, locale);
        }
        sendSafely(chatId, text, humoCorrectOrSkipButtons(locale, draftId), "bot.humo-balance-diff");
    }

    private void sendHumoUnlinked(long chatId, Locale locale, String cardLast4, BigDecimal bankBalance, UUID draftId) {
        String bank = moneyFormatter.formatBare(Money.of(bankBalance), locale);
        String text = messageSource.getMessage("bot.humo-balance-unlinked", new Object[]{cardLast4, bank}, locale);
        InlineKeyboardButton link = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-button-link", null, locale))
                .callbackData("G" + draftId)
                .build();
        InlineKeyboardButton ignore = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-button-ignore", null, locale))
                .callbackData("I" + draftId)
                .build();
        sendSafely(chatId, text, new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(link, ignore))), "bot.humo-balance-unlinked");
    }

    private InlineKeyboardMarkup humoCorrectOrSkipButtons(Locale locale, UUID draftId) {
        InlineKeyboardButton correct = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-button-correct", null, locale))
                .callbackData("A" + draftId)
                .build();
        InlineKeyboardButton skip = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.recurring-button-skip", null, locale))
                .callbackData("F" + draftId)
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(correct, skip)));
    }

    private String formatSignedBare(BigDecimal diff, Locale locale) {
        String magnitude = moneyFormatter.formatBare(Money.of(diff.abs()), locale);
        return (diff.signum() < 0 ? "−" : "+") + magnitude;
    }

    private void handleHumoCorrect(UUID userId, Locale locale, long chatId, int messageId, UUID draftId) {
        Optional<HumoReconciliation.CorrectResult> result = humoReconciliation.correct(userId, draftId);
        if (result.isEmpty()) {
            return;
        }
        String text = result.get().type() == null
                ? messageSource.getMessage("bot.humo-already-matched", null, locale)
                : messageSource.getMessage(
                        "bot.humo-corrected", new Object[]{moneyFormatter.format(Money.of(result.get().amount()), locale)}, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleHumoSkip(UUID userId, Locale locale, long chatId, int messageId, UUID draftId) {
        humoReconciliation.skip(userId, draftId);
        telegramGateway.editMessageText(chatId, messageId, messageSource.getMessage("bot.humo-skipped", null, locale), null);
    }

    private void handleHumoIgnore(UUID userId, Locale locale, long chatId, int messageId, UUID draftId) {
        humoReconciliation.ignore(userId, draftId);
        telegramGateway.editMessageText(chatId, messageId, messageSource.getMessage("bot.humo-ignored", null, locale), null);
    }

    private void handleHumoChooseAccount(UUID userId, Locale locale, long chatId, int messageId, UUID draftId) {
        List<Accounts.AccountView> userAccountViews = accounts.listForUser(userId);
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < userAccountViews.size(); i++) {
            rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
                    .text(userAccountViews.get(i).name())
                    .callbackData("H" + draftId + "." + i)
                    .build()));
        }
        String text = messageSource.getMessage("bot.humo-choose-account", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, new InlineKeyboardMarkup(rows));
    }

    private void handleHumoLinkPick(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        IdAndInt ref = parseIdAndInt(payload);
        List<Accounts.AccountView> userAccountViews = accounts.listForUser(userId);
        if (ref.value() < 0 || ref.value() >= userAccountViews.size()) {
            return;
        }
        UUID accountId = userAccountViews.get(ref.value()).id();
        Optional<HumoReconciliation.LinkResult> result = humoReconciliation.link(userId, ref.id(), accountId);
        if (result.isEmpty()) {
            return;
        }
        if (result.get().matched()) {
            telegramGateway.editMessageText(
                    chatId, messageId, messageSource.getMessage("bot.humo-linked-matched", null, locale), null);
            return;
        }
        String text = messageSource.getMessage(
                "bot.humo-linked-diff", new Object[]{formatSignedBare(result.get().diff(), locale)}, locale);
        if (result.get().showHint()) {
            text += "\n" + messageSource.getMessage("bot.humo-tuzatish-hint", null, locale);
        }
        telegramGateway.editMessageText(chatId, messageId, text, humoCorrectOrSkipButtons(locale, result.get().draftId()));
    }

    // ---- onboarding gate ----

    private void runOnboarded(long chatId, UUID userId, Locale locale, Runnable action) {
        if (!userProfiles.getProfile(userId).onboardingCompleted()) {
            replyWithAppButton(chatId, locale, "bot.onboarding-required");
            return;
        }
        action.run();
    }

    // ---- quick input (docs/PRD.md §5) ----

    private void handleQuickInput(String text, Integer updateId, long chatId, UUID userId, Locale locale) {
        if (text == null) {
            return;
        }
        List<String> lines = Arrays.stream(text.split("\\R")).map(String::trim).filter(s -> !s.isBlank()).toList();

        List<Accounts.AccountView> userAccountViews = null;
        List<QuickInputParser.CategoryKeyword> parserCategories = null;
        Map<String, UUID> memory = null;
        boolean anyHandled = false;
        int lineIndex = 0;

        for (String line : lines) {
            lineIndex++;
            if (mentionsCredit(line)) {
                anyHandled = true;
                handleDebtQuickInput(line, chatId, userId, locale);
                continue;
            }
            if (parserCategories == null) {
                userAccountViews = accounts.listForUser(userId);
                parserCategories = categories.keywordsForUser(userId).stream()
                        .map(c -> new QuickInputParser.CategoryKeyword(c.id(), c.type(), c.keywords()))
                        .toList();
                memory = parsingMemoryRepository.findAllByUserId(userId).stream()
                        .collect(Collectors.toMap(UserParsingMemory::getPhrase, UserParsingMemory::getCategoryId,
                                (first, second) -> second));
            }
            Optional<QuickInputParser.ParsedLine> parsed = parser.parseLine(line, parserCategories, memory);
            if (parsed.isEmpty()) {
                continue;
            }
            anyHandled = true;
            handleParsedLine(parsed.get(), chatId, userId, locale, userAccountViews, "tg:" + updateId + ":" + lineIndex);
        }

        if (!anyHandled) {
            reply(chatId, locale, "bot.quick-input-no-amount");
        }
    }

    private boolean mentionsCredit(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("kredit") || normalized.contains("кредит");
    }

    // ---- Humo transaction import (docs/PRD.md §1-5) ----

    private Function<String, UUID> categoryMemoryLookup(UUID userId) {
        return merchantKey -> parsingMemoryRepository.findByUserIdAndPhrase(userId, merchantKey)
                .map(UserParsingMemory::getCategoryId)
                .orElse(null);
    }

    private void rememberMerchantCategory(UUID userId, String merchantKey, UUID categoryId) {
        UserParsingMemory entry = parsingMemoryRepository.findByUserIdAndPhrase(userId, merchantKey)
                .orElseGet(() -> new UserParsingMemory(userId, merchantKey, categoryId));
        entry.setCategoryId(categoryId);
        parsingMemoryRepository.save(entry);
    }

    private void sendHumoImportResult(UUID userId, Locale locale, long chatId, HumoImport.ProcessResult result) {
        String summaryKey = result.pending().isEmpty() ? "bot.humo-import-summary-no-questions" : "bot.humo-import-summary";
        Object[] args = result.pending().isEmpty()
                ? new Object[]{result.addedCount(), result.duplicateCount()}
                : new Object[]{result.addedCount(), result.duplicateCount(), result.pending().size()};
        StringBuilder summary = new StringBuilder(messageSource.getMessage(summaryKey, args, locale));
        if (result.skippedNonUzsCount() > 0) {
            summary.append('\n').append(messageSource.getMessage(
                    "bot.humo-import-skipped-non-uzs", new Object[]{result.skippedNonUzsCount()}, locale));
        }
        sendSafely(chatId, summary.toString(), null, "bot.humo-import-summary");
        for (HumoImport.Outcome outcome : result.pending()) {
            sendHumoImportOutcome(userId, locale, chatId, outcome);
        }
    }

    private void sendHumoImportOutcome(UUID userId, Locale locale, long chatId, HumoImport.Outcome outcome) {
        switch (outcome) {
            case HumoImport.Outcome.NeedsCardLink o -> sendSafely(
                    chatId, humoImportAccountAskText(o.cardLast4(), o.type(), o.amount(), o.merchantRaw(), locale),
                    humoImportAccountButtons(userId, locale, "Q" + o.draftId()), "bot.humo-import-ask-account");
            case HumoImport.Outcome.NeedsP2pChoice o -> sendSafely(
                    chatId, humoImportP2pAskText(o, locale), humoImportP2pButtons(locale, o.draftId()), "bot.humo-import-ask-p2p");
            case HumoImport.Outcome.NeedsP2pAccount o -> sendSafely(
                    chatId, messageSource.getMessage("bot.humo-import-ask-p2p-account", null, locale),
                    humoImportAccountButtons(userId, locale, "Q" + o.draftId()), "bot.humo-import-ask-p2p-account");
            case HumoImport.Outcome.NeedsLoanPick o -> sendSafely(
                    chatId, humoImportLoanAskText(o, locale), humoImportLoanButtons(userId, locale, o.draftId()), "bot.humo-import-ask-debt");
            case HumoImport.Outcome.NeedsCategory o -> sendSafely(
                    chatId, messageSource.getMessage("bot.quick-input-ask-category", null, locale),
                    buildCategoryKeyboard(typedCategories(userId, o.type()), locale, "J" + o.draftId(), "P" + o.draftId(), 0),
                    "bot.quick-input-ask-category");
            case HumoImport.Outcome.NeedsDuplicateConfirm o -> sendSafely(
                    chatId, humoImportDuplicateAskText(o, locale), humoImportDuplicateButtons(locale, o.draftId()),
                    "bot.humo-import-ask-duplicate");
            case HumoImport.Outcome.Booked o -> sendSafely(chatId, humoImportBookedText(userId, locale, o), null, "bot.humo-import-booked");
            case HumoImport.Outcome.Skipped o -> reply(chatId, locale, "bot.humo-import-skipped-duplicate");
        }
    }

    private void renderHumoImportOutcome(UUID userId, Locale locale, long chatId, int messageId, HumoImport.Outcome outcome) {
        switch (outcome) {
            case HumoImport.Outcome.NeedsCardLink o -> telegramGateway.editMessageText(
                    chatId, messageId, humoImportAccountAskText(o.cardLast4(), o.type(), o.amount(), o.merchantRaw(), locale),
                    humoImportAccountButtons(userId, locale, "Q" + o.draftId()));
            case HumoImport.Outcome.NeedsP2pChoice o -> telegramGateway.editMessageText(
                    chatId, messageId, humoImportP2pAskText(o, locale), humoImportP2pButtons(locale, o.draftId()));
            case HumoImport.Outcome.NeedsP2pAccount o -> telegramGateway.editMessageText(
                    chatId, messageId, messageSource.getMessage("bot.humo-import-ask-p2p-account", null, locale),
                    humoImportAccountButtons(userId, locale, "Q" + o.draftId()));
            case HumoImport.Outcome.NeedsLoanPick o -> telegramGateway.editMessageText(
                    chatId, messageId, humoImportLoanAskText(o, locale), humoImportLoanButtons(userId, locale, o.draftId()));
            case HumoImport.Outcome.NeedsCategory o -> telegramGateway.editMessageText(
                    chatId, messageId, messageSource.getMessage("bot.quick-input-ask-category", null, locale),
                    buildCategoryKeyboard(typedCategories(userId, o.type()), locale, "J" + o.draftId(), "P" + o.draftId(), 0));
            case HumoImport.Outcome.NeedsDuplicateConfirm o -> telegramGateway.editMessageText(
                    chatId, messageId, humoImportDuplicateAskText(o, locale), humoImportDuplicateButtons(locale, o.draftId()));
            case HumoImport.Outcome.Booked o -> telegramGateway.editMessageText(chatId, messageId, humoImportBookedText(userId, locale, o), null);
            case HumoImport.Outcome.Skipped o -> telegramGateway.editMessageText(
                    chatId, messageId, messageSource.getMessage("bot.humo-import-skipped-duplicate", null, locale), null);
        }
    }

    private String humoImportAccountAskText(String cardLast4, TransactionType type, BigDecimal amount, String merchantRaw, Locale locale) {
        String amountText = humoImportSignedAmount(type, amount, locale);
        return messageSource.getMessage("bot.humo-import-ask-account", new Object[]{cardLast4, amountText, merchantRaw}, locale);
    }

    private String humoImportP2pAskText(HumoImport.Outcome.NeedsP2pChoice o, Locale locale) {
        String amountText = humoImportSignedAmount(o.type(), o.amount(), locale);
        return messageSource.getMessage("bot.humo-import-ask-p2p", new Object[]{o.merchantRaw(), amountText}, locale);
    }

    private String humoImportLoanAskText(HumoImport.Outcome.NeedsLoanPick o, Locale locale) {
        String amountText = moneyFormatter.formatBare(Money.of(o.amount()), locale);
        return messageSource.getMessage("bot.humo-import-ask-debt", new Object[]{o.merchantRaw(), amountText}, locale);
    }

    private String humoImportDuplicateAskText(HumoImport.Outcome.NeedsDuplicateConfirm o, Locale locale) {
        String amountText = humoImportSignedAmount(o.type(), o.amount(), locale);
        return messageSource.getMessage("bot.humo-import-ask-duplicate", new Object[]{o.merchantRaw(), amountText}, locale);
    }

    private String humoImportSignedAmount(TransactionType type, BigDecimal amount, Locale locale) {
        String sign = type == TransactionType.INCOME ? "+" : "-";
        return sign + moneyFormatter.formatBare(Money.of(amount), locale);
    }

    private String humoImportBookedText(UUID userId, Locale locale, HumoImport.Outcome.Booked o) {
        String amountText = moneyFormatter.format(Money.of(o.amount()), locale);
        return switch (o.type()) {
            case TRANSFER -> messageSource.getMessage("bot.humo-import-booked-transfer", new Object[]{amountText}, locale);
            case DEBT_PAYMENT -> messageSource.getMessage("bot.humo-import-booked-debt", new Object[]{amountText}, locale);
            case EXPENSE, INCOME -> humoImportSavedConfirmationText(userId, locale, o.transactionId());
        };
    }

    private String humoImportSavedConfirmationText(UUID userId, Locale locale, UUID transactionId) {
        Transactions.BotTransaction saved = transactions.find(userId, transactionId).orElseThrow();
        String accountName = accounts.listForUser(userId).stream()
                .filter(a -> a.id().equals(saved.accountId())).findFirst().map(Accounts.AccountView::name).orElse("");
        String categoryLabel = categoryLabel(userId, saved.categoryId(), locale);
        String amountText = moneyFormatter.format(Money.of(saved.amount()), locale);
        if (saved.type() == TransactionType.EXPENSE) {
            BigDecimal todayTotal = transactions.summarize(userId, userProfiles.zoneOf(userId)).todayTotal();
            String text = messageSource.getMessage("bot.quick-input-saved-expense",
                    new Object[]{amountText, categoryLabel, accountName, moneyFormatter.format(Money.of(todayTotal), locale)}, locale);
            return appendTodayLeftLine(text, userId, locale);
        }
        return messageSource.getMessage("bot.quick-input-saved-income", new Object[]{amountText, categoryLabel, accountName}, locale);
    }

    private InlineKeyboardMarkup humoImportAccountButtons(UUID userId, Locale locale, String pickPrefix) {
        List<Accounts.AccountView> userAccountViews = accounts.listForUser(userId);
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < userAccountViews.size(); i++) {
            rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
                    .text(userAccountViews.get(i).name())
                    .callbackData(pickPrefix + "." + i)
                    .build()));
        }
        InlineKeyboardButton newAccount = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-import-new-account-button", null, locale))
                .webApp(new WebAppInfo(properties.miniAppUrl()))
                .build();
        rows.add(new InlineKeyboardRow(newAccount));
        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardMarkup humoImportP2pButtons(Locale locale, UUID draftId) {
        InlineKeyboardButton own = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-import-button-own-transfer", null, locale))
                .callbackData("R" + draftId + ".0")
                .build();
        InlineKeyboardButton expense = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-import-button-expense", null, locale))
                .callbackData("R" + draftId + ".1")
                .build();
        InlineKeyboardButton income = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-import-button-income", null, locale))
                .callbackData("R" + draftId + ".2")
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(own), new InlineKeyboardRow(expense, income)));
    }

    private InlineKeyboardMarkup humoImportLoanButtons(UUID userId, Locale locale, UUID draftId) {
        List<Debts.DebtView> active = debts.listForUser(userId).stream().filter(d -> d.status() == DebtStatus.ACTIVE).toList();
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
                    .text(active.get(i).name())
                    .callbackData("S" + draftId + "." + i)
                    .build()));
        }
        rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-import-category-instead-button", null, locale))
                .callbackData("S" + draftId + ".-1")
                .build()));
        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardMarkup humoImportDuplicateButtons(Locale locale, UUID draftId) {
        InlineKeyboardButton notDuplicate = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-import-button-not-duplicate", null, locale))
                .callbackData("V" + draftId + ".0")
                .build();
        InlineKeyboardButton duplicate = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.humo-import-button-duplicate", null, locale))
                .callbackData("V" + draftId + ".1")
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(notDuplicate, duplicate)));
    }

    private void handleHumoImportAccountPick(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        IdAndInt ref = parseIdAndInt(payload);
        List<Accounts.AccountView> userAccountViews = accounts.listForUser(userId);
        if (ref.value() < 0 || ref.value() >= userAccountViews.size()) {
            return;
        }
        UUID accountId = userAccountViews.get(ref.value()).id();
        Optional<HumoImport.Outcome> outcome = humoImport.chooseAccount(userId, ref.id(), accountId, categoryMemoryLookup(userId));
        if (outcome.isEmpty()) {
            outcome = humoImport.chooseP2pAccount(userId, ref.id(), accountId);
        }
        outcome.ifPresent(o -> renderHumoImportOutcome(userId, locale, chatId, messageId, o));
    }

    private void handleHumoImportP2pChoice(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        IdAndInt ref = parseIdAndInt(payload);
        HumoImport.P2pChoice choice = switch (ref.value()) {
            case 0 -> HumoImport.P2pChoice.OWN_TRANSFER;
            case 1 -> HumoImport.P2pChoice.EXPENSE;
            case 2 -> HumoImport.P2pChoice.INCOME;
            default -> null;
        };
        if (choice == null) {
            return;
        }
        humoImport.chooseP2p(userId, ref.id(), choice, categoryMemoryLookup(userId))
                .ifPresent(o -> renderHumoImportOutcome(userId, locale, chatId, messageId, o));
    }

    private void handleHumoImportLoanPick(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        IdAndInt ref = parseIdAndInt(payload);
        UUID debtId = null;
        if (ref.value() >= 0) {
            List<Debts.DebtView> active = debts.listForUser(userId).stream().filter(d -> d.status() == DebtStatus.ACTIVE).toList();
            if (ref.value() >= active.size()) {
                return;
            }
            debtId = active.get(ref.value()).id();
        }
        humoImport.pickDebt(userId, ref.id(), debtId, categoryMemoryLookup(userId))
                .ifPresent(o -> renderHumoImportOutcome(userId, locale, chatId, messageId, o));
    }

    private void handleHumoImportCategoryPick(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        IdAndInt ref = parseIdAndInt(payload);
        Optional<HumoImport.CategoryQuestion> question = humoImport.findCategoryQuestion(userId, ref.id());
        if (question.isEmpty()) {
            return;
        }
        List<Categories.CategoryView> typed = typedCategories(userId, question.get().type());
        if (ref.value() < 0 || ref.value() >= typed.size()) {
            return;
        }
        UUID categoryId = typed.get(ref.value()).id();
        Optional<HumoImport.Outcome> outcome = humoImport.pickCategory(userId, ref.id(), categoryId);
        if (outcome.isEmpty()) {
            return;
        }
        rememberMerchantCategory(userId, question.get().merchantKey(), categoryId);
        renderHumoImportOutcome(userId, locale, chatId, messageId, outcome.get());
    }

    private void handleHumoImportCategoryPage(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        IdAndInt ref = parseIdAndInt(payload);
        Optional<HumoImport.CategoryQuestion> question = humoImport.findCategoryQuestion(userId, ref.id());
        if (question.isEmpty()) {
            return;
        }
        List<Categories.CategoryView> typed = typedCategories(userId, question.get().type());
        InlineKeyboardMarkup markup = buildCategoryKeyboard(typed, locale, "J" + ref.id(), "P" + ref.id(), ref.value());
        telegramGateway.editMessageText(chatId, messageId, messageSource.getMessage("bot.quick-input-ask-category", null, locale), markup);
    }

    private void handleHumoImportDuplicateChoice(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        IdAndInt ref = parseIdAndInt(payload);
        boolean isDuplicate = ref.value() == 1;
        humoImport.confirmDuplicate(userId, ref.id(), isDuplicate)
                .ifPresent(o -> renderHumoImportOutcome(userId, locale, chatId, messageId, o));
    }

    // ---- debt quick input / commands (PRD M8) ----

    private void handleDebtQuickInput(String line, long chatId, UUID userId, Locale locale) {
        Optional<QuickInputParser.ParsedLine> parsed = parser.parseLine(line, List.of(), Map.of());
        if (parsed.isEmpty()) {
            reply(chatId, locale, "bot.quick-input-credit-mention");
            return;
        }
        BigDecimal amount = parsed.get().amount();
        List<Debts.DebtView> active = debts.listForUser(userId).stream()
                .filter(d -> d.status() == DebtStatus.ACTIVE)
                .toList();
        if (active.isEmpty()) {
            reply(chatId, locale, "bot.debt-none-active");
            return;
        }

        if (active.size() == 1) {
            Debts.DebtView debt = active.get(0);
            String text = messageSource.getMessage(
                    "bot.debt-confirm", new Object[]{moneyFormatter.format(Money.of(amount), locale), debt.name()}, locale);
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(messageSource.getMessage("bot.debt-button-confirm", null, locale))
                    .callbackData("D" + debt.id() + ":" + amount.toPlainString())
                    .build();
            sendSafely(chatId, text, new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(button))), "bot.debt-confirm");
            return;
        }

        List<InlineKeyboardRow> rows = active.stream()
                .map(debt -> new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(debt.name())
                        .callbackData("D" + debt.id() + ":" + amount.toPlainString())
                        .build()))
                .toList();
        sendSafely(chatId, messageSource.getMessage("bot.debt-choose", null, locale), new InlineKeyboardMarkup(rows), "bot.debt-choose");
    }

    private void handleDebtPay(UUID userId, Locale locale, long chatId, int messageId, String payload) {
        int separator = payload.indexOf(':');
        UUID debtId = UUID.fromString(payload.substring(0, separator));
        BigDecimal amount = new BigDecimal(payload.substring(separator + 1));

        List<Accounts.AccountView> userAccountViews = accounts.listForUser(userId);
        UUID accountId = defaultAccountId(userAccountViews);
        if (accountId == null) {
            return;
        }
        LocalDate today = LocalDate.now(clock.withZone(userProfiles.zoneOf(userId)));
        Debts.DebtView debt = debts.get(userId, debtId);
        debts.pay(userId, debtId, new Debts.PayInput(amount, today, accountId, false));

        String text = messageSource.getMessage(
                "bot.debt-paid", new Object[]{moneyFormatter.format(Money.of(amount), locale), debt.name()}, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleLeftoverToGoal(UUID userId, Locale locale, long chatId, int messageId, YearMonth month) {
        Savings.LeftoverOfferView offer = savings.resolve(
                userId, month, new Savings.Resolution(Savings.ResolutionType.TO_GOAL, null, null));
        String text = messageSource.getMessage(
                "bot.leftover-to-goal-ack", new Object[]{moneyFormatter.format(Money.of(offer.amount()), locale)}, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleLeftoverToDebt(UUID userId, Locale locale, long chatId, int messageId, YearMonth month) {
        Savings.LeftoverOfferView offer = savings.resolve(
                userId, month, new Savings.Resolution(Savings.ResolutionType.TO_DEBT, null, null));
        String text = messageSource.getMessage(
                "bot.leftover-to-debt-ack", new Object[]{moneyFormatter.format(Money.of(offer.amount()), locale)}, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleLeftoverStayInApp(Locale locale, long chatId, int messageId) {
        String text = messageSource.getMessage("bot.leftover-stay-in-app-ack", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleLeftoverDismiss(UUID userId, Locale locale, long chatId, int messageId, YearMonth month) {
        savings.resolve(userId, month, new Savings.Resolution(Savings.ResolutionType.DISMISSED, null, null));
        String text = messageSource.getMessage("bot.leftover-dismissed-ack", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleDebtList(long chatId, UUID userId, Locale locale) {
        List<Debts.DebtView> active = debts.listForUser(userId).stream()
                .filter(d -> d.status() == DebtStatus.ACTIVE)
                .toList();
        if (active.isEmpty()) {
            reply(chatId, locale, "bot.debt-list-empty");
            return;
        }

        StringBuilder sb = new StringBuilder(messageSource.getMessage("bot.debt-list-header", null, locale));
        for (Debts.DebtView debt : active) {
            Debts.ProgressView progress = debts.progress(userId, debt.id());
            List<Debts.ScheduleRowView> schedule = debts.listSchedule(userId, debt.id());
            Debts.ScheduleRowView nextRow = schedule.stream().filter(r -> !r.paid()).findFirst().orElse(null);
            String nextDate = nextRow == null ? "—" : MonthNames.dayAndMonth(nextRow.dueDate(), locale);
            String nextAmount = nextRow == null ? "—"
                    : moneyFormatter.format(Money.of(nextRow.principal().add(nextRow.interest()).add(nextRow.fee())), locale);
            sb.append('\n').append(messageSource.getMessage("bot.debt-list-line", new Object[]{
                    debt.name(), moneyFormatter.format(Money.of(debt.outstandingPrincipal()), locale),
                    progress.paidPercent(), nextDate, nextAmount}, locale));
        }
        sendSafely(chatId, sb.toString(), null, "bot.debt-list-line");
    }

    private void handleParsedLine(
            QuickInputParser.ParsedLine parsed, long chatId, UUID userId, Locale locale,
            List<Accounts.AccountView> userAccountViews, String idempotencyKey) {
        UUID accountId = resolveAccountId(userAccountViews, parsed.accountHint());
        if (accountId == null) {
            return;
        }
        Instant transactionDate = backdatedInstant(userId, parsed.daysAgo());

        if (parsed.confidence() == QuickInputParser.Confidence.HIGH) {
            var input = new Transactions.BotCreateInput(
                    accountId, parsed.categoryId(), parsed.type(), parsed.amount(), null, parsed.rawLine(),
                    idempotencyKey, transactionDate);
            Transactions.BotTransaction saved = transactions.createFromBot(userId, input);
            sendSavedConfirmation(chatId, locale, userId, saved);
        } else {
            TelegramPendingEntry draft = pendingEntryRepository.save(
                    new TelegramPendingEntry(userId, accountId, parsed.type(), parsed.amount(), parsed.rawLine(), transactionDate));
            sendCategoryPrompt(chatId, locale, userId, parsed.type(), "N" + draft.getId(), "M" + draft.getId());
        }
    }

    /** {@code daysAgo == 0} means "today" -- resolves to {@code null} so downstream creation
     * falls back to its usual "now" default instead of carrying an explicit (and redundant) date. */
    private Instant backdatedInstant(UUID userId, int daysAgo) {
        if (daysAgo == 0) {
            return null;
        }
        ZoneId zone = userProfiles.zoneOf(userId);
        return ZonedDateTime.now(clock.withZone(zone)).minusDays(daysAgo).toInstant();
    }

    private UUID resolveAccountId(List<Accounts.AccountView> userAccountViews, QuickInputParser.AccountHint hint) {
        UUID byHint = switch (hint) {
            case CASH -> findAccountIdByType(userAccountViews, AccountType.CASH);
            case BANK_CARD -> findAccountIdByType(userAccountViews, AccountType.BANK_CARD);
            case DEFAULT -> null;
        };
        return byHint != null ? byHint : defaultAccountId(userAccountViews);
    }

    private UUID findAccountIdByType(List<Accounts.AccountView> userAccountViews, AccountType type) {
        return userAccountViews.stream().filter(a -> a.type() == type).findFirst().map(Accounts.AccountView::id).orElse(null);
    }

    private UUID defaultAccountId(List<Accounts.AccountView> userAccountViews) {
        return userAccountViews.stream().filter(Accounts.AccountView::isDefault).findFirst()
                .or(() -> userAccountViews.stream().findFirst())
                .map(Accounts.AccountView::id)
                .orElse(null);
    }

    private void sendSavedConfirmation(long chatId, Locale locale, UUID userId, Transactions.BotTransaction saved) {
        String accountName = accounts.listForUser(userId).stream()
                .filter(a -> a.id().equals(saved.accountId())).findFirst().map(Accounts.AccountView::name).orElse("");
        String categoryLabel = categoryLabel(userId, saved.categoryId(), locale);
        String amountText = moneyFormatter.format(Money.of(saved.amount()), locale);

        String text;
        if (saved.type() == TransactionType.EXPENSE) {
            BigDecimal todayTotal = transactions.summarize(userId, userProfiles.zoneOf(userId)).todayTotal();
            text = messageSource.getMessage("bot.quick-input-saved-expense",
                    new Object[]{amountText, categoryLabel, accountName, moneyFormatter.format(Money.of(todayTotal), locale)},
                    locale);
            text = appendTodayLeftLine(text, userId, locale);
        } else {
            text = messageSource.getMessage(
                    "bot.quick-input-saved-income", new Object[]{amountText, categoryLabel, accountName}, locale);
        }
        text = appendBackdatedNoteLine(text, userId, locale, saved.transactionDate());
        InlineKeyboardMarkup markup = savedTransactionButtons(locale, saved.id());
        sendSafely(chatId, text, markup, "bot.quick-input-saved-expense");
    }

    /** Quick-input entries are usually logged the moment they happen; when a backdating phrase
     * ("kecha", "3 kun oldin", ...) moved the entry to an earlier day, call that day out explicitly
     * so the confirmation doesn't read as if it landed on today. */
    private String appendBackdatedNoteLine(String text, UUID userId, Locale locale, Instant transactionDate) {
        ZoneId zone = userProfiles.zoneOf(userId);
        LocalDate entryDate = transactionDate.atZone(zone).toLocalDate();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (entryDate.equals(today)) {
            return text;
        }
        String line = messageSource.getMessage(
                "bot.quick-input-backdated-note", new Object[]{MonthNames.dayAndMonth(entryDate, locale)}, locale);
        return text + "\n" + line;
    }

    private String appendTodayLeftLine(String text, UUID userId, Locale locale) {
        return budgets.getToday(userId).map(today -> {
            String line = today.overspentToday()
                    ? messageSource.getMessage("bot.quick-input-today-over",
                            new Object[]{moneyFormatter.format(Money.of(today.overspentAmount()), locale)}, locale)
                    : messageSource.getMessage("bot.quick-input-today-left",
                            new Object[]{moneyFormatter.format(Money.of(today.todayLeft()), locale)}, locale);
            return text + "\n" + line;
        }).orElse(text);
    }

    private InlineKeyboardMarkup savedTransactionButtons(Locale locale, UUID transactionId) {
        InlineKeyboardButton cancel = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.button-cancel", null, locale))
                .callbackData("X" + transactionId)
                .build();
        InlineKeyboardButton category = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.button-category", null, locale))
                .callbackData("K" + transactionId)
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(cancel, category)));
    }

    private void sendCategoryPrompt(long chatId, Locale locale, UUID userId, TransactionType type, String pickPrefix, String pagePrefix) {
        List<Categories.CategoryView> typed = typedCategories(userId, type);
        InlineKeyboardMarkup markup = buildCategoryKeyboard(typed, locale, pickPrefix, pagePrefix, 0);
        String text = messageSource.getMessage("bot.quick-input-ask-category", null, locale);
        sendSafely(chatId, text, markup, "bot.quick-input-ask-category");
    }

    private List<Categories.CategoryView> typedCategories(UUID userId, TransactionType type) {
        CategoryType target = type == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
        return categories.listForUser(userId).stream().filter(c -> c.type() == target).toList();
    }

    private String categoryLabel(UUID userId, UUID categoryId, Locale locale) {
        if (categoryId == null) {
            return "";
        }
        return categories.listForUser(userId).stream()
                .filter(c -> c.id().equals(categoryId)).findFirst()
                .map(c -> isRussian(locale) ? c.nameRu() : c.nameUz())
                .orElse("");
    }

    private boolean isRussian(Locale locale) {
        return "ru".equals(locale.getLanguage());
    }

    private InlineKeyboardMarkup buildCategoryKeyboard(
            List<Categories.CategoryView> typed, Locale locale, String pickPrefix, String pagePrefix, int page) {
        int from = page * CATEGORY_PAGE_SIZE;
        int to = Math.min(from + CATEGORY_PAGE_SIZE, typed.size());
        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<InlineKeyboardButton> current = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Categories.CategoryView c = typed.get(i);
            String label = isRussian(locale) ? c.nameRu() : c.nameUz();
            current.add(InlineKeyboardButton.builder().text(label).callbackData(pickPrefix + "." + i).build());
            if (current.size() == 2) {
                rows.add(new InlineKeyboardRow(current));
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            rows.add(new InlineKeyboardRow(current));
        }
        List<InlineKeyboardButton> nav = new ArrayList<>();
        if (page > 0) {
            nav.add(InlineKeyboardButton.builder().text("⬅️").callbackData(pagePrefix + "." + (page - 1)).build());
        }
        if (to < typed.size()) {
            String label = page == 0 ? messageSource.getMessage("bot.button-other", null, locale) : "➡️";
            nav.add(InlineKeyboardButton.builder().text(label).callbackData(pagePrefix + "." + (page + 1)).build());
        }
        if (!nav.isEmpty()) {
            rows.add(new InlineKeyboardRow(nav));
        }
        return new InlineKeyboardMarkup(rows);
    }

    private String extractPhrase(String rawLine) {
        return parser.parseLine(rawLine, List.of(), Map.of()).map(QuickInputParser.ParsedLine::notePhrase).orElse(null);
    }

    private void rememberCategory(UUID userId, String rawDescription, UUID categoryId) {
        String phrase = extractPhrase(rawDescription);
        if (phrase == null || phrase.isBlank()) {
            return;
        }
        UserParsingMemory entry = parsingMemoryRepository.findByUserIdAndPhrase(userId, phrase)
                .orElseGet(() -> new UserParsingMemory(userId, phrase, categoryId));
        entry.setCategoryId(categoryId);
        parsingMemoryRepository.save(entry);
    }

    // ---- callback queries: category pick/paging, cancel ----

    private void handleCallbackQuery(Update update) {
        var callback = update.getCallbackQuery();
        Boolean claimed = transactionTemplate.execute(status -> claimUpdate(update.getUpdateId()));
        if (callback.getId() != null) {
            try {
                telegramGateway.answerCallbackQuery(callback.getId());
            } catch (TelegramGatewayException ex) {
                log.warn("Failed to answer callback query");
            }
        }
        if (!Boolean.TRUE.equals(claimed)) {
            return;
        }
        User from = callback.getFrom();
        if (from == null || Boolean.TRUE.equals(from.getIsBot())) {
            return;
        }
        var message = callback.getMessage();
        String data = callback.getData();
        if (message == null || data == null || data.isEmpty()) {
            return;
        }

        UserAccounts.LoginResult loginResult = userAccounts.registerOrUpdate(new UserAccounts.TelegramProfile(
                from.getId(), from.getFirstName(), from.getLastName(), from.getUserName(), from.getLanguageCode()));
        Locale locale = Locale.of(loginResult.language());
        UUID userId = loginResult.userId();
        long chatId = message.getChatId();
        int messageId = message.getMessageId();

        char action = data.charAt(0);
        String payload = data.substring(1);
        try {
            switch (action) {
                case 'X' -> handleCancel(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'K' -> handleOpenRecategorize(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'C' -> handlePick(userId, locale, chatId, messageId, payload, true);
                case 'O' -> handlePage(userId, locale, chatId, messageId, payload, true);
                case 'N' -> handlePick(userId, locale, chatId, messageId, payload, false);
                case 'M' -> handlePage(userId, locale, chatId, messageId, payload, false);
                case 'Z' -> handleDailyReportAck(userId, locale, chatId, messageId);
                case 'Y' -> handleRecurringPay(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'U' -> handleRecurringSkip(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'W' -> handleRecurringAck(chatId, messageId, locale);
                case 'D' -> handleDebtPay(userId, locale, chatId, messageId, payload);
                case 'L' -> handleLeftoverToGoal(userId, locale, chatId, messageId, YearMonth.parse(payload));
                case 'E' -> handleLeftoverToDebt(userId, locale, chatId, messageId, YearMonth.parse(payload));
                case 'B' -> handleLeftoverStayInApp(locale, chatId, messageId);
                case 'T' -> handleLeftoverDismiss(userId, locale, chatId, messageId, YearMonth.parse(payload));
                case 'A' -> handleHumoCorrect(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'F' -> handleHumoSkip(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'G' -> handleHumoChooseAccount(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'H' -> handleHumoLinkPick(userId, locale, chatId, messageId, payload);
                case 'I' -> handleHumoIgnore(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'J' -> handleHumoImportCategoryPick(userId, locale, chatId, messageId, payload);
                case 'P' -> handleHumoImportCategoryPage(userId, locale, chatId, messageId, payload);
                case 'Q' -> handleHumoImportAccountPick(userId, locale, chatId, messageId, payload);
                case 'R' -> handleHumoImportP2pChoice(userId, locale, chatId, messageId, payload);
                case 'S' -> handleHumoImportLoanPick(userId, locale, chatId, messageId, payload);
                case 'V' -> handleHumoImportDuplicateChoice(userId, locale, chatId, messageId, payload);
                default -> { }
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Malformed callback data ignored: {}", data);
        }
    }

    private void handleDailyReportAck(UUID userId, Locale locale, long chatId, int messageId) {
        ZoneId zone = userProfiles.zoneOf(userId);
        habits.markNoSpend(userId, LocalDate.now(clock.withZone(zone)));
        String text = messageSource.getMessage("bot.daily-report-ack", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleRecurringPay(UUID userId, Locale locale, long chatId, int messageId, UUID occurrenceId) {
        recurring.pay(userId, occurrenceId);
        String text = messageSource.getMessage("bot.recurring-paid", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleRecurringSkip(UUID userId, Locale locale, long chatId, int messageId, UUID occurrenceId) {
        recurring.skip(userId, occurrenceId);
        String text = messageSource.getMessage("bot.recurring-skipped", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleRecurringAck(long chatId, int messageId, Locale locale) {
        String text = messageSource.getMessage("bot.recurring-ack", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleCancel(UUID userId, Locale locale, long chatId, int messageId, UUID transactionId) {
        if (transactions.find(userId, transactionId).isEmpty()) {
            return;
        }
        transactions.softDelete(userId, transactionId);
        String text = messageSource.getMessage("bot.quick-input-cancelled", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, null);
    }

    private void handleOpenRecategorize(UUID userId, Locale locale, long chatId, int messageId, UUID transactionId) {
        var txOpt = transactions.find(userId, transactionId);
        if (txOpt.isEmpty()) {
            return;
        }
        List<Categories.CategoryView> typed = typedCategories(userId, txOpt.get().type());
        InlineKeyboardMarkup markup = buildCategoryKeyboard(typed, locale, "C" + transactionId, "O" + transactionId, 0);
        String text = messageSource.getMessage("bot.quick-input-ask-category", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, markup);
    }

    private void handlePick(UUID userId, Locale locale, long chatId, int messageId, String payload, boolean forSaved) {
        IdAndInt ref = parseIdAndInt(payload);
        TransactionType type;
        if (forSaved) {
            var txOpt = transactions.find(userId, ref.id());
            if (txOpt.isEmpty()) {
                return;
            }
            type = txOpt.get().type();
        } else {
            var draftOpt = pendingEntryRepository.findByIdAndUserId(ref.id(), userId);
            if (draftOpt.isEmpty()) {
                return;
            }
            type = draftOpt.get().getType();
        }
        List<Categories.CategoryView> typed = typedCategories(userId, type);
        if (ref.value() < 0 || ref.value() >= typed.size()) {
            return;
        }
        UUID categoryId = typed.get(ref.value()).id();

        Transactions.BotTransaction result;
        if (forSaved) {
            result = transactions.updateCategory(userId, ref.id(), categoryId);
        } else {
            TelegramPendingEntry draft = pendingEntryRepository.findByIdAndUserId(ref.id(), userId).orElseThrow();
            var input = new Transactions.BotCreateInput(
                    draft.getAccountId(), categoryId, draft.getType(), draft.getAmount(), null,
                    draft.getDescription(), "tg-draft:" + draft.getId(), draft.getTransactionDate());
            result = transactions.createFromBot(userId, input);
            pendingEntryRepository.delete(draft);
        }
        rememberCategory(userId, result.description(), categoryId);

        String accountName = accounts.listForUser(userId).stream()
                .filter(a -> a.id().equals(result.accountId())).findFirst().map(Accounts.AccountView::name).orElse("");
        String categoryLabel = categoryLabel(userId, categoryId, locale);
        String amountText = moneyFormatter.format(Money.of(result.amount()), locale);

        String text;
        if (result.type() == TransactionType.EXPENSE) {
            BigDecimal todayTotal = transactions.summarize(userId, userProfiles.zoneOf(userId)).todayTotal();
            text = messageSource.getMessage("bot.quick-input-saved-expense",
                    new Object[]{amountText, categoryLabel, accountName, moneyFormatter.format(Money.of(todayTotal), locale)},
                    locale);
            text = appendTodayLeftLine(text, userId, locale);
        } else {
            text = messageSource.getMessage(
                    "bot.quick-input-saved-income", new Object[]{amountText, categoryLabel, accountName}, locale);
        }
        text = appendBackdatedNoteLine(text, userId, locale, result.transactionDate());
        telegramGateway.editMessageText(chatId, messageId, text, savedTransactionButtons(locale, result.id()));
    }

    private void handlePage(UUID userId, Locale locale, long chatId, int messageId, String payload, boolean forSaved) {
        IdAndInt ref = parseIdAndInt(payload);
        TransactionType type;
        String pickPrefix;
        String pagePrefix;
        if (forSaved) {
            var txOpt = transactions.find(userId, ref.id());
            if (txOpt.isEmpty()) {
                return;
            }
            type = txOpt.get().type();
            pickPrefix = "C" + ref.id();
            pagePrefix = "O" + ref.id();
        } else {
            var draftOpt = pendingEntryRepository.findByIdAndUserId(ref.id(), userId);
            if (draftOpt.isEmpty()) {
                return;
            }
            type = draftOpt.get().getType();
            pickPrefix = "N" + ref.id();
            pagePrefix = "M" + ref.id();
        }
        List<Categories.CategoryView> typed = typedCategories(userId, type);
        InlineKeyboardMarkup markup = buildCategoryKeyboard(typed, locale, pickPrefix, pagePrefix, ref.value());
        String text = messageSource.getMessage("bot.quick-input-ask-category", null, locale);
        telegramGateway.editMessageText(chatId, messageId, text, markup);
    }

    private IdAndInt parseIdAndInt(String payload) {
        int dot = payload.lastIndexOf('.');
        return new IdAndInt(UUID.fromString(payload.substring(0, dot)), Integer.parseInt(payload.substring(dot + 1)));
    }

    private record IdAndInt(UUID id, int value) {
    }

    // ---- commands ----

    private void handleToday(long chatId, UUID userId, Locale locale) {
        ZoneId zone = userProfiles.zoneOf(userId);
        List<Transactions.BotTransaction> todayTx = transactions.listToday(userId, zone);

        BigDecimal income = sumByType(todayTx, TransactionType.INCOME);
        BigDecimal expense = sumByType(todayTx, TransactionType.EXPENSE);

        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage("bot.today-header", null, locale)).append('\n');
        sb.append(messageSource.getMessage(
                "bot.today-income", new Object[]{moneyFormatter.format(Money.of(income), locale)}, locale)).append('\n');
        sb.append(messageSource.getMessage(
                "bot.today-expense", new Object[]{moneyFormatter.format(Money.of(expense), locale)}, locale)).append('\n');
        budgets.getToday(userId).ifPresent(today -> {
            sb.append(messageSource.getMessage("bot.today-allowance",
                    new Object[]{moneyFormatter.format(Money.of(today.dailyAllowance()), locale)}, locale)).append('\n');
            String leftLine = today.overspentToday()
                    ? messageSource.getMessage("bot.quick-input-today-over",
                            new Object[]{moneyFormatter.format(Money.of(today.overspentAmount()), locale)}, locale)
                    : messageSource.getMessage("bot.quick-input-today-left",
                            new Object[]{moneyFormatter.format(Money.of(today.todayLeft()), locale)}, locale);
            sb.append(leftLine).append('\n');
        });
        int shown = 0;
        for (var t : todayTx) {
            if (shown >= 10) {
                break;
            }
            String sign = t.type() == TransactionType.EXPENSE ? "-" : "+";
            sb.append(sign).append(moneyFormatter.format(Money.of(t.amount()), locale))
                    .append(" — ").append(categoryLabel(userId, t.categoryId(), locale)).append('\n');
            shown++;
        }
        sendSafely(chatId, sb.toString().strip(), null, "bot.today-header");
    }

    private BigDecimal sumByType(List<Transactions.BotTransaction> list, TransactionType type) {
        return list.stream().filter(t -> t.type() == type).map(Transactions.BotTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void handleBalance(long chatId, UUID userId, Locale locale) {
        List<Accounts.AccountView> userAccountViews = accounts.listForUser(userId);
        BigDecimal total = userAccountViews.stream().map(Accounts.AccountView::balance).reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage("bot.balance-header", null, locale)).append('\n');
        for (var a : userAccountViews) {
            sb.append(a.name()).append(": ").append(moneyFormatter.format(Money.of(a.balance()), locale)).append('\n');
        }
        sb.append(messageSource.getMessage(
                "bot.balance-total", new Object[]{moneyFormatter.format(Money.of(total), locale)}, locale));
        sendSafely(chatId, sb.toString(), null, "bot.balance-header");
    }

    private void handleUndo(long chatId, UUID userId, Locale locale) {
        Optional<Transactions.BotTransaction> removed = transactions.undoLast(userId);
        if (removed.isEmpty()) {
            reply(chatId, locale, "bot.undo-nothing");
            return;
        }
        Transactions.BotTransaction t = removed.get();
        String categoryLabel = categoryLabel(userId, t.categoryId(), locale);
        String amountText = moneyFormatter.format(Money.of(t.amount().abs()), locale);
        String text = messageSource.getMessage("bot.undo-removed", new Object[]{amountText, categoryLabel}, locale);
        sendSafely(chatId, text, null, "bot.undo-removed");
    }

    private void handleWeek(long chatId, UUID userId, Locale locale) {
        ZoneId zone = userProfiles.zoneOf(userId);
        DateRange current = DateRange.thisWeek(clock, zone);
        DateRange previous = DateRange.custom(current.from().minusDays(7), current.toInclusive().minusDays(7));
        sendPeriodReport(chatId, userId, locale, "bot.week-header", zone, current, previous);
    }

    private void handleMonth(long chatId, UUID userId, Locale locale) {
        ZoneId zone = userProfiles.zoneOf(userId);
        DateRange current = DateRange.thisMonth(clock, zone);
        DateRange previous = DateRange.ofMonth(YearMonth.from(current.from()).minusMonths(1));
        sendPeriodReport(chatId, userId, locale, "bot.month-header", zone, current, previous);
    }

    private void sendPeriodReport(
            long chatId, UUID userId, Locale locale, String headerKey, ZoneId zone, DateRange current, DateRange previous) {
        Transactions.PeriodAnalytics currentPeriod =
                transactions.analyticsForRange(userId, zone, current.from(), current.toInclusive());
        Transactions.PeriodAnalytics previousPeriod =
                transactions.analyticsForRange(userId, zone, previous.from(), previous.toInclusive());

        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage(headerKey, null, locale)).append('\n');
        sb.append(messageSource.getMessage(
                "bot.period-income", new Object[]{moneyFormatter.format(Money.of(currentPeriod.income()), locale)}, locale))
                .append('\n');
        sb.append(messageSource.getMessage(
                "bot.period-expense", new Object[]{moneyFormatter.format(Money.of(currentPeriod.expense()), locale)}, locale))
                .append('\n');

        List<Transactions.CategoryAmount> top3 = currentPeriod.expenseByCategory().stream()
                .sorted(Comparator.comparing(Transactions.CategoryAmount::amount).reversed())
                .limit(3)
                .toList();
        if (!top3.isEmpty()) {
            sb.append(messageSource.getMessage("bot.period-top-categories", null, locale)).append('\n');
            for (var categoryAmount : top3) {
                sb.append(categoryLabel(userId, categoryAmount.categoryId(), locale)).append(": ")
                        .append(moneyFormatter.format(Money.of(categoryAmount.amount()), locale)).append('\n');
            }
        }
        BigDecimal change = Percentages.changeOf(currentPeriod.expense(), previousPeriod.expense());
        if (change != null) {
            sb.append(messageSource.getMessage("bot.period-change", new Object[]{change.toPlainString()}, locale));
        }
        sendSafely(chatId, sb.toString().strip(), null, headerKey);
    }

    // ---- shared plumbing (unchanged from before this feature) ----

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

    private void replyWithAppButton(long chatId, Locale locale, String messageKey, Object... args) {
        String text = messageSource.getMessage(messageKey, args, locale);
        String buttonText = messageSource.getMessage("bot.open-app-button", null, locale);
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(buttonText)
                .webApp(new WebAppInfo(properties.miniAppUrl()))
                .build();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(button)));
        sendSafely(chatId, text, markup, messageKey);
    }

    private void reply(long chatId, Locale locale, String messageKey) {
        String text = messageSource.getMessage(messageKey, null, locale);
        sendSafely(chatId, text, null, messageKey);
    }

    private void sendSafely(long chatId, String text, InlineKeyboardMarkup markup, String messageKey) {
        try {
            telegramGateway.sendMessage(chatId, text, markup);
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send bot reply: key={}", messageKey);
        }
    }

}
