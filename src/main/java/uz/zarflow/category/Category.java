package uz.zarflow.category;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
class Category {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private String code;

    private String nameUz;

    private String nameRu;

    @Enumerated(EnumType.STRING)
    private CategoryType type;

    @Enumerated(EnumType.STRING)
    private Necessity necessity;

    private String icon;

    private String color;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] keywords;

    private int sortOrder;

    private Instant archivedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Category() {
    }

    Category(UUID userId, String code, String nameUz, String nameRu, CategoryType type, Necessity necessity,
              String icon, String color, String[] keywords, int sortOrder) {
        this.userId = userId;
        this.code = code;
        this.nameUz = nameUz;
        this.nameRu = nameRu;
        this.type = type;
        this.necessity = necessity;
        this.icon = icon;
        this.color = color;
        this.keywords = keywords;
        this.sortOrder = sortOrder;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getCode() {
        return code;
    }

    String getNameUz() {
        return nameUz;
    }

    String getNameRu() {
        return nameRu;
    }

    CategoryType getType() {
        return type;
    }

    Necessity getNecessity() {
        return necessity;
    }

    String getIcon() {
        return icon;
    }

    String getColor() {
        return color;
    }

    String[] getKeywords() {
        return keywords;
    }

    int getSortOrder() {
        return sortOrder;
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
