package za.co.mawa.bes.repository;

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
}
