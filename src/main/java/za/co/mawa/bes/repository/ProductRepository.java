package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity,String>
{
}
