package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.MessageQueueEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageQueueRepository  extends JpaRepository<MessageQueueEntity, Long> {
    List<MessageQueueEntity> findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(LocalDateTime now);
    // Atomic Xero-only lease: exactly one Cloud Run worker can claim an eligible row.
    // REQUIRES_NEW commits the claim before the external HTTP call begins.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE MessageQueueEntity m SET m.nextAttemptAt = :leaseUntil " +
            "WHERE m.id = :id AND m.processed = false AND m.nextAttemptAt < :now " +
            "AND m.type IN ('XERO-INVOICE', 'XERO-CUSTOMER', 'XERO-PRODUCT')")
    int claimXeroMessage(@Param("id") Long id, @Param("now") LocalDateTime now,
                         @Param("leaseUntil") LocalDateTime leaseUntil);

    // Fencing: a worker may only complete the exact Xero lease it acquired.
    // A reclaimed lease has a different nextAttemptAt and cannot be overwritten
    // by an older worker's detached JPA entity.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE MessageQueueEntity m SET m.processed = :processed, " +
            "m.retryCount = :retryCount, m.nextAttemptAt = :nextAttemptAt " +
            "WHERE m.id = :id AND m.processed = false " +
            "AND m.nextAttemptAt = :expectedLeaseUntil " +
            "AND m.type IN ('XERO-INVOICE', 'XERO-CUSTOMER', 'XERO-PRODUCT')")
    int completeXeroClaim(@Param("id") Long id,
                          @Param("expectedLeaseUntil") LocalDateTime expectedLeaseUntil,
                          @Param("processed") boolean processed,
                          @Param("retryCount") int retryCount,
                          @Param("nextAttemptAt") LocalDateTime nextAttemptAt);

    boolean existsByTypeAndReferenceId(String type, String referenceId);
    Optional<MessageQueueEntity> findFirstByTypeAndReferenceIdOrderByIdDesc(String type, String referenceId);
}

