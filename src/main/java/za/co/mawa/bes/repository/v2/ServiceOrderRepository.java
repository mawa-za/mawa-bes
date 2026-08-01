package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import za.co.mawa.bes.entity.v2.ServiceOrderEntity;

public interface ServiceOrderRepository extends
        JpaRepository<ServiceOrderEntity, String>,
        JpaSpecificationExecutor<ServiceOrderEntity> {
    boolean existsByServiceOrderNo(String serviceOrderNo);
}
