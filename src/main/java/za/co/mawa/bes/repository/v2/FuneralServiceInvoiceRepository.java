package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.FuneralServiceInvoiceEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuneralServiceInvoiceRepository extends JpaRepository<FuneralServiceInvoiceEntity, String> {
    Optional<FuneralServiceInvoiceEntity> findFirstByInvoiceId(String invoiceId);
    List<FuneralServiceInvoiceEntity> findByFuneralServiceId(String funeralServiceId);
}
