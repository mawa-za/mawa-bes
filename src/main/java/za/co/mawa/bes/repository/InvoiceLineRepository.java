package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.InvoiceLineEntity;

import java.util.List;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLineEntity, String> {
    List<InvoiceLineEntity> findByInvoiceId(String invoiceId);
}