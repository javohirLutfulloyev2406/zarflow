package uz.zarflow.category;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.shared.security.OwnershipGuard;

@Service
class CategoriesService implements Categories {

    private final CategoryRepository categoryRepository;

    CategoriesService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryView> listForUser(UUID userId) {
        return categoryRepository.findVisibleForUserOrderedByUsage(userId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryOwnership requireUsable(UUID userId, UUID categoryId) {
        Category category = OwnershipGuard.requireFound(
                categoryRepository.findVisibleByIdForUser(categoryId, userId), "category");
        return new CategoryOwnership(category.getId(), category.getType(), category.getNecessity(), category.isFixedDefault());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryKeywords> keywordsForUser(UUID userId) {
        return categoryRepository.findVisibleForUser(userId).stream()
                .map(c -> new CategoryKeywords(c.getId(), c.getNameUz(), c.getNameRu(), c.getType(), List.of(c.getKeywords())))
                .toList();
    }

    private CategoryView toView(CategoryRepository.CategoryUsageRow row) {
        return new CategoryView(
                row.getId(),
                row.getCode(),
                row.getNameUz(),
                row.getNameRu(),
                CategoryType.valueOf(row.getType()),
                row.getNecessity() == null ? null : Necessity.valueOf(row.getNecessity()),
                row.getIcon(),
                row.getColor(),
                row.getUsageCount(),
                row.getFixedDefault());
    }

}
