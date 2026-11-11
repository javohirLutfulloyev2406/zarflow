package uz.zarflow.humo;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.account.Accounts;
import uz.zarflow.category.Categories;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;

@Service
class HumoReconciliationService implements HumoReconciliation {

    /** System categories seeded by V21__seed_tuzatish_categories.sql. */
    private static final String TUZATISH_EXPENSE_CODE = "tuzatish_exp";
    private static final String TUZATISH_INCOME_CODE = "tuzatish_inc";
    private static final String CORRECTION_DESCRIPTION = "Balans tuzatildi";

    private final Accounts accounts;
    private final Categories categories;
    private final Transactions transactions;
    private final HumoReconciliationDraftRepository draftRepository;
    private final JdbcTemplate jdbcTemplate;

    HumoReconciliationService(
            Accounts accounts,
            Categories categories,
            Transactions transactions,
            HumoReconciliationDraftRepository draftRepository,
            JdbcTemplate jdbcTemplate) {
        this.accounts = accounts;
        this.categories = categories;
        this.transactions = transactions;
        this.draftRepository = draftRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public ProcessResult process(UUID userId, String rawText, Instant asOf, boolean wasForwarded) {
        List<HumoBalanceParser.HumoCardAmount> cards = HumoBalanceParser.parse(rawText);
        if (cards.isEmpty()) {
            return new ProcessResult(false, false, List.of(), 0);
        }
        if (!claimMessage(userId, asOf, contentHash(cards))) {
            return new ProcessResult(true, true, List.of(), 0);
        }

        List<CardResult> results = new ArrayList<>();
        int emptyOrIgnored = 0;
        for (HumoBalanceParser.HumoCardAmount card : cards) {
            Optional<UUID> linkedAccountId = accounts.findAccountIdByHumoCard(userId, card.cardLast4());
            if (linkedAccountId.isPresent()) {
                results.add(reconcileLinked(userId, card.cardLast4(), card.balance(), linkedAccountId.get(), asOf));
            } else if (card.balance().signum() == 0 || isIgnored(userId, card.cardLast4())) {
                emptyOrIgnored++;
            } else {
                UUID draftId = saveDraft(userId, null, card.cardLast4(), card.balance(), asOf);
                results.add(new CardResult(card.cardLast4(), Kind.UNLINKED, draftId, card.balance(), null, null, false));
            }
        }
        return new ProcessResult(true, false, results, emptyOrIgnored);
    }

    @Override
    @Transactional
    public Optional<CorrectResult> correct(UUID userId, UUID draftId) {
        Optional<HumoReconciliationDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty() || draftOpt.get().getAccountId() == null) {
            return Optional.empty();
        }
        HumoReconciliationDraft draft = draftOpt.get();
        BigDecimal zarflowBalance = accounts.balanceAsOf(userId, draft.getAccountId(), draft.getAsOf());
        BigDecimal diff = draft.getBankBalance().subtract(zarflowBalance);
        draftRepository.delete(draft);
        if (diff.signum() == 0) {
            return Optional.of(new CorrectResult(null, BigDecimal.ZERO));
        }

        TransactionType type = diff.signum() > 0 ? TransactionType.INCOME : TransactionType.EXPENSE;
        UUID categoryId = resolveCategoryId(type == TransactionType.INCOME ? TUZATISH_INCOME_CODE : TUZATISH_EXPENSE_CODE);
        String idempotencyKey = "humo:" + draft.getAccountId() + ":" + draft.getAsOf();
        var input = new Transactions.BotCreateInput(
                draft.getAccountId(), categoryId, type, diff.abs(), null, CORRECTION_DESCRIPTION, idempotencyKey, draft.getAsOf());
        Transactions.BotTransaction saved = transactions.createFromBot(userId, input);
        return Optional.of(new CorrectResult(saved.type(), saved.amount()));
    }

    @Override
    @Transactional
    public void skip(UUID userId, UUID draftId) {
        draftRepository.findByIdAndUserId(draftId, userId).ifPresent(draftRepository::delete);
    }

