package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerRelationEntity;
import za.co.mawa.bes.entity.PartnerRelationPKEntity;

import java.util.List;
@Repository
public interface PartnerRelationRepository extends JpaRepository<PartnerRelationEntity, PartnerRelationPKEntity> {
    @Query("SELECT p FROM PartnerRelationEntity p WHERE p.partnerRelationPK.partner2 = :partner2")
    List<PartnerRelationEntity> findPartnerRelationByPartner2(String partner2);

    @Query("SELECT p FROM PartnerRelationEntity p WHERE p.partnerRelationPK.partner1 = :partner1")
    List<PartnerRelationEntity> findPartnerRelationByPartner1(String partner1);

    @Query("SELECT p FROM PartnerRelationEntity p WHERE p.partnerRelationPK.partner1 = :partnerId OR p.partnerRelationPK.partner2 = :partnerId")
    List<PartnerRelationEntity> findAllByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT p FROM PartnerRelationEntity p WHERE (p.partnerRelationPK.partner1 = :partnerId1 AND p.partnerRelationPK.partner2 = :partnerId2) " +
            "OR (p.partnerRelationPK.partner1 = :partnerId2 AND p.partnerRelationPK.partner2 = :partnerId1)")
    List<PartnerRelationEntity> findAllByBothPartnerIds(@Param("partnerId1") String partnerId1, @Param("partnerId2") String partnerId2);
}
