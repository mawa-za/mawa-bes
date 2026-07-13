package za.co.mawa.bes.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerEntity;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity,String> {
    @Query("SELECT a.id FROM AttachmentEntity a WHERE a.file IS NOT NULL AND (a.filePath IS NULL OR a.filePath = '') AND a.id > :afterId ORDER BY a.id")
    List<String> findLegacyAttachmentIdsAfter(@Param("afterId") String afterId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AttachmentEntity a WHERE a.file IS NOT NULL AND (a.filePath IS NULL OR a.filePath = '')")
    long countLegacyDatabaseFiles();

    @Query("SELECT a FROM AttachmentEntity a WHERE a.objectId = :objectId")
    List<AttachmentEntity> findByObjectId(String objectId);

    @Query("SELECT a FROM AttachmentEntity a WHERE a.objectId = :objectId AND a.documentType = :documentType")
    AttachmentEntity findByObjectDocumentType(String objectId, String documentType);
}
