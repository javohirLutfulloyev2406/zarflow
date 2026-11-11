package uz.zarflow.humo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.account.Accounts;
import uz.zarflow.category.Categories;
import uz.zarflow.debt.DebtStatus;
import uz.zarflow.debt.Debts;
import uz.zarflow.humo.HumoTransactionParser.HumoTransactionBlock;
import uz.zarflow.humo.HumoTransactionParser.ParsedBatch;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;

@Service
class HumoImportService implements HumoImport {

    private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    private static final Duration DUPLICATE_WINDOW = Duration.ofHours(3);

    private final Accounts accounts;
    private final Categories categories;
    private final Debts debts;
    private final Transactions transactions;
    private final HumoImportDraftRepository draftRepository;
    private final JdbcTemplate jdbcTemplate;

    HumoImportService(
            Accounts accounts,
            Categories categories,
            Debts debts,
            Transactions transactions,
            HumoImportDraftRepository draftRepository,
            JdbcTemplate jdbcTemplate) {
        this.accounts = accounts;
        this.categories = categories;
        this.debts = debts;
        this.transactions = transactions;
        this.draftRepository = draftRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public ProcessResult process(UUID userId, String rawText, Function<String, UUID> categoryMemoryLookup) {
        ParsedBatch batch = HumoTransactionParser.parse(rawText);
        if (batch.blocks().isEmpty() && batch.skippedNonUzs() == 0) {
            return new ProcessResult(false, 0, 0, 0, List.of());
        }

        int added = 0;
        int duplicates = 0;
        List<Outcome> pending = new ArrayList<>();
        for (HumoTransactionBlock block : batch.blocks()) {
            Optional<UUID> bankImportId = claim(userId, HumoImportFingerprint.of(block));
            if (bankImportId.isEmpty()) {
                duplicates++;
                continue;
            }

            Optional<UUID> accountId = accounts.findAccountIdByHumoCard(userId, block.cardLast4());
            if (accountId.isEmpty()) {
                HumoImportDraft draft = createDraft(userId, bankImportId.get(), block, null, HumoImportDraft.State.PENDING_CARD_LINK);
                pending.add(new Outcome.NeedsCardLink(draft.getId(), block.cardLast4(), block.type(), block.amount(), block.merchantRaw()));
                continue;
            }

            HumoImportDraft draft = createDraft(userId, bankImportId.get(), block, accountId.get(), HumoImportDraft.State.PENDING_CATEGORY);
            Outcome outcome = classifyFresh(draft, categoryMemoryLookup);
            if (outcome instanceof Outcome.Booked) {
                added++;
            } else {
                pending.add(outcome);
            }
        }
        return new ProcessResult(true, added, duplicates, batch.skippedNonUzs(), pending);
    }

    @Override
    @Transactional
    public Optional<Outcome> chooseAccount(UUID userId, UUID draftId, UUID accountId, Function<String, UUID> categoryMemoryLookup) {
        Optional<HumoImportDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty() || draftOpt.get().getState() != HumoImportDraft.State.PENDING_CARD_LINK) {
            return Optional.empty();
        }
        HumoImportDraft draft = draftOpt.get();
        accounts.requireOwned(userId, accountId);
        accounts.linkHumoCard(userId, accountId, draft.getCardLast4());
        draft.linkAccount(accountId, HumoImportDraft.State.PENDING_CATEGORY);
        draftRepository.save(draft);
        return Optional.of(classifyFresh(draft, categoryMemoryLookup));
    }

    @Override
    @Transactional
    public Optional<Outcome> chooseP2p(UUID userId, UUID draftId, P2pChoice choice, Function<String, UUID> categoryMemoryLookup) {
        Optional<HumoImportDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty() || draftOpt.get().getState() != HumoImportDraft.State.PENDING_P2P_CHOICE) {
            return Optional.empty();
        }
        HumoImportDraft draft = draftOpt.get();
        rememberP2pChoice(userId, draft.getMerchantKey(), choice);

