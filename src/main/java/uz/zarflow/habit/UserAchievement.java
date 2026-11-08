package uz.zarflow.habit;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_achievements")
class UserAchievement {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private AchievementCode code;

    @CreationTimestamp
    private Instant achievedAt;

    protected UserAchievement() {
    }

    UserAchievement(UUID userId, AchievementCode code) {
        this.userId = userId;
        this.code = code;
    }

    UUID getUserId() {
        return userId;
    }

    AchievementCode getCode() {
        return code;
    }

    Instant getAchievedAt() {
        return achievedAt;
    }

}
