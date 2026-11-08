package uz.zarflow.category;

import java.util.List;
import java.util.UUID;

/**
 * The category module's public API. Other modules (transaction's validation)
 * talk to categories only through here — never through {@code Category}/{@code CategoryRepository}.
 */
public interface Categories {

    /** System categories plus this user's own, most-used by this user first. */
    List<CategoryView> listForUser(UUID userId);

    /** 404s (never 403) when the category doesn't exist or isn't visible to this user. */
    CategoryOwnership requireUsable(UUID userId, UUID categoryId);

    /** System categories plus this user's own, with their matching keywords — for the bot's quick-input parser. */
    List<CategoryKeywords> keywordsForUser(UUID userId);

    record CategoryKeywords(UUID id, String nameUz, String nameRu, CategoryType type, List<String> keywords) {
    }

    record CategoryView(
            UUID id,
            String code,
            String nameUz,
            String nameRu,
            CategoryType type,
            Necessity necessity,
            String icon,
            String color,
            long usageCount,
            boolean fixedDefault) {
    }

    record CategoryOwnership(UUID id, CategoryType type, Necessity necessity, boolean fixedDefault) {
    }

}
