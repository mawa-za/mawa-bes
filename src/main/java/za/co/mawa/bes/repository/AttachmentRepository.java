package za.co.mawa.bes.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerEntity;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity,String> {
    @Query("SELECT a.id FROM AttachmentEntity a WHERE a.file IS NOT NULL AND (a.filePath IS NULL OR a.filePath = '') AND a.id > :afterId ORDER BY a.id")
    List<String> findLegacyAttachmentIdsAfter(@Param("afterId") String afterId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AttachmentEntity a WHERE a.id = :id")
    Optional<AttachmentEntity> findByIdForMigration(@Param("id") String id);

    @Query("SELECT COUNT(a) FROM AttachmentEntity a WHERE a.file IS NOT NULL AND (a.filePath IS NULL OR a.filePath = '')")
    long countLegacyDatabaseFiles();

    @Query("SELECT a FROM AttachmentEntity a WHERE a.objectId = :objectId")
    List<AttachmentEntity> findByObjectId(String objectId);

    @Query("SELECT a FROM AttachmentEntity a WHERE a.objectId IN :objectIds ORDER BY a.uploadDate DESC, a.uploadTime DESC, a.id DESC")
    List<AttachmentEntity> findByObjectIdIn(@Param("objectIds") List<String> objectIds);

    @Query("SELECT a FROM AttachmentEntity a WHERE a.objectId = :objectId AND a.documentType = :documentType")
    AttachmentEntity findByObjectDocumentType(String objectId, String documentType);
}
