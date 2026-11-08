package uz.zarflow.habit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "day_marks")
class DayMark {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private LocalDate date;

    @Column(name = "no_spend")
    private boolean noSpend;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    protected DayMark() {
    }

    DayMark(UUID userId, LocalDate date) {
        this.userId = userId;
        this.date = date;
        this.noSpend = true;
    }

    UUID getUserId() {
        return userId;
    }

    LocalDate getDate() {
        return date;
    }

    boolean isNoSpend() {
        return noSpend;
    }

}
