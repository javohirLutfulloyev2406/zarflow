package uz.zarflow.insight;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface InsightRepository extends Repository<Insight, UUID> {

    Insight save(Insight insight);

    Optional<Insight> findByIdAndUserId(UUID id, UUID userId);

    Optional<Insight> findByUserIdAndCodeAndPeriodKey(UUID userId, InsightCode code, String periodKey);

    List<Insight> findAllByUserIdAndDismissedAtIsNullOrderByPriorityAsc(UUID userId);

    Optional<Insight> findFirstByUserIdAndDismissedAtIsNullAndSentInBotAtIsNullOrderByPriorityAsc(UUID userId);

}
