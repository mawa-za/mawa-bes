package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.InvoicePaymentEntity;

import java.util.List;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePaymentEntity, String> {
    List<InvoicePaymentEntity> findByInvoiceId(String invoiceId);
}