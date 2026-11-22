package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.PlatformBillingInvoiceMirrorEntity;
import java.util.List;

public interface PlatformBillingInvoiceMirrorRepository extends JpaRepository<PlatformBillingInvoiceMirrorEntity, Long> {
    List<PlatformBillingInvoiceMirrorEntity> findTop500ByOrderByUpdatedAtDesc();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PlatformBillingInvoiceMirrorEntity p where p.sourceInvoiceId = :id")
    java.util.Optional<PlatformBillingInvoiceMirrorEntity> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
