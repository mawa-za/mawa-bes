package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.InvoiceEntity;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    // Additional query methods if needed
    // e.g., List<Invoice> findByStatus(String status);
}