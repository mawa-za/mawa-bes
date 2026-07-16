package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PaymentDisbursementAttemptEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentDisbursementAttemptRepository extends JpaRepository<PaymentDisbursementAttemptEntity, String> {
    Optional<PaymentDisbursementAttemptEntity> findFirstByPaymentRequestIdOrderByAttemptNoDesc(String paymentRequestId);
    Optional<PaymentDisbursementAttemptEntity> findFirstByInstructionIdOrderByAttemptNoDesc(String instructionId);
    List<PaymentDisbursementAttemptEntity> findByPaymentRequestIdOrderByAttemptNoAsc(String paymentRequestId);
}
