package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerViewEntity;


import java.util.List;

@Repository
public interface PartnerViewRepository extends JpaRepository<PartnerViewEntity, String> {
    @Query("SELECT p FROM PartnerViewEntity p ORDER BY p.partnerNo")
    List<PartnerViewEntity> findAllOrderedByPartnerNo();
}