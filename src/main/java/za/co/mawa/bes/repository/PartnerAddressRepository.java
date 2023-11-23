package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerAddressEntity;
import za.co.mawa.bes.entity.PartnerAddressPKEntity;
import za.co.mawa.bes.entity.PartnerEntity;

import java.util.List;

@Repository
public interface PartnerAddressRepository extends JpaRepository<PartnerAddressEntity, PartnerAddressPKEntity> {
    @Query("SELECT p FROM PartnerAddressEntity p WHERE p.partnerAddressPK.partner = :partner")
    List<PartnerAddressEntity> findByPartner(String partner);
    List<PartnerAddressEntity> findAll(Specification<PartnerAddressEntity> byCriteria, Sort sort);
}
