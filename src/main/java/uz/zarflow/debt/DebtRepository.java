package uz.zarflow.debt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface DebtRepository extends Repository<Debt, UUID> {

    Debt save(Debt debt);

    Optional<Debt> findByIdAndUserId(UUID id, UUID userId);

    List<Debt> findAllByUserId(UUID userId);

    List<Debt> findAllByUserIdAndStatus(UUID userId, DebtStatus status);

}
