package uz.zarflow.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface UserRepository extends Repository<User, UUID> {

    User save(User user);

    User saveAndFlush(User user);

    Optional<User> findByTelegramId(Long telegramId);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    List<User> findAllByDeletedAtIsNull();

}
