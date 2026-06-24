package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerDateEntity;
import za.co.mawa.bes.entity.PartnerDatePKEntity;

import java.util.List;

@Repository
public interface PartnerDateRepository extends JpaRepository<PartnerDateEntity, PartnerDatePKEntity> {

    @Query("SELECT p FROM PartnerDateEntity p WHERE p.partnerDatePK.partnerNumber = :partnerNumber")
    List<PartnerDateEntity> findByPartner(@Param("partnerNumber") String partnerNumber);
}
