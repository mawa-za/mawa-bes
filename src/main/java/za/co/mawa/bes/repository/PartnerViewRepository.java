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

    @Query("SELECT p FROM PartnerViewEntity p ORDER BY p.partnerNo")
    List<PartnerViewEntity> findAll();

    @Query("SELECT p FROM PartnerViewEntity p WHERE p.identityNumber like :query OR " +
            "p.name1 like :query OR " +
            "p.name2 like :query OR " +
            "p.name3 like :query ORDER BY p.partnerNo")
    List<PartnerViewEntity> findByString(String query);

    @Query("SELECT p FROM PartnerViewEntity p WHERE p.partnerRole like :role " +
            "ORDER BY p.partnerNo")
    List<PartnerViewEntity> findByRole(String role);
}