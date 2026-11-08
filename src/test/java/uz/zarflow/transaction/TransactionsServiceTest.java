package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import uz.zarflow.account.Accounts;
import uz.zarflow.category.Categories;
import uz.zarflow.shared.exception.BusinessRuleException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionAuditRepository auditRepository;

    @Mock
    private Accounts accounts;

    @Mock
    private Categories categories;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneId.of("UTC"));
    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransactionsService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TransactionsService(
                transactionRepository, auditRepository, accounts, categories, clock, objectMapper, eventPublisher);
    }

    @Test
    void transferCreatesTwoLegsWithOppositeSignsAndSharedGroupId() {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        when(accounts.requireOwned(userId, fromAccountId)).thenReturn(new Accounts.AccountOwnership(fromAccountId, "UZS"));
        when(accounts.requireOwned(userId, toAccountId)).thenReturn(new Accounts.AccountOwnership(toAccountId, "UZS"));

        service.transfer(userId, new TransactionsService.TransferCommand(
                fromAccountId, toAccountId, new BigDecimal("50000.00"), null, "Taxi uchun"));

        verify(transactionRepository, org.mockito.Mockito.times(2)).save(transactionCaptor.capture());
        Transaction outgoing = transactionCaptor.getAllValues().get(0);
        Transaction incoming = transactionCaptor.getAllValues().get(1);

        assertThat(outgoing.getAccountId()).isEqualTo(fromAccountId);
        assertThat(outgoing.getAmount()).isEqualByComparingTo("-50000.00");
        assertThat(outgoing.getType()).isEqualTo(TransactionType.TRANSFER);

        assertThat(incoming.getAccountId()).isEqualTo(toAccountId);
        assertThat(incoming.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(incoming.getType()).isEqualTo(TransactionType.TRANSFER);

        assertThat(outgoing.getTransferGroupId()).isNotNull().isEqualTo(incoming.getTransferGroupId());
    }

    @Test
    void transferRejectsSameSourceAndDestinationAccount() {
        UUID accountId = UUID.randomUUID();

        assertThatThrownBy(() -> service.transfer(userId, new TransactionsService.TransferCommand(
                accountId, accountId, new BigDecimal("1000.00"), null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo("transaction.transfer_same_account");
    }

    @Test
    void adjustCreatesIncomeCorrectionWhenActualBalanceIsHigher() {
        UUID accountId = UUID.randomUUID();
        when(accounts.requireOwned(userId, accountId)).thenReturn(new Accounts.AccountOwnership(accountId, "UZS"));
        when(accounts.currentBalance(userId, accountId)).thenReturn(new BigDecimal("100000.00"));

        service.adjust(userId, accountId, new BigDecimal("150000.00"), null);

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction correction = transactionCaptor.getValue();
        assertThat(correction.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(correction.getAmount()).isEqualByComparingTo("50000.00");
    }

    @Test
    void adjustCreatesExpenseCorrectionWhenActualBalanceIsLower() {
        UUID accountId = UUID.randomUUID();
        when(accounts.requireOwned(userId, accountId)).thenReturn(new Accounts.AccountOwnership(accountId, "UZS"));
        when(accounts.currentBalance(userId, accountId)).thenReturn(new BigDecimal("100000.00"));

        service.adjust(userId, accountId, new BigDecimal("70000.00"), null);

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction correction = transactionCaptor.getValue();
        assertThat(correction.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(correction.getAmount()).isEqualByComparingTo("30000.00");
    }

    @Test
    void adjustRejectsWhenActualBalanceAlreadyMatches() {
        UUID accountId = UUID.randomUUID();
        when(accounts.requireOwned(userId, accountId)).thenReturn(new Accounts.AccountOwnership(accountId, "UZS"));
        when(accounts.currentBalance(userId, accountId)).thenReturn(new BigDecimal("100000.00"));

        assertThatThrownBy(() -> service.adjust(userId, accountId, new BigDecimal("100000.00"), null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo("transaction.adjust_no_difference");

        verify(transactionRepository, org.mockito.Mockito.never()).save(any());
    }

}
