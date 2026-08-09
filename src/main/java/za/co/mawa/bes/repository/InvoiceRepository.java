package za.co.mawa.bes.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.InvoiceEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findByStatus(String status);
    List<InvoiceEntity> findByPartnerId(String partnerId);
    List<InvoiceEntity> findByInvoiceDate(LocalDate invoiceDate);
    List<InvoiceEntity> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceEntity i where i.id = :id")
    Optional<InvoiceEntity> findByIdForUpdate(@Param("id") String id);
}