        if (choice == P2pChoice.OWN_TRANSFER) {
            draft.advance(HumoImportDraft.State.PENDING_P2P_ACCOUNT);
            draftRepository.save(draft);
            return Optional.of(new Outcome.NeedsP2pAccount(draft.getId(), draft.getAccountId(), draft.getAmount()));
        }
        draft.retype(choice == P2pChoice.INCOME ? TransactionType.INCOME : TransactionType.EXPENSE);
        return Optional.of(resolveCategoryPath(draft, categoryMemoryLookup));
    }

    @Override
    @Transactional
    public Optional<Outcome> chooseP2pAccount(UUID userId, UUID draftId, UUID destinationAccountId) {
        Optional<HumoImportDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty() || draftOpt.get().getState() != HumoImportDraft.State.PENDING_P2P_ACCOUNT) {
            return Optional.empty();
        }
        HumoImportDraft draft = draftOpt.get();
        accounts.requireOwned(userId, destinationAccountId);
        var input = new Transactions.BankImportTransferInput(
                draft.getAccountId(), destinationAccountId, draft.getAmount(), draft.getMerchantRaw(), draft.getTransactionDate());
        transactions.createTransferFromBankImport(userId, input);
        draftRepository.delete(draft);
        return Optional.of(new Outcome.Booked(TransactionType.TRANSFER, draft.getAmount(), null));
    }

    @Override
    @Transactional
    public Optional<Outcome> pickDebt(UUID userId, UUID draftId, UUID debtId, Function<String, UUID> categoryMemoryLookup) {
        Optional<HumoImportDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty() || draftOpt.get().getState() != HumoImportDraft.State.PENDING_LOAN_PICK) {
            return Optional.empty();
        }
        HumoImportDraft draft = draftOpt.get();
        if (debtId == null) {
            return Optional.of(resolveCategoryPath(draft, categoryMemoryLookup));
        }
        debts.requireOwned(userId, debtId);
        LocalDate paidOn = draft.getTransactionDate().atZone(TASHKENT).toLocalDate();
        Debts.PaymentResult result = debts.pay(userId, debtId, new Debts.PayInput(draft.getAmount(), paidOn, draft.getAccountId(), false));
        markBooked(draft.getBankImportId(), result.transactionId());
        draftRepository.delete(draft);
        return Optional.of(new Outcome.Booked(TransactionType.DEBT_PAYMENT, draft.getAmount(), result.transactionId()));
    }

    @Override
    @Transactional
    public Optional<Outcome> pickCategory(UUID userId, UUID draftId, UUID categoryId) {
        Optional<HumoImportDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty() || draftOpt.get().getState() != HumoImportDraft.State.PENDING_CATEGORY) {
            return Optional.empty();
        }
        return Optional.of(bookOrAskDuplicate(draftOpt.get(), categoryId));
    }

    @Override
    @Transactional
    public Optional<Outcome> confirmDuplicate(UUID userId, UUID draftId, boolean isDuplicate) {
        Optional<HumoImportDraft> draftOpt = draftRepository.findByIdAndUserId(draftId, userId);
        if (draftOpt.isEmpty() || draftOpt.get().getState() != HumoImportDraft.State.PENDING_DUPLICATE_CONFIRM) {
            return Optional.empty();
        }
        HumoImportDraft draft = draftOpt.get();
        if (isDuplicate) {
            draftRepository.delete(draft);
            return Optional.of(new Outcome.Skipped(draft.getType(), draft.getAmount()));
        }
        return Optional.of(bookExpenseIncome(draft, draft.getCategoryId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryQuestion> findCategoryQuestion(UUID userId, UUID draftId) {
        return draftRepository.findByIdAndUserId(draftId, userId)
                .filter(d -> d.getState() == HumoImportDraft.State.PENDING_CATEGORY)
                .map(d -> new CategoryQuestion(d.getType(), d.getMerchantKey()));
    }

    /** Called right after this block's account becomes known (fresh from {@link #process},
     * or just linked via {@link #chooseAccount}) -- the full P2P/loan/category decision. */
    private Outcome classifyFresh(HumoImportDraft draft, Function<String, UUID> categoryMemoryLookup) {
        String merchantKey = draft.getMerchantKey();
        if (MerchantNormalizer.isP2p(merchantKey)) {
            Optional<P2pChoice> remembered = selectP2pChoice(draft.getUserId(), merchantKey);
            if (remembered.isPresent()) {
                if (remembered.get() == P2pChoice.OWN_TRANSFER) {
                    draft.advance(HumoImportDraft.State.PENDING_P2P_ACCOUNT);
                    draftRepository.save(draft);
                    return new Outcome.NeedsP2pAccount(draft.getId(), draft.getAccountId(), draft.getAmount());
                }
                draft.retype(remembered.get() == P2pChoice.INCOME ? TransactionType.INCOME : TransactionType.EXPENSE);
                return resolveCategoryPath(draft, categoryMemoryLookup);
            }
            draft.advance(HumoImportDraft.State.PENDING_P2P_CHOICE);
            draftRepository.save(draft);
            return new Outcome.NeedsP2pChoice(draft.getId(), draft.getMerchantRaw(), draft.getType(), draft.getAmount());
        }
        if (MerchantNormalizer.isLoan(merchantKey)) {
            boolean hasActiveDebt = debts.listForUser(draft.getUserId()).stream().anyMatch(d -> d.status() == DebtStatus.ACTIVE);
            if (hasActiveDebt) {
                draft.advance(HumoImportDraft.State.PENDING_LOAN_PICK);
                draftRepository.save(draft);
                return new Outcome.NeedsLoanPick(draft.getId(), draft.getMerchantRaw(), draft.getAmount());
            }
        }
        return resolveCategoryPath(draft, categoryMemoryLookup);
    }

    /** user_parsing_memory (via {@code categoryMemoryLookup}), then the static merchant
     * hints (docs/PRD.md §4), then {@link Outcome.NeedsCategory ask}. */
    private Outcome resolveCategoryPath(HumoImportDraft draft, Function<String, UUID> categoryMemoryLookup) {
        UUID categoryId = categoryMemoryLookup.apply(draft.getMerchantKey());
        if (categoryId == null) {
            categoryId = MerchantNormalizer.categoryHintCode(draft.getMerchantKey())
                    .flatMap(categories::systemCategoryIdByCode)
                    .orElse(null);
        }
        if (categoryId == null) {
            draft.advance(HumoImportDraft.State.PENDING_CATEGORY);
            draftRepository.save(draft);
            return new Outcome.NeedsCategory(draft.getId(), draft.getType(), draft.getAmount(), draft.getMerchantRaw());
        }
        return bookOrAskDuplicate(draft, categoryId);
    }

    private Outcome bookOrAskDuplicate(HumoImportDraft draft, UUID categoryId) {
        Optional<Transactions.BotTransaction> duplicate = transactions.findPossibleManualDuplicate(
                draft.getUserId(), draft.getType(), draft.getAmount(), draft.getTransactionDate(), DUPLICATE_WINDOW);
        if (duplicate.isPresent()) {
            draft.resolveCategory(categoryId, HumoImportDraft.State.PENDING_DUPLICATE_CONFIRM);
            draftRepository.save(draft);
            return new Outcome.NeedsDuplicateConfirm(draft.getId(), draft.getType(), draft.getAmount(), draft.getMerchantRaw());
        }
        return bookExpenseIncome(draft, categoryId);
    }

    private Outcome bookExpenseIncome(HumoImportDraft draft, UUID categoryId) {
        var input = new Transactions.BankImportCreateInput(
                draft.getAccountId(), categoryId, draft.getType(), draft.getAmount(), draft.getMerchantRaw(),
                "humo-import:" + draft.getId(), draft.getTransactionDate());
        Transactions.BotTransaction saved = transactions.createFromBankImport(draft.getUserId(), input);
        markBooked(draft.getBankImportId(), saved.id());
        draftRepository.delete(draft);
        return new Outcome.Booked(saved.type(), saved.amount(), saved.id());
    }

    private HumoImportDraft createDraft(
            UUID userId, UUID bankImportId, HumoTransactionBlock block, UUID accountId, HumoImportDraft.State initialState) {
        return draftRepository.save(new HumoImportDraft(
                userId, bankImportId, block.type(), block.amount(), block.merchantRaw(), block.merchantKey(),
                block.cardLast4(), accountId, block.transactionDate(), initialState));
    }

    private Optional<UUID> claim(UUID userId, String fingerprint) {
        UUID id = UUID.randomUUID();
        int rows = jdbcTemplate.update(
                "insert into bank_imports(id, user_id, fingerprint) values (?, ?, ?) on conflict (user_id, fingerprint) do nothing",
                id, userId, fingerprint);
        return rows > 0 ? Optional.of(id) : Optional.empty();
    }

    private void markBooked(UUID bankImportId, UUID transactionId) {
        if (transactionId == null) {
            return;
        }
        jdbcTemplate.update("update bank_imports set transaction_id = ? where id = ?", transactionId, bankImportId);
    }

    private Optional<P2pChoice> selectP2pChoice(UUID userId, String merchantKey) {
        List<String> rows = jdbcTemplate.queryForList(
                "select choice from humo_p2p_choices where user_id = ? and merchant_key = ?", String.class, userId, merchantKey);
        return rows.stream().findFirst().map(P2pChoice::valueOf);
    }

    private void rememberP2pChoice(UUID userId, String merchantKey, P2pChoice choice) {
        jdbcTemplate.update("""
                insert into humo_p2p_choices(user_id, merchant_key, choice) values (?, ?, ?)
                on conflict (user_id, merchant_key) do update set choice = excluded.choice, updated_at = now()
                """,
                userId, merchantKey, choice.name());
    }

}
