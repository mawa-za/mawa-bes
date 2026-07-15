package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.LeaveRequestStatusHistoryEntity;

import java.util.List;

@Repository
public interface LeaveRequestStatusHistoryRepository extends JpaRepository<LeaveRequestStatusHistoryEntity, String> {
    List<LeaveRequestStatusHistoryEntity> findByLeaveRequestIdOrderByChangedAtDesc(String leaveRequestId);
}
