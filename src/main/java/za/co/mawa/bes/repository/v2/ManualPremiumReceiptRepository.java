package za.co.mawa.bes.repository.v2;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.ManualPremiumReceiptEntity;

import java.util.List;

public interface ManualPremiumReceiptRepository extends JpaRepository<ManualPremiumReceiptEntity, String> {
    boolean existsByReceiptBookNoAndManualReceiptNo(String receiptBookNo, String manualReceiptNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select receipt
              from ManualPremiumReceiptEntity receipt
             where receipt.receiptBookNo = :receiptBookNo
             order by receipt.originalReceiptDate asc, receipt.manualReceiptNo asc
            """)
    List<ManualPremiumReceiptEntity> findByReceiptBookNoForUpdate(
            @Param("receiptBookNo") String receiptBookNo
    );
}
