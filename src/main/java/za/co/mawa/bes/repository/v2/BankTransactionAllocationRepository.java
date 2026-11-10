package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.BankTransactionAllocationEntity;

public interface BankTransactionAllocationRepository extends JpaRepository<BankTransactionAllocationEntity, Long> {
    @Query("select coalesce(sum(a.amountCents), 0) from BankTransactionAllocationEntity a "
            + "where a.bankTransactionId = :id and a.releasedAt is null")
    Long usedAmount(@Param("id") Long id);
    java.util.Optional<BankTransactionAllocationEntity> findByAllocationTypeAndObjectId(String allocationType, String objectId);
    java.util.List<BankTransactionAllocationEntity> findByBankTransactionIdOrderByCreatedAtDesc(Long id);
}
