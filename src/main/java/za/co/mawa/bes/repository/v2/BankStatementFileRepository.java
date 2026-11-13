package za.co.mawa.bes.repository.v2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.v2.BankStatementFileEntity;
import java.time.LocalDateTime;
import java.util.Optional;

public interface BankStatementFileRepository extends JpaRepository<BankStatementFileEntity, Long> {
    Optional<BankStatementFileEntity> findBySourceAndProviderAndMessageIdAndAttachmentId(
            String source, String provider, String messageId, String attachmentId);

    @Query("select f from BankStatementFileEntity f where "
            + "(:source = '' or f.source = :source) and "
            + "(:status = '' or f.status = :status) and "
            + "(:search = '' or lower(f.fileName) like lower(concat('%', :search, '%')) "
            + "or lower(f.uploadedBy) like lower(concat('%', :search, '%')))")
    Page<BankStatementFileEntity> search(@Param("source") String source, @Param("status") String status,
                                         @Param("search") String search, Pageable pageable);

    /** Claim before processing, preventing overlapping manual and email requests. */
    @Modifying @Transactional
    @Query("update BankStatementFileEntity f set f.status = 'PROCESSING', f.lastProcessedAt = :now, "
            + "f.attemptCount = f.attemptCount + 1 where f.id = :id and f.storagePath is not null "
            + "and (f.status in ('STORED', 'FAILED', 'PARTIAL') "
            + "or (f.status = 'PROCESSING' and f.lastProcessedAt < :stale))")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now,
              @Param("stale") LocalDateTime stale);
}
