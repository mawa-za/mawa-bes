package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchEntity;
import za.co.mawa.bes.enums.payroll.PayrollPaymentBatchStatus;

import java.util.List;
import java.util.Optional;

public interface PayrollPaymentBatchRepository extends JpaRepository<PayrollPaymentBatchEntity, String> {

    Optional<PayrollPaymentBatchEntity> findByBatchNo(String batchNo);

    List<PayrollPaymentBatchEntity> findByPayPeriodOrderByCreatedAtDesc(String payPeriod);

    List<PayrollPaymentBatchEntity> findByStatusOrderByCreatedAtDesc(PayrollPaymentBatchStatus status);

    List<PayrollPaymentBatchEntity> findByPayPeriodAndStatusOrderByCreatedAtDesc(
            String payPeriod,
            PayrollPaymentBatchStatus status
    );
}