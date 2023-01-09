package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.raretag.mawabes.entity.NumberRangeEntity;
import za.co.raretag.mawabes.entity.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity,String>
{
}
