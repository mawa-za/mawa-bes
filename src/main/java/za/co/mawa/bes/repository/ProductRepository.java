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

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity,String>
{
    List<ProductEntity> findAll(Specification<ProductEntity>byCriteria, Sort sort);

    @Query("SELECT p FROM ProductEntity p WHERE p.code = :code ORDER BY p.code")
    ProductEntity findByCode(String code);

    @Query("SELECT p FROM ProductEntity p WHERE (:query IS NULL OR UPPER(p.description) like UPPER(:query) OR UPPER(p.code) like UPPER(:query)) AND (:type IS NULL OR p.type = :type) ORDER BY p.code")
    List<ProductEntity> findByQuery(@Param("type") String type, @Param("query") String query);

    long countByCategoryId(String categoryId);

    long countByCategoryIdInAndTypeNot(Collection<String> categoryIds, String type);

}
