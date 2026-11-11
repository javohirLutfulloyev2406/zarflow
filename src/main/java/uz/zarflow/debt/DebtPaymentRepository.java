package uz.zarflow.debt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface DebtPaymentRepository extends Repository<DebtPayment, UUID> {

    DebtPayment save(DebtPayment payment);

    List<DebtPayment> findAllByDebtIdOrderByPaidOnDesc(UUID debtId);

    List<DebtPayment> findAllByDebtIdInAndPaidOnBetween(List<UUID> debtIds, LocalDate from, LocalDate toInclusive);

}
