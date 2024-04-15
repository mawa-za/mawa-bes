package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.ProductAttributeEntity;
import za.co.mawa.bes.entity.ProductAttributePKEntity;
import za.co.mawa.bes.entity.product.ProductCategoryEntity;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, String> {
    @Query("SELECT p FROM ProductCategoryEntity p WHERE p.product = :product")
    List<ProductCategoryEntity>findByProduct(String product);
    @Query("SELECT p FROM ProductCategoryEntity p WHERE p.category = :category")
    List<ProductCategoryEntity>findByCategory(String category);
}
