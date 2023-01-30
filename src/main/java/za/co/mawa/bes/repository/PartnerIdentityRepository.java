package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.PartnerIdentityPKEntity;

import java.util.List;

@Repository
public interface PartnerIdentityRepository extends JpaRepository<PartnerIdentityEntity, PartnerIdentityPKEntity> {
    @Query(value = "SELECT p FROM PartnerIdentity p WHERE p.partner = :partner", nativeQuery = true)
    List<PartnerIdentityEntity> findPartnerIdentityByPartner(String partner);

    @Query(value = "SELECT p FROM PartnerIdentity p WHERE p.partnerIdentityPK.value = :value", nativeQuery = true)
    List<PartnerIdentityEntity> findPartnerIdentityByValue(String value);
}
