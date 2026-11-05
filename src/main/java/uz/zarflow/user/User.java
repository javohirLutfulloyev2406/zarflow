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

    private String language;

    private String timezone;

    private BigDecimal expectedMonthlyIncome;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant deletedAt;

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

}
