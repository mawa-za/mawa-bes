package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.InvoiceEntity;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findByStatus(String status);
    List<InvoiceEntity> findByPartnerId(String partnerId);
    List<InvoiceEntity> findByInvoiceDate(LocalDate invoiceDate);
}