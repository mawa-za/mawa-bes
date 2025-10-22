package za.co.mawa.bes.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.entity.ProductEntity;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity,String>
{
    List<ProductEntity> findAll(Specification<ProductEntity>byCriteria, Sort sort);

    @Query("SELECT p FROM ProductEntity p WHERE p.code = :code ORDER BY p.code")
    ProductEntity findByCode(String code);

}
