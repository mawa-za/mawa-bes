package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PremiumEntity;

import java.util.List;

@Repository
public interface PremiumRepository extends JpaRepository<PremiumEntity, String> {
    List<PremiumEntity> findAll(Specification<PremiumEntity> byCriteria, Sort sort);

    @Query("SELECT p FROM PremiumEntity p WHERE p.receiptNumber = :query OR " +
            "p.extReceiptNumber = :query ORDER BY p.receiptNumber")
    List<PremiumEntity> findByString(String query) ;
}
