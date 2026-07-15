package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.LeaveRequestEntity;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, String>, JpaSpecificationExecutor<LeaveRequestEntity> {
}
