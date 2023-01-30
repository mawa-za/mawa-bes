package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerEntity;

import java.util.List;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, String> {
     @Query(value = "SELECT p FROM Partner p WHERE p.name1 = :name1", nativeQuery = true)
     List<PartnerEntity> findPartnerByName1(String name1);
    @Query(value = "SELECT p FROM Partner p WHERE p.name2 = :name2", nativeQuery = true)
    List<PartnerEntity> findPartnerByName2(String name2);
    @Query(value = "SELECT p FROM Partner p WHERE p.name3 = :name3", nativeQuery = true)
    List<PartnerEntity> findPartnerByName3(String name3);

}
