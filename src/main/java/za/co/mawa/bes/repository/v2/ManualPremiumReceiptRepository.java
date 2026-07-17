package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ManualPremiumReceiptEntity;
public interface ManualPremiumReceiptRepository extends JpaRepository<ManualPremiumReceiptEntity, String> {
    boolean existsByReceiptBookNoAndManualReceiptNo(String receiptBookNo, String manualReceiptNo);
}
