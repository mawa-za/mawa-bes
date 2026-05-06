package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchAuditEntity;

import java.util.List;

public interface PayrollPaymentBatchAuditRepository extends JpaRepository<PayrollPaymentBatchAuditEntity, String> {

    List<PayrollPaymentBatchAuditEntity> findByBatchIdOrderByCreatedAtDesc(String batchId);
}