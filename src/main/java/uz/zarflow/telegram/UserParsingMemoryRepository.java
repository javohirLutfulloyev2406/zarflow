package uz.zarflow.telegram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface UserParsingMemoryRepository extends Repository<UserParsingMemory, UUID> {

    UserParsingMemory save(UserParsingMemory entry);

    List<UserParsingMemory> findAllByUserId(UUID userId);

    Optional<UserParsingMemory> findByUserIdAndPhrase(UUID userId, String phrase);

}
