package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerAttachmentEntity;
import za.co.mawa.bes.entity.PartnerAttachmentPKEntity;

import java.util.List;
@Repository
public interface PartnerAttachmentRepository extends JpaRepository<PartnerAttachmentEntity, PartnerAttachmentPKEntity> {
    @Query("SELECT p FROM PartnerAttachmentEntity p WHERE p.partnerAttachmentPKEntity.partner = :partner")
    List<PartnerAttachmentEntity> findByPartner(String partner);
}
