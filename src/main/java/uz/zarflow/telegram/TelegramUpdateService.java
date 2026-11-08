package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import uz.zarflow.account.AccountType;
import uz.zarflow.account.Accounts;
import uz.zarflow.budget.Budgets;
import uz.zarflow.category.CategoryType;
import uz.zarflow.category.Categories;
import uz.zarflow.recurring.Recurring;
import uz.zarflow.shared.common.DateRange;
import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.common.MoneyFormatter;
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
            default -> runOnboarded(chatId, userId, locale,
                    () -> handleQuickInput(message.getText(), update.getUpdateId(), chatId, userId, locale));
        }
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
                reply(chatId, locale, "bot.quick-input-credit-mention");
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

    private void handleParsedLine(
            QuickInputParser.ParsedLine parsed, long chatId, UUID userId, Locale locale,
            List<Accounts.AccountView> userAccountViews, String idempotencyKey) {
        UUID accountId = resolveAccountId(userAccountViews, parsed.accountHint());
        if (accountId == null) {
            return;
        }

        if (parsed.confidence() == QuickInputParser.Confidence.HIGH) {
            var input = new Transactions.BotCreateInput(
                    accountId, parsed.categoryId(), parsed.type(), parsed.amount(), null, parsed.rawLine(), idempotencyKey);
            Transactions.BotTransaction saved = transactions.createFromBot(userId, input);
            sendSavedConfirmation(chatId, locale, userId, saved);
        } else {
            TelegramPendingEntry draft = pendingEntryRepository.save(
                    new TelegramPendingEntry(userId, accountId, parsed.type(), parsed.amount(), parsed.rawLine()));
            sendCategoryPrompt(chatId, locale, userId, parsed.type(), "N" + draft.getId(), "M" + draft.getId());
        }
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
        InlineKeyboardMarkup markup = savedTransactionButtons(locale, saved.id());
        sendSafely(chatId, text, markup, "bot.quick-input-saved-expense");
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
                case 'Z' -> handleDailyReportAck(chatId, messageId, locale);
                case 'Y' -> handleRecurringPay(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'U' -> handleRecurringSkip(userId, locale, chatId, messageId, UUID.fromString(payload));
                case 'W' -> handleRecurringAck(chatId, messageId, locale);
                default -> { }
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Malformed callback data ignored: {}", data);
        }
    }

    private void handleDailyReportAck(long chatId, int messageId, Locale locale) {
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
                    draft.getDescription(), "tg-draft:" + draft.getId());
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
