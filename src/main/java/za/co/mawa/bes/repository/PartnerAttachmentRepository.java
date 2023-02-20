package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerAttachmentEntity;

import java.util.List;
@Repository
public interface PartnerAttachmentRepository extends JpaRepository<PartnerAttachmentEntity, String> {
    @Query("SELECT p FROM PartnerAttachmentEntity p WHERE p.partner = :partner")
    List<PartnerAttachmentEntity> findByPartner(String partner);
}
