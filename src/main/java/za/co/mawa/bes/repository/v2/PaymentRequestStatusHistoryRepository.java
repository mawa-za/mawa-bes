package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PaymentRequestStatusHistoryEntity;

import java.util.List;

public interface PaymentRequestStatusHistoryRepository extends JpaRepository<PaymentRequestStatusHistoryEntity, String> {

    List<PaymentRequestStatusHistoryEntity> findByPaymentRequestIdOrderByChangedAtAsc(String paymentRequestId);
}
