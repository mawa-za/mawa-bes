package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerEntity;


import java.util.List;

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

    List<PartnerEntity> findAll(Specification<PartnerEntity> byCriteria, Sort sort);
}
