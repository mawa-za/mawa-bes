package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.PartnerAttributeEntity;
import za.co.mawa.bes.entity.PartnerAttributePKEntity;


import java.util.List;

@Repository
public interface PartnerAttributeRepository extends JpaRepository<PartnerAttributeEntity, PartnerAttributePKEntity> {
  List<PartnerAttributeEntity> findAll(Specification<PartnerAttributeEntity> byCriteria, Sort sort);
}
