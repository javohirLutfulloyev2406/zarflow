package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import uz.zarflow.category.Necessity;
import uz.zarflow.shared.security.CurrentUser;
import uz.zarflow.user.UserProfiles;

@RestController
class TransactionController {

    private final TransactionsService transactionsService;
    private final CurrentUser currentUser;
    private final UserProfiles userProfiles;

    TransactionController(TransactionsService transactionsService, CurrentUser currentUser, UserProfiles userProfiles) {
        this.transactionsService = transactionsService;
        this.currentUser = currentUser;
        this.userProfiles = userProfiles;
    }

    @PostMapping("/api/v1/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse create(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var cmd = new TransactionsService.CreateCommand(
                request.accountId(), request.categoryId(), request.type(), request.amount(), request.necessity(),
                request.transactionDate(), request.description(), idempotencyKey);
        return toResponse(transactionsService.create(currentUser.requireUserId(), cmd));
    }

    @PutMapping("/api/v1/transactions/{id}")
    TransactionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTransactionRequest request) {
        var cmd = new TransactionsService.UpdateCommand(
                request.accountId(), request.categoryId(), request.amount(), request.necessity(),
                request.transactionDate(), request.description());
        return toResponse(transactionsService.update(currentUser.requireUserId(), id, cmd));
    }

    @DeleteMapping("/api/v1/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        transactionsService.delete(currentUser.requireUserId(), id);
    }

    @GetMapping("/api/v1/transactions")
    PageResponse list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = currentUser.requireUserId();
        ZoneId zone = userProfiles.zoneOf(userId);
        Instant fromInstant = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();
        var cmd = new TransactionsService.SearchCommand(fromInstant, toInstant, type, categoryId, accountId, q);
        TransactionsService.PageResult result = transactionsService.list(userId, cmd, PageRequest.of(page, size));
        return new PageResponse(result.items().stream().map(this::toResponse).toList(),
                result.totalElements(), result.page(), result.size());
    }

    @PostMapping("/api/v1/accounts/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        var cmd = new TransactionsService.TransferCommand(
                request.fromAccountId(), request.toAccountId(), request.amount(),
                request.transactionDate(), request.description());
        TransactionsService.TransferResult result = transactionsService.transfer(currentUser.requireUserId(), cmd);
        return new TransferResponse(result.transferGroupId(), toResponse(result.fromLeg()), toResponse(result.toLeg()));
    }

    @PostMapping("/api/v1/accounts/{id}/adjust")
    TransactionResponse adjust(@PathVariable UUID id, @Valid @RequestBody AdjustRequest request) {
        TransactionsService.TransactionView view = transactionsService.adjust(
                currentUser.requireUserId(), id, request.actualBalance(), request.description());
        return toResponse(view);
    }

    private TransactionResponse toResponse(TransactionsService.TransactionView view) {
        return new TransactionResponse(
                view.id(), view.accountId(), view.categoryId(), view.type(), view.amount(), view.currency(),
                view.necessity(), view.transactionDate(), view.description(), view.transferGroupId());
    }

    record CreateTransactionRequest(
            @NotNull UUID accountId,
            @NotNull UUID categoryId,
            @NotNull TransactionType type,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            Necessity necessity,
            Instant transactionDate,
            @Size(max = 280) String description) {
    }

    record UpdateTransactionRequest(
            @NotNull UUID accountId,
            @NotNull UUID categoryId,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            Necessity necessity,
            Instant transactionDate,
            @Size(max = 280) String description) {
    }

    record TransferRequest(
            @NotNull UUID fromAccountId,
            @NotNull UUID toAccountId,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            Instant transactionDate,
            @Size(max = 280) String description) {
    }

    record AdjustRequest(@NotNull BigDecimal actualBalance, @Size(max = 280) String description) {
    }

    record TransactionResponse(
            UUID id, UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount, String currency,
            Necessity necessity, Instant transactionDate, String description, UUID transferGroupId) {
    }

    record TransferResponse(UUID transferGroupId, TransactionResponse fromLeg, TransactionResponse toLeg) {
    }

    record PageResponse(List<TransactionResponse> items, long totalElements, int page, int size) {
    }

}
