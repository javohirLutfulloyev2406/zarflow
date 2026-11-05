package uz.zarflow.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface UserRepository extends Repository<User, UUID> {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByTelegramId(Long telegramId);

}
