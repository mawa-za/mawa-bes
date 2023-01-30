package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerAttachmentEntity;

import java.util.List;
@Repository
public interface PartnerAttachmentRepository extends JpaRepository<PartnerAttachmentEntity, String> {
    @Query(value = "SELECT p FROM PartnerAttachment p WHERE p.partner = :partner", nativeQuery = true)
    List<PartnerAttachmentEntity> findByPartner(String partner);
}
