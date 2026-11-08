package uz.zarflow.budget;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface BudgetRepository extends Repository<Budget, UUID> {

    Budget save(Budget budget);

    Optional<Budget> findByUserIdAndMonth(UUID userId, LocalDate month);

}
