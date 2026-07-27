package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ManualReceiptBookEntity;

import java.util.List;
import java.util.Optional;

public interface ManualReceiptBookRepository extends JpaRepository<ManualReceiptBookEntity, String> {
    Optional<ManualReceiptBookEntity> findByReceiptBookNoIgnoreCase(String receiptBookNo);
    boolean existsByReceiptBookNoIgnoreCase(String receiptBookNo);
    List<ManualReceiptBookEntity> findAllByOrderByReceiptBookNoAsc();
    List<ManualReceiptBookEntity> findByActiveTrueOrderByReceiptBookNoAsc();
}
