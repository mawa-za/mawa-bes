package za.co.mawa.bes.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.entity.PartnerContactPKEntity;

import java.util.List;
@Repository
public interface PartnerContactRepository extends JpaRepository<PartnerContactEntity, PartnerContactPKEntity> {
    @Query("SELECT p FROM PartnerContactEntity p WHERE p.value = :value")
    List<PartnerContactEntity> findPartnerByValue(String value);

    @Query("SELECT p FROM PartnerContactEntity p WHERE p.value LIKE :valuePrefix%")
    List<PartnerContactEntity> findByValuePrefix(@Param("valuePrefix") String valuePrefix, Pageable pageable);

    @Query("SELECT p FROM PartnerContactEntity p WHERE p.partnerContactPK.partner = :partner")
    List<PartnerContactEntity> findContactsByPartner(String partner);
    List<PartnerContactEntity> findAll(Specification<PartnerContactEntity> byCriteria, Sort sort);
}
