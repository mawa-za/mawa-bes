package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.ProductPricingEntity;
import za.co.mawa.bes.entity.ProductPricingPKEntity;

import java.util.List;

@Repository
public interface ProductPricingRepository extends JpaRepository<ProductPricingEntity, ProductPricingPKEntity>
{
    @Query("SELECT p FROM ProductPricingEntity p WHERE p.productPricingPKEntity.product = :productId")
    List<ProductPricingEntity>findPricing(String productId);
    @Modifying
    @Query("DELETE from ProductPricingEntity p WHERE p.productPricingPKEntity.product = :productId")
    void deleteByProductId(String productId);
}
