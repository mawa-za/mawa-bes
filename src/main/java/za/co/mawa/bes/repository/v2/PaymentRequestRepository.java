package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;

import java.util.List;
import java.util.Optional;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequestEntity, String> {

    Optional<PaymentRequestEntity> findByRequestNo(String requestNo);

    List<PaymentRequestEntity> findByStatusOrderByCreatedAtDesc(PaymentRequestStatus status);

    List<PaymentRequestEntity> findByRequestTypeOrderByCreatedAtDesc(PaymentRequestType requestType);

    List<PaymentRequestEntity> findByPayeePartnerIdOrderByCreatedAtDesc(String payeePartnerId);

    List<PaymentRequestEntity> findBySourceTypeAndSourceIdOrderByCreatedAtDesc(
            PaymentRequestSourceType sourceType,
            String sourceId
    );
}
