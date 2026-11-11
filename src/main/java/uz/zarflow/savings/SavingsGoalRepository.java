package uz.zarflow.savings;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface SavingsGoalRepository extends Repository<SavingsGoal, UUID> {

    SavingsGoal save(SavingsGoal goal);

    Optional<SavingsGoal> findByIdAndUserId(UUID id, UUID userId);

    List<SavingsGoal> findAllByUserId(UUID userId);

    Optional<SavingsGoal> findByUserIdAndIsEmergencyTrueAndStatus(UUID userId, GoalStatus status);

}
