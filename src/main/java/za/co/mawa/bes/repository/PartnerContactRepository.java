package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.entity.PartnerContactPKEntity;

import java.util.List;

public interface PartnerContactRepository extends JpaRepository<PartnerContactEntity, PartnerContactPKEntity> {
    @Query(value = "SELECT p FROM PartnerContact p WHERE p.value = :value", nativeQuery = true)
    List<PartnerContactEntity> findPartnerByValue(String value);
}
