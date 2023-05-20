package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.PartnerIdentityPKEntity;

import java.util.List;

@Repository
public interface PartnerIdentityRepository extends JpaRepository<PartnerIdentityEntity, PartnerIdentityPKEntity> {
    @Query("SELECT p FROM PartnerIdentityEntity p WHERE p.partner = :partner")
    List<PartnerIdentityEntity> findPartnerIdentityByPartner(String partner);
    @Query("SELECT p FROM PartnerIdentityEntity p WHERE p.partnerIdentityPK.value = :value")
    List<PartnerIdentityEntity> findPartnerIdentityByValue(String value);
    List<PartnerIdentityEntity> findAll(Specification<PartnerIdentityEntity> byCriteria, Sort sort);
    @Query(value = "SELECT * FROM partner_identity p WHERE p.type = :type AND p.partner = :partner LIMIT 1",nativeQuery = true)
    PartnerIdentityEntity findPartnerIdentityByTypeAndPartner(String type,String partner);
}
