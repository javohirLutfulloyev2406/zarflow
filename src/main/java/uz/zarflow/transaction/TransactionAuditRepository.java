package uz.zarflow.transaction;

import java.util.UUID;

import org.springframework.data.repository.Repository;

interface TransactionAuditRepository extends Repository<TransactionAudit, UUID> {

    TransactionAudit save(TransactionAudit audit);

}
