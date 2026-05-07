package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PayrollPaymentItemEntity;
import za.co.mawa.bes.enums.payroll.PayrollPaymentItemStatus;

import java.util.List;

public interface PayrollPaymentItemRepository extends JpaRepository<PayrollPaymentItemEntity, String> {

    List<PayrollPaymentItemEntity> findByBatchIdOrderByEmployeeNameAsc(String batchId);

    List<PayrollPaymentItemEntity> findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(String batchId);

    List<PayrollPaymentItemEntity> findByBatchIdAndStatus(String batchId, PayrollPaymentItemStatus status);

    void deleteByBatchId(String batchId);
}
