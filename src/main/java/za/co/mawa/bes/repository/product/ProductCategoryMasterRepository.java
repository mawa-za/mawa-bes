package za.co.mawa.bes.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.product.ProductCategoryMasterEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryMasterRepository extends JpaRepository<ProductCategoryMasterEntity, String> {
    Optional<ProductCategoryMasterEntity> findByCodeIgnoreCase(String code);
    List<ProductCategoryMasterEntity> findAllByOrderBySortOrderAscNameAsc();
    List<ProductCategoryMasterEntity> findByParentIdAndActiveTrueOrderBySortOrderAscNameAsc(String parentId);
    List<ProductCategoryMasterEntity> findByParentIdOrderBySortOrderAscNameAsc(String parentId);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}
