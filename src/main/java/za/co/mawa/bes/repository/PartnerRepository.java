package za.co.mawa.bes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerEntity;


import java.util.List;
import java.util.Set;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, String> {
     @Query("SELECT p FROM PartnerEntity p WHERE p.name1 = :name1")
     List<PartnerEntity> findPartnerByName1(String name1);
    @Query("SELECT p FROM PartnerEntity p WHERE p.name2 = :name2")
    List<PartnerEntity> findPartnerByName2(String name2);
    @Query("SELECT p FROM PartnerEntity p WHERE p.name3 = :name3")
    List<PartnerEntity> findPartnerByName3(String name3);


    @Query("SELECT p FROM PartnerEntity p " +
            "WHERE (:name1 IS NULL OR LOWER(p.name1) LIKE CONCAT(LOWER(:name1), '%')) " +
            "  AND (:name2 IS NULL OR LOWER(p.name2) LIKE CONCAT(LOWER(:name2), '%')) " +
            "  AND (:name3 IS NULL OR LOWER(p.name3) LIKE CONCAT(LOWER(:name3), '%'))")
    List<PartnerEntity> findPartnersByNamePrefix(
            @Param("name1") String name1,
            @Param("name2") String name2,
            @Param("name3") String name3);


    //List<PartnerEntity> findAllById(Specification<PartnerEntity> byCriteria, Sort sort);
    List<PartnerEntity> findAllByIdIn(Set<String> ids , Pageable pageable);

    Page<PartnerEntity> findAll( Specification<PartnerEntity> byCriteria , Pageable pageable);

    @Query(value = "SELECT p.* " +
            "FROM partner p " +
            "INNER JOIN partner_role pr ON p.id = pr.partner " +
            "INNER JOIN partner_identity pi ON p.id = pi.partner " +
            //"INNER JOIN partner_attribute pa ON p.id = pa.partner " +
            "WHERE (:role IS NULL OR pr.role = :role) " +
            //"  AND (:attributeValue IS NULL OR pa.value = :attributeValue) " +
            "  AND (:idNumber IS NULL OR pi.value = :idNumber) " ,

            countQuery = "SELECT COUNT(DISTINCT p.id) " +
                    "FROM partner p " +
                    "INNER JOIN partner_role pr ON p.id = pr.partner " +
                    "INNER JOIN partner_identity pi ON p.id = pi.partner " +
                    //"INNER JOIN partner_attribute pa ON p.id = pa.partner " +
                    "WHERE (:role IS NULL OR pr.role = :role) " +
                    //"  AND (:attributeValue IS NULL OR pa.value = :attributeValue) " +
                    "  AND (:idNumber IS NULL OR pi.value = :idNumber) " ,

            nativeQuery = true)
    Page<PartnerEntity> searchPartners(
            @Param("role") String role,
            @Param("idNumber") String idNumber,
//            @Param("attributeValue") String attributeValue,
//            @Param("cellphone") String cellphone,
//            @Param("email") String email,
//            @Param("name1") String name1,
//            @Param("name2") String name2,
//            @Param("name3") String name3,
            Pageable pageable);
}
