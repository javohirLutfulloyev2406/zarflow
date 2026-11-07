package uz.zarflow.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "accounts")
class Account {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private String name;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    private String currency;

    private BigDecimal initialBalance;

    private String color;

    private boolean isDefault;

    @Version
    private Long version;

    private Instant archivedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Account() {
    }

    Account(UUID userId, String name, AccountType type, BigDecimal initialBalance, String color, boolean isDefault) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.currency = "UZS";
        this.initialBalance = initialBalance;
        this.color = color;
        this.isDefault = isDefault;
    }

    void update(String name, AccountType type, String color) {
        this.name = name;
        this.type = type;
        this.color = color;
    }

    void unmarkDefault() {
        this.isDefault = false;
    }

    void archive(Instant at) {
        this.archivedAt = at;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getName() {
        return name;
    }

    AccountType getType() {
        return type;
    }

    String getCurrency() {
        return currency;
    }

    BigDecimal getInitialBalance() {
        return initialBalance;
    }

    String getColor() {
        return color;
    }

    boolean isDefault() {
        return isDefault;
    }

    Long getVersion() {
        return version;
    }

    Instant getArchivedAt() {
        return archivedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

}
