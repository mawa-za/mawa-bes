package za.co.mawa.bes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.dto.partner.PartnerBasicDto;
import za.co.mawa.bes.dto.partner.PartnerPageDto;
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

    @Query("SELECT p FROM PartnerEntity p WHERE CONCAT(p.name1, ' ', p.name2) = :fullName")
    List<PartnerEntity> findByFullName(String fullName);


    @Query("SELECT p FROM PartnerEntity p " +
            "WHERE (:name1 IS NULL OR LOWER(p.name1) LIKE CONCAT(LOWER(:name1), '%')) " +
            "  AND (:name2 IS NULL OR LOWER(p.name2) LIKE CONCAT(LOWER(:name2), '%')) " +
            "  AND (:name3 IS NULL OR LOWER(p.name3) LIKE CONCAT(LOWER(:name3), '%'))")
    List<PartnerEntity> findPartnersByNamePrefix(@Param("name1") String name1, @Param("name2") String name2, @Param("name3") String name3);

    //List<PartnerEntity> findAllById(Specification<PartnerEntity> byCriteria, Sort sort);
    @Query("SELECT new za.co.mawa.bes.dto.partner.PartnerPageDto(p.type, p.id, p.no as number, pi.partnerIdentityPK.type as idType, pi.partnerIdentityPK.value as idNumber, " +
            "p.name1, p.name2, p.name3, p.title, p.birthDate, p.maritalStatus, p.gender, p.language, " +
            "p.status, p.statusReason, p.createdBy, p.validFrom, p.validTo, p.creationDate) " +
            "FROM PartnerEntity p " +
            "INNER JOIN PartnerIdentityEntity pi ON pi.partner = p.id " +
            "WHERE p.id IN :ids")
    List<PartnerPageDto> findAllByIdIn(@Param("ids") Set<String> ids, Pageable pageable);


    List<PartnerEntity> findAll( Specification<PartnerEntity> byCriteria , Sort sort);

    @Query("SELECT new za.co.mawa.bes.dto.partner.PartnerPageDto(p.type, p.id, p.no as number, pi.partnerIdentityPK.type as idType, pi.partnerIdentityPK.value as idNumber, " +
            "p.name1, p.name2, p.name3, p.title, p.birthDate, p.maritalStatus, p.gender, p.language, " +
            "p.status, p.statusReason, p.createdBy, p.validFrom, p.validTo, p.creationDate) " +
            "FROM PartnerEntity p " +
            "JOIN PartnerRoleEntity pr ON pr.partnerRolePK.id = p.id " +
            "JOIN PartnerIdentityEntity pi ON pi.partner = p.id " +
            "WHERE (:role IS NULL OR pr.partnerRolePK.role = :role) " +
            "AND (:type IS NULL OR p.type = :type) " +
            "AND (:idNumber IS NULL OR pi.partnerIdentityPK.value = :idNumber)")
    List<PartnerPageDto> searchPartners(
            @Param("role") String role,
            @Param("idNumber") String idNumber,
            @Param("type") String type,
            Pageable pageable);


//    @Query(value = "SELECT  p.type as type " +
//            "FROM partner p " +
//            "INNER JOIN partner_role pr ON p.id = pr.partner " +
//            "INNER JOIN partner_identity pi ON p.id = pi.partner " +
//            //"INNER JOIN partner_attribute pa ON p.id = pa.partner " +
//            "WHERE (:role IS NULL OR pr.role = :role) " +
//            //"  AND (:attributeValue IS NULL OR pa.value = :attributeValue) " +
//            "  AND (:type IS NULL OR p.type = :type) "+
//            "  AND (:idNumber IS NULL OR pi.value = :idNumber) " ,
//
//            countQuery = "SELECT COUNT(DISTINCT p.id) " +
//                    "FROM partner p " +
//                    "INNER JOIN partner_role pr ON p.id = pr.partner " +
//                    "INNER JOIN partner_identity pi ON p.id = pi.partner " +
//                    //"INNER JOIN partner_attribute pa ON p.id = pa.partner " +
//                    "WHERE (:role IS NULL OR pr.role = :role) " +
//                    //"  AND (:attributeValue IS NULL OR pa.value = :attributeValue) " +
//                    "  AND (:type IS NULL OR p.type = :type) "+
//                    "  AND (:idNumber IS NULL OR pi.value = :idNumber) " ,
//
//            nativeQuery = true)
//    Page<PartnerPageDto> searchPartners(
//            @Param("role") String role,
//            @Param("idNumber") String idNumber,
////            @Param("attributeValue") String attributeValue,
//            @Param("type") String type,
////            @Param("cellphone") String cellphone,
////            @Param("email") String email,
////            @Param("name1") String name1,
////            @Param("name2") String name2,
////            @Param("name3") String name3,
//            Pageable pageable);
}
