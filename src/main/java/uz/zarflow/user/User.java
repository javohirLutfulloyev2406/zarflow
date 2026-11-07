package uz.zarflow.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private Long telegramId;

    private String firstName;

    private String lastName;

    private String username;

    private String displayName;

    private String language;

    private String timezone;

    private BigDecimal expectedMonthlyIncome;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant deletedAt;

    private Instant onboardingCompletedAt;

    @Version
    private Long version;

    protected User() {
    }

    User(Long telegramId, String firstName, String lastName, String username,
         String language, String timezone, BigDecimal expectedMonthlyIncome) {
        this.telegramId = telegramId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.language = language;
        this.timezone = timezone;
        this.expectedMonthlyIncome = expectedMonthlyIncome;
    }

    void updateProfile(String firstName, String lastName, String username) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    void updateSettings(String language, String timezone, BigDecimal expectedMonthlyIncome) {
        this.language = language;
        this.timezone = timezone;
        this.expectedMonthlyIncome = expectedMonthlyIncome;
    }

    void completeOnboarding(BigDecimal expectedMonthlyIncome, Instant at) {
        this.expectedMonthlyIncome = expectedMonthlyIncome;
        this.onboardingCompletedAt = at;
    }

    void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    UUID getId() {
        return id;
    }

    Long getTelegramId() {
        return telegramId;
    }

    String getFirstName() {
        return firstName;
    }

    String getLastName() {
        return lastName;
    }

    String getUsername() {
        return username;
    }

    String getDisplayName() {
        return displayName;
    }

    String getLanguage() {
        return language;
    }

    String getTimezone() {
        return timezone;
    }

    BigDecimal getExpectedMonthlyIncome() {
        return expectedMonthlyIncome;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getDeletedAt() {
        return deletedAt;
    }

    Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    Long getVersion() {
        return version;
    }

}
