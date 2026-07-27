package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.PartnerIdentityPKEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerIdentityRepository extends JpaRepository<PartnerIdentityEntity, PartnerIdentityPKEntity> {
    @Query("SELECT p FROM PartnerIdentityEntity p WHERE p.partner = :partner")
    List<PartnerIdentityEntity> findByPartner(String partner);
    @Query("SELECT p FROM PartnerIdentityEntity p WHERE p.partner = :partner")
    List<PartnerIdentityEntity> findPartnerIdentityByPartner(String partner);
    @Query("SELECT p FROM PartnerIdentityEntity p WHERE p.partnerIdentityPK.value = :value")
    List<PartnerIdentityEntity> findPartnerIdentityByValue(String value);
    List<PartnerIdentityEntity> findAll(Specification<PartnerIdentityEntity> byCriteria, Sort sort);
    @Query(value = "SELECT * FROM partner_identity p WHERE p.type = :type AND p.partner = :partner LIMIT 1",nativeQuery = true)
    PartnerIdentityEntity findPartnerIdentityByTypeAndPartner(String type,String partner);

    @Query(value = """
            SELECT *
            FROM partner_identity p
            WHERE UPPER(TRIM(p.type)) = :type
              AND UPPER(REPLACE(TRIM(p.value), ' ', '')) = :value
            ORDER BY p.valid_to DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<PartnerIdentityEntity> findByNormalizedIdentity(
            @Param("type") String normalizedType,
            @Param("value") String normalizedValue);
}
