package uz.zarflow.category;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController {

    private final Categories categories;
    private final CurrentUser currentUser;

    CategoryController(Categories categories, CurrentUser currentUser) {
        this.categories = categories;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<CategoryResponse> list() {
        return categories.listForUser(currentUser.requireUserId()).stream().map(this::toResponse).toList();
    }

    private CategoryResponse toResponse(Categories.CategoryView view) {
        return new CategoryResponse(
                view.id(), view.code(), view.nameUz(), view.nameRu(), view.type(),
                view.necessity(), view.icon(), view.color(), view.fixedDefault());
    }

    record CategoryResponse(
            UUID id,
            String code,
            String nameUz,
            String nameRu,
            CategoryType type,
            Necessity necessity,
            String icon,
            String color,
            boolean fixedDefault) {
    }

}
