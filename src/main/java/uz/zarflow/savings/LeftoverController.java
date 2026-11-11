package uz.zarflow.savings;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/leftover")
class LeftoverController {

    private final Savings savings;
    private final CurrentUser currentUser;

    LeftoverController(Savings savings, CurrentUser currentUser) {
        this.savings = savings;
        this.currentUser = currentUser;
    }

    @GetMapping("/current")
    Optional<OfferResponse> current() {
        return savings.currentOffer(currentUser.requireUserId()).map(this::toResponse);
    }

    @PostMapping("/{month}/resolve")
    OfferResponse resolve(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, @Valid @RequestBody ResolveRequest request) {
        Savings.Resolution resolution = new Savings.Resolution(
                request.type(), request.splitGoalAmount(), request.splitDebtAmount());
        return toResponse(savings.resolve(currentUser.requireUserId(), month, resolution));
    }

    private OfferResponse toResponse(Savings.LeftoverOfferView view) {
        return new OfferResponse(view.id(), view.month(), view.amount(), view.status());
    }

    record ResolveRequest(
            @NotNull Savings.ResolutionType type,
            @DecimalMin("0.00") BigDecimal splitGoalAmount,
            @DecimalMin("0.00") BigDecimal splitDebtAmount) {
    }

    record OfferResponse(java.util.UUID id, YearMonth month, BigDecimal amount, LeftoverStatus status) {
    }

}
