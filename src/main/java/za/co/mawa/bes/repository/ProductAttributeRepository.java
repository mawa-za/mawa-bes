package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.ProductAttributeEntity;
import za.co.mawa.bes.entity.ProductAttributePKEntity;

import java.util.List;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttributeEntity, ProductAttributePKEntity> {
    List<ProductAttributeEntity> findAll(Specification<ProductAttributeEntity> byCriteria, Sort sort);
}
