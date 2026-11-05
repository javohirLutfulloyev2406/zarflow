package uz.zarflow.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface AccountRepository extends Repository<Account, UUID> {

    Account save(Account account);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    List<Account> findAllByUserIdAndArchivedAtIsNull(UUID userId);

}
