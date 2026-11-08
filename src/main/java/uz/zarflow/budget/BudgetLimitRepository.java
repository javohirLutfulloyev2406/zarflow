package uz.zarflow.budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface BudgetLimitRepository extends Repository<BudgetLimit, UUID> {

    BudgetLimit save(BudgetLimit limit);

    List<BudgetLimit> findByBudgetId(UUID budgetId);

    Optional<BudgetLimit> findByBudgetIdAndCategoryId(UUID budgetId, UUID categoryId);

    void deleteByBudgetId(UUID budgetId);

}
