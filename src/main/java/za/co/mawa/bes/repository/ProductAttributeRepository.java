package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.ProductAttributeEntity;
import za.co.mawa.bes.entity.ProductAttributePKEntity;
import za.co.mawa.bes.entity.ProductPricingEntity;

import java.util.List;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttributeEntity, ProductAttributePKEntity> {
    @Query("SELECT p FROM ProductAttributeEntity p WHERE p.productAttributePKEntity.product = :productId")
    List<ProductAttributeEntity>findByProduct(String productId);
    List<ProductAttributeEntity> findAll(Specification<ProductAttributeEntity> byCriteria, Sort sort);
}
