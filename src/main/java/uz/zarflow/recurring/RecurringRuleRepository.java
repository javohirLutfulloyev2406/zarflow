package uz.zarflow.recurring;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface RecurringRuleRepository extends Repository<RecurringRule, UUID> {

    RecurringRule save(RecurringRule rule);

    Optional<RecurringRule> findByIdAndUserId(UUID id, UUID userId);

    List<RecurringRule> findAllByUserIdAndArchivedAtIsNull(UUID userId);

    List<RecurringRule> findAllByUserIdAndKindAndActiveTrueAndArchivedAtIsNull(UUID userId, RecurringKind kind);

    /** For the reminder/auto-create scheduler sweep, across every user. */
    List<RecurringRule> findAllByActiveTrueAndArchivedAtIsNull();

}
