package uz.zarflow.transaction;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_audit")
class TransactionAudit {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    private TransactionAuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    private String before;

    @JdbcTypeCode(SqlTypes.JSON)
    private String after;

    @CreationTimestamp
    private Instant createdAt;

    protected TransactionAudit() {
    }

    TransactionAudit(UUID transactionId, TransactionAuditAction action, String before, String after) {
        this.transactionId = transactionId;
        this.action = action;
        this.before = before;
        this.after = after;
    }

}
