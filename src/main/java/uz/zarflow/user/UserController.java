package uz.zarflow.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/users/me")
class UserController {

    private final UserProfiles userProfiles;
    private final CurrentUser currentUser;

    UserController(UserProfiles userProfiles, CurrentUser currentUser) {
        this.userProfiles = userProfiles;
        this.currentUser = currentUser;
    }

    @GetMapping
    UserProfileResponse get() {
        return toResponse(userProfiles.getProfile(currentUser.requireUserId()));
    }

    @PutMapping("/settings")
    UserProfileResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        UserProfiles.SettingsUpdate update = new UserProfiles.SettingsUpdate(
                request.language(), request.timezone(), request.expectedMonthlyIncome());
        return toResponse(userProfiles.updateSettings(currentUser.requireUserId(), update));
    }

    @PutMapping("/display-name")
    UserProfileResponse updateDisplayName(@Valid @RequestBody UpdateDisplayNameRequest request) {
        return toResponse(userProfiles.updateDisplayName(currentUser.requireUserId(), request.displayName()));
    }

    private UserProfileResponse toResponse(UserProfiles.UserProfile profile) {
        return new UserProfileResponse(
                profile.id(),
                profile.firstName(),
                profile.lastName(),
                profile.username(),
                profile.displayName(),
                profile.language(),
                profile.timezone(),
                profile.expectedMonthlyIncome(),
                profile.onboardingCompleted(),
                profile.createdAt());
    }

    record UserProfileResponse(
            UUID id,
            String firstName,
            String lastName,
            String username,
            String displayName,
            String language,
            String timezone,
            @Schema(type = "string", example = "8500000.00") BigDecimal expectedMonthlyIncome,
            boolean onboardingCompleted,
            Instant createdAt) {
    }

    record UpdateSettingsRequest(
            @NotBlank @Pattern(regexp = "uz|ru") String language,
            @NotBlank @ValidTimezone String timezone,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
            @Schema(type = "string", example = "8500000.00") BigDecimal expectedMonthlyIncome) {
    }

    record UpdateDisplayNameRequest(@NotBlank @Size(max = 50) String displayName) {
    }

}
