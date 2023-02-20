package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerDateEntity;
import za.co.mawa.bes.entity.PartnerDatePKEntity;

import java.util.List;

@Repository
public interface PartnerDateRepository extends JpaRepository<PartnerDateEntity, PartnerDatePKEntity> {
    @Query("SELECT p FROM PartnerDate p WHERE p.partnerDatePK.partner_no = :partner_no")
    List<PartnerDateEntity> findByPartner(String partner_no);
}
