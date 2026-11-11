package uz.zarflow.humo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface HumoImportDraftRepository extends Repository<HumoImportDraft, UUID> {

    HumoImportDraft save(HumoImportDraft draft);

    Optional<HumoImportDraft> findByIdAndUserId(UUID id, UUID userId);

    void delete(HumoImportDraft draft);

}
