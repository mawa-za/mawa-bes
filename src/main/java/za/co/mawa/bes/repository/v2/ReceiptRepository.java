package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.ReceiptStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<ReceiptEntity, String> {

    Optional<ReceiptEntity> findByReceiptNo(String receiptNo);

    List<ReceiptEntity> findByPaymentBatchId(String paymentBatchId);

    List<ReceiptEntity> findByMembershipIdOrderByReceiptDateDesc(String membershipId);

    List<ReceiptEntity> findBySourceTypeAndStatusAndReceiptDateBetweenOrderByReceiptDateDesc(
            ReceiptSourceType sourceType,
            ReceiptStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    boolean existsByReceiptNo(String receiptNo);
}
