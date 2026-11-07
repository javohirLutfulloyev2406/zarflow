package uz.zarflow.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

    private final Accounts accounts;
    private final CurrentUser currentUser;

    AccountController(Accounts accounts, CurrentUser currentUser) {
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<AccountResponse> list() {
        return accounts.listForUser(currentUser.requireUserId()).stream().map(this::toResponse).toList();
    }

    @PostMapping
    AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        Accounts.CreateAccount input = new Accounts.CreateAccount(
                request.name(), request.type(), request.initialBalance(), request.color(), false);
        return toResponse(accounts.create(currentUser.requireUserId(), input));
    }

    @PutMapping("/{id}")
    AccountResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        Accounts.UpdateAccount input = new Accounts.UpdateAccount(request.name(), request.type(), request.color());
        return toResponse(accounts.update(currentUser.requireUserId(), id, input));
    }

    @PostMapping("/{id}/archive")
    AccountResponse archive(@PathVariable UUID id) {
        return toResponse(accounts.archive(currentUser.requireUserId(), id));
    }

    private AccountResponse toResponse(Accounts.AccountView view) {
        return new AccountResponse(
                view.id(), view.name(), view.type(), view.currency(), view.initialBalance(),
                view.color(), view.isDefault(), view.balance());
    }

    record AccountResponse(
            UUID id,
            String name,
            AccountType type,
            String currency,
            BigDecimal initialBalance,
            String color,
            boolean isDefault,
            BigDecimal balance) {
    }

    record CreateAccountRequest(
            @NotBlank @Size(max = 60) String name,
            @NotNull AccountType type,
            @NotNull BigDecimal initialBalance,
            @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color) {
    }

    record UpdateAccountRequest(
            @NotBlank @Size(max = 60) String name,
            @NotNull AccountType type,
            @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color) {
    }

}
