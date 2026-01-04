package za.co.mawa.bes.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerEntity;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity,String> {
    @Query("SELECT a FROM AttachmentEntity a WHERE a.objectId = :objectId")
    List<AttachmentEntity> findByObjectId(String objectId);

    @Query("SELECT a FROM AttachmentEntity a WHERE a.objectId = :objectId AND a.documentType = :type")
    AttachmentEntity findByObjectDocumentType(String objectId, String documentType);
}
