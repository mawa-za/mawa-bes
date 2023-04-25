package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.entity.PartnerContactPKEntity;

import java.util.List;
@Repository
public interface PartnerContactRepository extends JpaRepository<PartnerContactEntity, PartnerContactPKEntity> {
    @Query("SELECT p FROM PartnerContactEntity p WHERE p.value = :value")
    List<PartnerContactEntity> findPartnerByValue(String value);
    @Query("SELECT p FROM PartnerContactEntity p WHERE p.partnerContactPK.partner = :partner")
    List<PartnerContactEntity> findContactsByPartner(String partner);
}
