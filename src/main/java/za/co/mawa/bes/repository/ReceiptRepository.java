package za.co.mawa.bes.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.ReceiptEntity;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<ReceiptEntity, String> {
    List<ReceiptEntity> findAll(Specification<ReceiptEntity> byCriteria, Sort sort);
    boolean existsByExtReceiptNumber(String extReceiptNumber);
}
