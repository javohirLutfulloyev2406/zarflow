package uz.zarflow.humo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface HumoReconciliationDraftRepository extends Repository<HumoReconciliationDraft, UUID> {

    HumoReconciliationDraft save(HumoReconciliationDraft draft);

    Optional<HumoReconciliationDraft> findByIdAndUserId(UUID id, UUID userId);

    void delete(HumoReconciliationDraft draft);

}