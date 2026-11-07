package uz.zarflow.telegram;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface TelegramPendingEntryRepository extends Repository<TelegramPendingEntry, UUID> {

    TelegramPendingEntry save(TelegramPendingEntry entry);

    Optional<TelegramPendingEntry> findByIdAndUserId(UUID id, UUID userId);

    void delete(TelegramPendingEntry entry);

}
