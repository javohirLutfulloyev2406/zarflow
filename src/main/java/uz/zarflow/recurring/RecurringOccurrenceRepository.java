package uz.zarflow.recurring;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface RecurringOccurrenceRepository extends Repository<RecurringOccurrence, UUID> {

    RecurringOccurrence save(RecurringOccurrence occurrence);

    Optional<RecurringOccurrence> findById(UUID id);

    Optional<RecurringOccurrence> findByRuleIdAndDueDate(UUID ruleId, LocalDate dueDate);

    List<RecurringOccurrence> findByRuleIdInAndDueDateBetween(List<UUID> ruleIds, LocalDate from, LocalDate toInclusive);

}
