package uz.zarflow.telegram;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Learns which category the user meant by a given phrase, keyed per user (docs/PRD.md §5 rule 3). */
@Entity
@Table(name = "user_parsing_memory")
class UserParsingMemory {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private String phrase;

    private UUID categoryId;

    @UpdateTimestamp
    private Instant updatedAt;

    protected UserParsingMemory() {
    }

    UserParsingMemory(UUID userId, String phrase, UUID categoryId) {
        this.userId = userId;
        this.phrase = phrase;
        this.categoryId = categoryId;
    }

    void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    UUID getUserId() {
        return userId;
    }

    String getPhrase() {
        return phrase;
    }

    UUID getCategoryId() {
        return categoryId;
    }

}
