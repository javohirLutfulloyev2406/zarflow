package uz.zarflow.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface CategoryRepository extends Repository<Category, UUID> {

    Category save(Category category);

    /** Excludes hidden system categories (internal bookkeeping only, e.g. Humo correction
     * categories) -- never offered in the user-facing picker. */
    @Query("""
            select c from Category c
            where (c.userId is null or c.userId = :userId) and c.archivedAt is null and c.hidden = false
            order by c.sortOrder
            """)
    List<Category> findVisibleForUser(@Param("userId") UUID userId);

    /** Unlike {@link #findVisibleForUser}, intentionally does not filter out hidden categories --
     * this is "is this category usable by id" (Categories#requireUsable), not "should the picker
     * offer it", and a correction transaction must still validate against its hidden category. */
    @Query("select c from Category c where c.id = :id and (c.userId is null or c.userId = :userId) and c.archivedAt is null")
    Optional<Category> findVisibleByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    /** A system category's id by its well-known code, hidden or not -- for modules that need
     * to reference a specific system category directly (docs/PRD.md §7 Humo corrections)
     * rather than through the user-facing picker, which hides internal-use-only ones. */
    @Query("select c from Category c where c.userId is null and c.code = :code and c.archivedAt is null")
    Optional<Category> findSystemByCode(@Param("code") String code);

    /** System categories plus this user's own, most-used by this user first; unused ones keep the curated sort_order. */
    @Query(value = """
            SELECT c.id AS id, c.code AS code, c.name_uz AS nameUz, c.name_ru AS nameRu,
                   c.type AS type, c.necessity AS necessity, c.icon AS icon, c.color AS color,
                   c.fixed_default AS fixedDefault, COUNT(t.id) AS usageCount
            FROM categories c
            LEFT JOIN transactions t ON t.category_id = c.id AND t.user_id = :userId AND t.deleted_at IS NULL
            WHERE (c.user_id IS NULL OR c.user_id = :userId) AND c.archived_at IS NULL AND c.hidden = false
            GROUP BY c.id, c.sort_order
            ORDER BY usageCount DESC, c.sort_order ASC
            """, nativeQuery = true)
    List<CategoryUsageRow> findVisibleForUserOrderedByUsage(@Param("userId") UUID userId);

    interface CategoryUsageRow {
        UUID getId();

        String getCode();

        String getNameUz();

        String getNameRu();

        String getType();

        String getNecessity();

        String getIcon();

        String getColor();

        boolean getFixedDefault();

        long getUsageCount();
    }

}
