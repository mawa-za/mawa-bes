package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.PartnerAttributeEntity;
import za.co.mawa.bes.entity.PartnerAttributePKEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;


import java.util.List;

@Repository
public interface PartnerAttributeRepository extends JpaRepository<PartnerAttributeEntity, PartnerAttributePKEntity> {
  @Query("SELECT p FROM PartnerAttributeEntity p WHERE p.partnerAttributePKEntity.attribute = :attribute and p.value = :value")
  List<PartnerAttributeEntity> findByAttributeValue(String attribute,String value);
  @Query("SELECT p FROM PartnerAttributeEntity p WHERE p.value = :value")
  List<PartnerAttributeEntity> findByValue(String value);
  List<PartnerAttributeEntity> findAll(Specification<PartnerAttributeEntity> byCriteria, Sort sort);
}
