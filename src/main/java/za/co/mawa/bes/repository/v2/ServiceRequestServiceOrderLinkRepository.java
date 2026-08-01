package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ServiceRequestServiceOrderLinkEntity;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestServiceOrderLinkRepository
        extends JpaRepository<ServiceRequestServiceOrderLinkEntity, String> {
    List<ServiceRequestServiceOrderLinkEntity> findByServiceRequestIdOrderByCreatedAtDesc(String serviceRequestId);
    Optional<ServiceRequestServiceOrderLinkEntity> findByServiceOrderId(String serviceOrderId);
}