    @Override
    @Transactional
    public Optional<LinkResult> link(UUID userId, UUID draftId, UUID accountId) {
        Optional<HumoReconciliationDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty()) {
            return Optional.empty();
        }
        HumoReconciliationDraft draft = draftOpt.get();
        accounts.requireOwned(userId, accountId);
        accounts.linkHumoCard(userId, accountId, draft.getCardLast4());

        BigDecimal zarflowBalance = accounts.balanceAsOf(userId, accountId, draft.getAsOf());
        HumoReconciliationMath.Classification classification = HumoReconciliationMath.classify(draft.getBankBalance(), zarflowBalance);
        if (classification.matches()) {
            draftRepository.delete(draft);
            return Optional.of(new LinkResult(true, null, BigDecimal.ZERO, false));
        }
        draft.link(accountId);
        draftRepository.save(draft);
        return Optional.of(new LinkResult(false, draft.getId(), classification.diff(), markHintIfFirstTime(userId)));
    }

    @Override
    @Transactional
    public void ignore(UUID userId, UUID draftId) {
        draftRepository.findByIdAndUserId(draftId, userId).ifPresent(draft -> {
            markIgnored(userId, draft.getCardLast4());
            draftRepository.delete(draft);
        });
    }

    private CardResult reconcileLinked(UUID userId, String cardLast4, BigDecimal bankBalance, UUID accountId, Instant asOf) {
        BigDecimal zarflowBalance = accounts.balanceAsOf(userId, accountId, asOf);
        HumoReconciliationMath.Classification classification = HumoReconciliationMath.classify(bankBalance, zarflowBalance);
        if (classification.matches()) {
            return new CardResult(cardLast4, Kind.MATCHED, null, bankBalance, zarflowBalance, BigDecimal.ZERO, false);
        }
        UUID draftId = saveDraft(userId, accountId, cardLast4, bankBalance, asOf);
        return new CardResult(cardLast4, Kind.DIFF, draftId, bankBalance, zarflowBalance, classification.diff(), markHintIfFirstTime(userId));
    }

    private UUID saveDraft(UUID userId, UUID accountId, String cardLast4, BigDecimal bankBalance, Instant asOf) {
        return draftRepository.save(new HumoReconciliationDraft(userId, accountId, cardLast4, bankBalance, asOf)).getId();
    }

    private UUID resolveCategoryId(String code) {
        // Not categories.listForUser(userId) -- tuzatish_exp/tuzatish_inc are hidden-from-the-
        // picker system categories (V21__seed_tuzatish_categories.sql), resolved by code directly.
        return categories.systemCategoryIdByCode(code)
                .orElseThrow(() -> new IllegalStateException("Missing system category: " + code));
    }

    private boolean claimMessage(UUID userId, Instant asOf, String contentHash) {
        int rows = jdbcTemplate.update(
                "insert into humo_balance_messages(user_id, as_of, content_hash) values (?, ?, ?) on conflict do nothing",
                userId, Timestamp.from(asOf), contentHash);
        return rows > 0;
    }

    private boolean isIgnored(UUID userId, String cardLast4) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists(select 1 from humo_ignored_cards where user_id = ? and card_last4 = ?)",
                Boolean.class, userId, cardLast4);
        return Boolean.TRUE.equals(exists);
    }

    private void markIgnored(UUID userId, String cardLast4) {
        jdbcTemplate.update(
                "insert into humo_ignored_cards(user_id, card_last4) values (?, ?) on conflict do nothing", userId, cardLast4);
    }

    /** @return true the first time ever for this user (the one-time hint should show now). */
    private boolean markHintIfFirstTime(UUID userId) {
        int rows = jdbcTemplate.update("insert into humo_hints(user_id) values (?) on conflict do nothing", userId);
        return rows > 0;
    }

    private String contentHash(List<HumoBalanceParser.HumoCardAmount> cards) {
        String joined = cards.stream()
                .map(c -> c.cardLast4() + ":" + c.balance().toPlainString())
                .collect(Collectors.joining("|"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(joined.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

}