package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.ProductPricingEntity;
import za.co.mawa.bes.entity.ProductPricingPKEntity;

@Repository
public interface ProductPricingRepository extends JpaRepository<ProductPricingEntity, ProductPricingPKEntity>
{
}
