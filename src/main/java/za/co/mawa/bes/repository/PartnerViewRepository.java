package za.co.mawa.bes.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerViewEntity;


import java.util.List;

@Repository
public interface PartnerViewRepository extends JpaRepository<PartnerViewEntity, String> {
    @Query("SELECT p FROM PartnerViewEntity p ORDER BY p.partnerNo")
    List<PartnerViewEntity> findAllOrderedByPartnerNo();

    @Query("SELECT p FROM PartnerViewEntity p ORDER BY p.partnerNo")
    List<PartnerViewEntity> findAll();

    @Query("SELECT p FROM PartnerViewEntity p WHERE " +
            "LOWER(COALESCE(p.identityNumber, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.partnerNo, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.name1, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.name2, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.name3, '')) like LOWER(:query) OR " +
            "LOWER(CONCAT(CONCAT(COALESCE(p.name2, ''), ' '), COALESCE(p.name1, ''))) like LOWER(:query) OR " +
            "LOWER(CONCAT(CONCAT(COALESCE(p.name1, ''), ' '), COALESCE(p.name2, ''))) like LOWER(:query) OR " +
            "LOWER(CONCAT(CONCAT(CONCAT(CONCAT(COALESCE(p.name2, ''), ' '), COALESCE(p.name3, '')), ' '), COALESCE(p.name1, ''))) like LOWER(:query) OR " +
            "EXISTS (SELECT c FROM PartnerContactEntity c " +
            "WHERE c.partnerContactPK.partner = p.partnerId " +
            "AND LOWER(COALESCE(c.value, '')) like LOWER(:query)) " +
            "ORDER BY p.partnerNo")
    List<PartnerViewEntity> findByString(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM PartnerViewEntity p WHERE p.partnerRole like :role ORDER BY p.partnerNo")
    List<PartnerViewEntity> findByRole(@Param("role") String role);

    @Query("SELECT p FROM PartnerViewEntity p WHERE p.partnerRole = :role AND (" +
            "LOWER(COALESCE(p.identityNumber, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.partnerNo, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.name1, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.name2, '')) like LOWER(:query) OR " +
            "LOWER(COALESCE(p.name3, '')) like LOWER(:query) OR " +
            "LOWER(CONCAT(CONCAT(COALESCE(p.name2, ''), ' '), COALESCE(p.name1, ''))) like LOWER(:query) OR " +
            "LOWER(CONCAT(CONCAT(COALESCE(p.name1, ''), ' '), COALESCE(p.name2, ''))) like LOWER(:query) OR " +
            "LOWER(CONCAT(CONCAT(CONCAT(CONCAT(COALESCE(p.name2, ''), ' '), COALESCE(p.name3, '')), ' '), COALESCE(p.name1, ''))) like LOWER(:query) OR " +
            "EXISTS (SELECT c FROM PartnerContactEntity c " +
            "WHERE c.partnerContactPK.partner = p.partnerId " +
            "AND LOWER(COALESCE(c.value, '')) like LOWER(:query))) " +
            "ORDER BY p.partnerNo")
    List<PartnerViewEntity> findByStringAndRole(
            @Param("query") String query,
            @Param("role") String role,
            Pageable pageable
    );


    /**
     * partner_view may expose multiple rows for one partner because roles and
     * identities are joined into the view. Return one deterministic row instead
     * of using JpaRepository.findById/getReferenceById, whose single-id loader
     * fails when duplicate physical rows share partner_id.
     */
    @Query(value = """
            SELECT *
              FROM partner_view
             WHERE partner_id = :partnerId
             ORDER BY CASE WHEN partner_role = 'SUPPLIER' THEN 0 ELSE 1 END,
                      partner_role,
                      identity_type,
                      identity_number
             LIMIT 1
            """, nativeQuery = true)
    PartnerViewEntity findCanonicalByPartnerId(@Param("partnerId") String partnerId);
}
