package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;

import java.util.List;
import java.util.Optional;

public interface PaymentBatchRepository extends JpaRepository<PaymentBatchEntity, String> {

    Optional<PaymentBatchEntity> findByPaymentBatchNo(String paymentBatchNo);

    Optional<PaymentBatchEntity> findByDeviceIdAndLocalPaymentBatchId(
            String deviceId,
            String localPaymentBatchId
    );

    List<PaymentBatchEntity> findByMembershipIdOrderByPaymentDateDesc(String membershipId);

    boolean existsByPaymentBatchNo(String paymentBatchNo);
}