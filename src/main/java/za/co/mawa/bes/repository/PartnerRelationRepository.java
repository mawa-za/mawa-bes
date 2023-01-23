package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerRelationEntity;
import za.co.mawa.bes.entity.PartnerRelationPKEntity;

import java.util.List;
@Repository
public interface PartnerRelationRepository extends JpaRepository<PartnerRelationEntity, PartnerRelationPKEntity> {
    @Query(value = "SELECT p FROM PartnerRelation p WHERE p.partnerRelationPK.partner2 = :partner2", nativeQuery = true)
    List<PartnerRelationEntity> findPartnerRelationByPartner2(String partner2);
}
