package uz.zarflow.debt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface DebtScheduleRepository extends Repository<DebtScheduleRow, UUID> {

    DebtScheduleRow save(DebtScheduleRow row);

    <S extends DebtScheduleRow> List<S> saveAll(Iterable<S> rows);

    List<DebtScheduleRow> findAllByDebtIdOrderBySeqAsc(UUID debtId);

    List<DebtScheduleRow> findAllByDebtIdAndPaidPaymentIdIsNullOrderByDueDateAsc(UUID debtId);

    Optional<DebtScheduleRow> findFirstByDebtIdAndPaidPaymentIdIsNullOrderByDueDateAsc(UUID debtId);

    void deleteByDebtId(UUID debtId);

    Optional<DebtScheduleRow> findByDebtIdAndDueDate(UUID debtId, LocalDate dueDate);

}
