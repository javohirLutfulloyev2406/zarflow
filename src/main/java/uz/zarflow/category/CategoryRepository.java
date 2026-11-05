package uz.zarflow.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface CategoryRepository extends Repository<Category, UUID> {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    @Query("select c from Category c where (c.userId is null or c.userId = :userId) and c.archivedAt is null order by c.sortOrder")
    List<Category> findVisibleForUser(@Param("userId") UUID userId);

}
